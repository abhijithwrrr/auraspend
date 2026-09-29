#!/usr/bin/env python3
"""Score the golden SMS corpus through Needle, host-side.

This is the measurement that handoff 0011 recorded as "0/65 exact" and then
never wrote down. It reproduces that number exactly first (STRICT, the same
arithmetic as `ClassificationEval.run`) so the two are comparable, then reports a
second view (CREDITED) that reflects how a grammar-constrained extractor is
actually specified to behave.

The difference between the two views is the whole point, and it is not a
thumb on the scale. Needle's contract (llms.txt) is explicit:

  * "Off-topic / unsupported request -> empty function_calls (a refusal)."
  * "optional fields with no evidence are omitted, not guessed"
  * "Do not read arguments keys that were not evidenced in the input"

So CREDITED reads what a grammar-constrained extractor actually asserted rather
than treating every gap as a wrong answer:

  * a **refusal** is the assertion `isTransaction=false` — "nothing to do here"
    is the honest reading of an empty call for an SMS classifier, and scoring it
    as an unasserted field would hand the model a free pass on a false veto,
    which `ClassificationEval` calls its most damaging error class;
  * an **omitted optional field** is "no assertion", scored against a null
    expectation only.

Both views are reported because both being equal is itself the finding: it
separates "the harness is unfair to grammar-constrained runtimes" from "the
model is wrong", and on this corpus it is the latter. STRICT stays the headline
since it is the apples-to-apples number against the regex floor.

`--agent` adds a second pass through `Needle.complete()` to read the calibrated
score, which `extract()` does not return. On this corpus the engine withholds
every call it produces, so that pass is the more honest one.

Usage:
    NEEDLE_TELEMETRY=0 DO_NOT_TRACK=1 tools/needle_eval.py
    tools/needle_eval.py --schema v2 --limit 10 --dump

Telemetry is opt-out via environment variables, which a host script can honour
reliably in a way Android cannot hand to a statically-linked library, so they
are force-set here rather than merely documented.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from pathlib import Path

# Honoured before `import needle`; the engine reads them at load time.
os.environ.setdefault("NEEDLE_TELEMETRY", "0")
os.environ.setdefault("DO_NOT_TRACK", "1")

REPO = Path(__file__).resolve().parent.parent
CORPUS = REPO / "app/src/test/resources/golden/sms_corpus.jsonl"

# Must stay identical to ClassificationEval.DEFAULT_CATEGORIES.
CATEGORIES: dict[str, str] = {
    "cat_food": "Food & Dining",
    "cat_transport": "Transport",
    "cat_subscription": "Subscriptions",
    "cat_salary": "Salary",
    "cat_shopping": "Shopping",
    "cat_grocery": "Groceries",
    "cat_healthcare": "Healthcare",
    "cat_education": "Education",
    "cat_bills": "Bills & Utilities",
    "cat_transfer": "Transfer",
    "cat_other": "Other",
}

FIELDS = ["isTransaction", "type", "category", "merchant", "subscription"]

_TYPES = ["expense", "income", "transfer"]

_CAT_LIST = ", ".join(f"{cid} ({name})" for cid, name in CATEGORIES.items())

# v1 is the naive shape: a one-line description with the real category
# vocabulary bound as an enum. Kept so the two can be compared, not because it
# is the shape to ship.
SCHEMA_V1 = {
    "name": "record_transaction",
    "description": "Record a bank transaction from an SMS.",
    "parameters": {
        "type": "object",
        "properties": {
            "isTransaction": {"type": "boolean"},
            "type": {"type": "string", "enum": _TYPES},
            "category": {"type": "string", "enum": list(CATEGORIES)},
            "merchant": {"type": "string"},
            "subscription": {"type": "boolean"},
        },
        "required": ["isTransaction"],
    },
}

# v2 states the job per field, which is what the needle guides actually ask
# for ("formats in descriptions"). The specific things v1 got wrong:
#   - it never said what isTransaction means, so a bank spend came back false;
#   - it never said not to read a card or account number as a merchant, so
#     "XX4821" was returned as one;
#   - category/merchant were optional with no guidance, so they came back
#     omitted even when the SMS plainly named a shop.
SCHEMA_V2 = {
    "name": "record_transaction",
    "description": (
        "Read one bank SMS and report the transaction it describes, if any. "
        "Set isTransaction to true only when the message reports money actually "
        "moving in or out. Set it to false for OTPs, fraud and security alerts, "
        "balance and limit updates, offers, promotions and statements of intent "
        "that report no completed movement. Never leave it unstated: decide."
    ),
    "parameters": {
        "type": "object",
        "properties": {
            "isTransaction": {
                "type": "boolean",
                "description": (
                    "true only if money moved. false for OTP codes, fraud or "
                    "security alerts, balance/limit/available-balance lines, "
                    "promotions, and pending authorisations."
                ),
            },
            "type": {
                "type": "string",
                "enum": _TYPES,
                "description": (
                    "expense when money was spent or a card was charged; income "
                    "when money was credited or received; transfer when it only "
                    "moved between the sender's own accounts."
                ),
            },
            "category": {
                "type": "string",
                "enum": list(CATEGORIES),
                "description": (
                    "One id from: " + _CAT_LIST + ". Choose the id the merchant "
                    "best fits. Omit only if the merchant is unidentifiable."
                ),
            },
            "merchant": {
                "type": "string",
                "description": (
                    "The trading name as it appears in the message, e.g. "
                    "'SUNRISE DINER'. A shop, brand or biller. Never a "
                    "card number, account number, phone number, IFSC or UPI pin."
                ),
            },
            "subscription": {
                "type": "boolean",
                "description": (
                    "true only for a recurring charge of a subscription or "
                    "membership that renews, such as a streaming or telecom bill."
                ),
            },
        },
        "required": ["isTransaction", "type", "subscription"],
    },
}

# v3 narrows the job to the only two fields the regex layer is actually weak
# at. `AiSignalFusion` takes the regex type on conflict, so expense/income never
# needed the model (regex 95.4%), and merchant falls back to the regex value
# whenever it found one. Category (36.9%) and isTransaction (66.2%) are the
# whole opportunity. Removing the fields the model is not responsible for also
# removes the failure that sank v1/v2: it no longer has to infer a type from a
# verb, and the enum it must respect is a closed category vocabulary.
SCHEMA_V3 = {
    "name": "categorise_bank_sms",
    "description": (
        "Decide whether one bank SMS reports money moving, and if so which "
        "spending category the merchant belongs to."
    ),
    "parameters": {
        "type": "object",
        "properties": {
            "isTransaction": {
                "type": "boolean",
                "description": (
                    "true only if the SMS reports a completed debit, credit or "
                    "transfer. false for OTP codes, fraud or security alerts, "
                    "balance and available-limit lines, promotions, and "
                    "messages that only announce an upcoming charge."
                ),
            },
            "category": {
                "type": "string",
                "enum": list(CATEGORIES),
                "description": (
                    "The spending category for the named merchant. "
                    "Prefer a category named by the SMS itself. Otherwise pick "
                    "the best fit for the merchant. Use " + _CAT_LIST + "."
                ),
            },
        },
        "required": ["isTransaction", "category"],
    },
}

SCHEMAS = {"v1": SCHEMA_V1, "v2": SCHEMA_V2, "v3": SCHEMA_V3}


def load_corpus() -> list[dict]:
    return [json.loads(l) for l in CORPUS.read_text().splitlines() if l.strip()]


def norm_expected(case: dict, field: str):
    """Mirrors ClassificationEval.expectedValue."""
    if field == "isTransaction":
        return str(case["isTransaction"]).lower()
    if field == "type":
        t = case.get("type")
        return t.upper() if t else None
    if field == "category":
        return case.get("category") or None
    if field == "merchant":
        return case.get("merchant") or None
    if field == "subscription":
        return str(case["subscription"]).lower()
    return None


def norm_actual(args: dict | None, field: str):
    """Mirrors ClassificationEval.actualValue over a needle argument dict."""
    if args is None:
        return None
    if field == "isTransaction":
        v = args.get("isTransaction")
        return None if v is None else str(bool(v)).lower()
    if field == "type":
        t = args.get("type")
        return t.upper() if t else None
    if field == "category":
        return args.get("category") or None
    if field == "merchant":
        return args.get("merchant") or None
    if field == "subscription":
        v = args.get("subscription")
        return None if v is None else str(bool(v)).lower()
    return None


def score(cases, predictions):
    """predictions: list of (args|None, confidence|None)."""
    rows = []
    for case, (args, conf) in zip(cases, predictions):
        strict = []
        credited = []
        for f in FIELDS:
            exp = norm_expected(case, f)
            act = norm_actual(args, f)
            strict.append(exp == act)
            # CREDITED. Two corrections, both about reading what a
            # grammar-constrained extractor actually asserted:
            #
            #  1. An empty call is a refusal, and Needle's contract is that a
            #     refusal means "nothing to do here". For an SMS classifier that
            #     is the assertion isTransaction=false. Scoring it as an
            #     unasserted optional field would quietly hand the model a pass
            #     on a false veto, which the harness calls its most damaging
            #     error class, so it is scored as the verdict it is.
            #  2. A field the model omitted is "no assertion", not a wrong one,
            #     so it is scored against a null expectation only.
            if args is None:
                credited.append(exp == ("false" if f == "isTransaction" else None))
            elif act is not None:
                credited.append(exp == act)
            else:
                credited.append(exp is None)
        rows.append(
            {
                "id": case["id"],
                "conf": conf,
                "args": args,
                "refused": args is None,
                "strict": all(strict),
                "credited": all(credited),
                "strict_fields": dict(zip(FIELDS, strict)),
                "credited_fields": dict(zip(FIELDS, credited)),
            }
        )
    return rows


def per_field(rows, key):
    return {f: 100.0 * sum(r[key][f] for r in rows) / len(rows) for f in FIELDS}


def report(rows, total, elapsed, schema_name, asked=None):
    """`asked` = the fields the schema actually declares, for the scoped view."""
    strict_exact = sum(r["strict"] for r in rows)
    cred_exact = sum(r["credited"] for r in rows)
    refusals = sum(r["refused"] for r in rows)
    sf, cf = per_field(rows, "strict_fields"), per_field(rows, "credited_fields")
    vetoes = [
        r for r in rows
        if r["args"] is not None
        and r["args"].get("isTransaction") is False
        and norm_expected(next(c for c in CORPUS_CASES if c["id"] == r["id"]), "isTransaction") == "true"
    ]
    p = print
    p("=" * 68)
    p(f"NEEDLE {needle_version()}  schema={schema_name}  cases={len(rows)}/{total}")
    p(f"elapsed {elapsed:.1f}s  ({elapsed / max(len(rows), 1):.2f}s/SMS)")
    p("=" * 68)
    p(f"refused (no call)    : {refusals}/{len(rows)}")
    p(f"STRICT exact match   : {strict_exact}/{len(rows)} ({100.0 * strict_exact / len(rows):.1f}%)")
    p(f"CREDITED exact match : {cred_exact}/{len(rows)} ({100.0 * cred_exact / len(rows):.1f}%)")
    p(f"false vetoes         : {len(vetoes)}")
    p("-" * 68)
    p(f"{'field':<16}{'STRICT':>10}{'CREDITED':>12}")
    for f in FIELDS:
        mark = " " if (asked is None or f in asked) else "*"
        p(f"{f:<15}{mark}{sf[f]:>9.1f}%{cf[f]:>11.1f}%")
    if asked is not None:
        got = [r for r in rows
               if all(r["credited_fields"][f] for f in asked)]
        p(f"scoped exact (on {', '.join(sorted(asked))}): "
          f"{len(got)}/{len(rows)} ({100.0 * len(got) / len(rows):.1f}%)")
        p("* = field this schema does not ask for; shown for continuity only.")
    p("=" * 68)
    return {"strict": strict_exact, "credited": cred_exact, "rows": rows}


def needle_version() -> str:
    import needle
    return str(getattr(needle, "__version__", "?"))


def run_agent(cases, schema) -> tuple[list, list]:
    """Second pass over the corpus via the agent API, to read the calibrated score.

    `extract()` is one-shot and returns only the arguments; the confidence head
    lives on `Needle.complete()`. That matters for two reasons. It settles
    whether the calibrated score this project reshaped its seam for is actually
    reachable, and it shows how often the engine withholds its own answer — a
    call moved to `suppressed_calls` is the model declining to stand behind its
    own extraction, which the first pass cannot see.

    A fresh agent is built per case because `complete()` carries conversation
    state, and these messages are independent: a shared instance would let one
    SMS contaminate the next.
    """
    import needle
    rows, confs = [], []
    for case in cases:
        try:
            agent = needle.Needle(tools=[schema])
            r = agent.complete(case["body"], max_new_tokens=256)
        except Exception as e:  # noqa: BLE001
            print(f"  ! {case['id']}: {type(e).__name__}: {e}", file=sys.stderr)
            rows.append((None, None))
            confs.append(None)
            continue
        calls = r.get("function_calls") or []
        suppressed = r.get("suppressed_calls") or []
        argsd = (calls or suppressed or [None])[0]
        argsd = argsd.get("arguments") if isinstance(argsd, dict) else None
        rows.append((argsd, bool(calls)))
        confs.append(r.get("confidence"))
    return rows, confs


CORPUS_CASES: list[dict] = []


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--schema", default="v2", choices=sorted(SCHEMAS))
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--dump", action="store_true", help="print a per-case diff")
    ap.add_argument("--json", help="write the full report to this path")
    ap.add_argument("--agent", action="store_true",
                    help="second pass reading the calibrated confidence and withheld calls")
    args = ap.parse_args()

    global CORPUS_CASES
    CORPUS_CASES = load_corpus()
    cases = CORPUS_CASES[: args.limit] if args.limit else CORPUS_CASES
    schema = SCHEMAS[args.schema]

    import needle

    t0 = time.time()
    predictions = []
    for i, case in enumerate(cases, 1):
        try:
            # strict=False: a call the engine withheld on grounding still counts
            # as evidence, and for this corpus a grounding false-negative is a
            # harness artefact rather than a user-visible error.
            res = needle.extract(case["body"], schema, max_new_tokens=256, strict=False)
        except Exception as e:  # noqa: BLE001 - a crash must not end the sweep
            print(f"  ! {case['id']}: {type(e).__name__}: {e}", file=sys.stderr)
            res = None
        argsd = res if isinstance(res, dict) else None
        conf = getattr(res, "confidence", None) if not isinstance(res, dict) else None
        predictions.append((argsd, conf))
        if i % 10 == 0:
            print(f"  ...{i}/{len(cases)}", file=sys.stderr)
    elapsed = time.time() - t0

    rows = score(cases, predictions)
    asked = set(schema["parameters"]["properties"]) & set(FIELDS)
    rep = report(rows, len(cases), elapsed, args.schema, asked)

    if args.dump:
        for r in rep["rows"]:
            case = next(c for c in cases if c["id"] == r["id"])
            flag = "OK " if r["credited"] else "MISS"
            print(f"{flag} {r['id']}")
            print(f"     exp: " + ", ".join(
                f"{f}={norm_expected(case, f)!r}" for f in FIELDS))
            print(f"     got: {r['args']!r}")

    if args.agent:
        arows, confs = run_agent(cases, schema)
        scored = [c for c in confs if c is not None]
        withheld = sum(1 for _, live in arows if live is False)
        p = print
        p("-" * 68)
        p("AGENT PASS (calibration head)")
        p(f"confidence reported  : {len(scored)}/{len(cases)}")
        if scored:
            s = sorted(scored)
            p(f"  min/median/max    : {s[0]:.4f} / {s[len(s) // 2]:.4f} / {s[-1]:.4f}")
            p(f"  above 0.7 (act)   : {sum(1 for c in s if c >= 0.7)}")
            p(f"  0.1-0.7 (confirm) : {sum(1 for c in s if 0.1 <= c < 0.7)}")
            p(f"  below 0.1 (refuse): {sum(1 for c in s if c < 0.1)}")
        p(f"withheld by engine  : {withheld}/{len(cases)} "
          f"(engine moved its own call to suppressed_calls)")
        agree = sum(1 for (a, _), (b, _) in zip(predictions, arows) if a == b)
        p(f"agent/extract agree : {agree}/{len(cases)}")
        p("=" * 68)

    if args.json:
        Path(args.json).write_text(json.dumps(
            {"schema": args.schema, "elapsed": elapsed, "rows": rep["rows"]}, indent=2))
        print(f"wrote {args.json}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
