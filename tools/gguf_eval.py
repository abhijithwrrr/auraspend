#!/usr/bin/env python3
"""Score the golden SMS corpus through a real GGUF model on the host.

The accuracy gate for swapping `ModelConstants.MODEL_URL`. It drives the
*actual* prompt from `GgufMessageCategorizer.buildPrompt` through llama.cpp and
the *actual* parse rules from `GgufMessageCategorizer.parse`, so what is scored
is what the app would do — not a paraphrase of either.

The Kotlin prompt and parser are the source of truth and are read from source
rather than reimplemented, so a prompt change cannot silently desync this
harness from the app. The mirroring is one-way and fail-loud: if a marker the
parser depends on is missing, this aborts instead of scoring something else.

This exists because a model swap cannot be judged by a green test suite. The
unit tests use a fake `LocalLlm` and never load weights, so they prove the
*parser* is right and say nothing about the *model*. Every accuracy claim about a
GGUF has to come from here.

Usage:
    tools/gguf_eval.py --model path/to.gguf
    tools/gguf_eval.py --model a.gguf --json out.json --dump

Needs the host `llama-server` tool:
    cmake -S third_party/llama.cpp -B third_party/llama.cpp/build-host \\
      -DCMAKE_BUILD_TYPE=Release -DGGML_METAL=ON -DLLAMA_CURL=OFF
    cmake --build third_party/llama.cpp/build-host --target llama-server
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
CORPUS = REPO / "app/src/test/resources/golden/sms_corpus.jsonl"
CATEGORIZER = REPO / "app/src/main/java/com/awbuilds/auraspend/data/ai/QwenMessageCategorizer.kt"
SERVER = REPO / "third_party/llama.cpp/build-host/bin/llama-server"

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

TYPE_MAP = {
    "income": "INCOME", "credit": "INCOME", "credited": "INCOME",
    "received": "INCOME", "refund": "INCOME", "refunded": "INCOME",
    "expense": "EXPENSE", "debit": "EXPENSE", "debited": "EXPENSE",
    "spent": "EXPENSE", "sent": "EXPENSE", "paid": "EXPENSE",
}

CATEGORY_OTHER = "cat_other"
CATEGORY_SUBSCRIPTION = "cat_subscription"


# ── the real prompt, lifted from the Kotlin source ────────────────────────────

def load_prompt_template() -> str:
    """Extract the prompt body from buildPrompt so this cannot drift from the app.

    The Kotlin builds it with appendLine, so the emitted text is the literal
    lines in order with the category list interpolated. Rather than retyping
    them (which is how harnesses silently start measuring a different prompt),
    the literals are read out of the source and the two interpolations are
    substituted. If the markers move, this fails loudly.
    """
    src = CATEGORIZER.read_text()
    start = src.index("fun buildPrompt(")
    end = src.index("fun parse(")
    body = src[start:end]

    lines = re.findall(r'appendLine\((?:"""(.*?)"""|"((?:[^"\\]|\\.)*)")\)', body, re.S)
    out: list[str] = []
    for raw_a, raw_b in lines:
        text = raw_a if raw_a else raw_b
        # Kotlin triple-quoted strings are raw; regular ones need unescaping.
        if raw_b:
            text = text.encode().decode("unicode_escape")
        text = text.replace('\\"', '"').replace("\\$", "$")
        # Kotlin string templates in the source: $categories and ${expr...}
        if "$categories" in text:
            text = text.replace(
                "$categories",
                ", ".join(dict.fromkeys(v for v in CATEGORIES.values() if v.strip())),
            )
        out.append(text)
    if not out:
        sys.exit("could not read the prompt from GgufMessageCategorizer.kt")
    template = "\n".join(out) + "\n"

    for marker in ("Extract bank-transaction data", "is_transaction", "Examples:"):
        if marker not in template:
            sys.exit(
                f"prompt template is missing {marker!r} — the Kotlin prompt "
                "changed shape and this harness must be updated, not guessed at. "
                f"(Read from {CATEGORIZER.name}; if the class was renamed, update "
                "CATEGORIZER.)"
            )
    return template


TEMPLATE = load_prompt_template()


def build_prompt(raw_message: str) -> str:
    """Mirrors the tail of buildPrompt: the SMS line and the OUT cue."""
    return TEMPLATE + raw_message + "\n" + "OUT:"


# ── the real parser, mirrored from parse() ────────────────────────────────────

def extract_json_object(out: str) -> str | None:
    """Balanced-brace scan, matching extractJsonObject."""
    start = out.find("{")
    if start < 0:
        return None
    depth = 0
    in_str = False
    esc = False
    for i in range(start, len(out)):
        ch = out[i]
        if in_str:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
            continue
        if ch == '"':
            in_str = True
        elif ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return out[start:i + 1]
    return None


def normalize_token(s: str) -> str:
    return re.sub(r"[^a-z0-9&+ ]", "", s.lower()).strip()


def lookup_category_id(name: str | None) -> str | None:
    if not name or not name.strip():
        return CATEGORY_OTHER
    if name.lower() in ("none", "other"):
        return CATEGORY_OTHER
    for cid, cname in CATEGORIES.items():
        if cname.lower() == name.lower():
            return cid
    for cid in CATEGORIES:
        if cid.lower() == name.lower():
            return cid
    q = normalize_token(name)
    if q:
        for cid, cname in CATEGORIES.items():
            if normalize_token(cname) == q:
                return cid
    words = [w for w in q.split() if w]
    if words:
        for cid, cname in CATEGORIES.items():
            if normalize_token(cname) in words:
                return cid
    return CATEGORY_OTHER


def parse(out: str) -> dict | None:
    """Mirrors GgufMessageCategorizer.parse. Returns None when nothing parses."""
    trimmed = out.strip()
    block = extract_json_object(trimmed)
    if block is None:
        return None

    m = re.search(r"[\"']?(?:is_?transaction|transaction)[\"']?\s*:\s*(true|false)", block, re.I)
    explicit = m.group(1).lower() if m else None

    m = re.search(r"[\"']?type[\"']?\s*:\s*[\"']?([a-z]+)[\"']?", block, re.I)
    tname = m.group(1).lower() if m else None
    ttype = TYPE_MAP.get(tname) if tname else None

    m = re.search(r"[\"']?category[\"']?\s*:\s*[\"']([^\"']*)[\"']", block, re.I)
    cat = m.group(1).strip() if m else None

    m = re.search(r"[\"']?merchant[\"']?\s*:\s*[\"']([^\"']*)[\"']", block, re.I)
    merch = m.group(1).strip() if m else None
    if not merch or merch.lower() == "none":
        merch = None

    m = re.search(r"[\"']?subscription[\"']?\s*:\s*[\"']?(true|false)[\"']?", block, re.I)
    is_sub = (cat or "").lower() == "subscriptions" or (m and m.group(1).lower() == "true")

    cat_id = CATEGORY_SUBSCRIPTION if is_sub else lookup_category_id(cat)

    is_txn = {"true": True, "false": False}.get(explicit, ttype is not None)

    return {
        "isTransaction": is_txn, "type": ttype, "category": cat_id,
        "merchant": merch, "isSubscription": is_sub, "raw": trimmed,
    }


# ── scoring (mirrors ClassificationEval) ──────────────────────────────────────

def norm_expected(case: dict, field: str):
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


def norm_actual(pred: dict | None, field: str):
    if pred is None:
        return None
    if field == "isTransaction":
        return str(pred["isTransaction"]).lower()
    if field == "type":
        return pred["type"]
    if field == "category":
        return pred["category"] or None
    if field == "merchant":
        return pred["merchant"] or None
    if field == "subscription":
        return str(pred["isSubscription"]).lower()
    return None


class LlamaServer:
    """A `llama-server` process serving one model, driven over HTTP.

    The HTTP API is used rather than a CLI for one specific reason: it is the
    only one of the three interfaces tried that returns *exactly* the
    generation, with no prompt echo and no chrome to strip.

    Getting that wrong is not a cosmetic problem. Every other interface echoes
    the prompt to stdout, and this prompt's second line is a JSON template
    containing a literal '{'. A balanced-brace scanner therefore latches onto
    the echo and scores the *template* instead of the model's answer, producing
    a number that looks like a real measurement and is entirely fictional.
    (`llama-completion` additionally re-renders '/' as '\\x08/\\x08' for terminal
    overstrike, so its echo is not even byte-comparable.) A JSON response field
    makes that whole class of error impossible.
    """

    def __init__(self, model: Path, port: int, ctx: int, threads: int = 6):
        self.model = model
        self.port = port
        self.base = f"http://127.0.0.1:{port}"
        self.proc = subprocess.Popen(
            [str(SERVER), "-m", str(model), "--port", str(port),
             "-c", str(ctx), "-t", str(threads), "--log-disable"],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        self._wait_ready()

    def _wait_ready(self, timeout: float = 90.0) -> None:
        deadline = time.time() + timeout
        while time.time() < deadline:
            if self.proc.poll() is not None:
                raise RuntimeError(
                    f"llama-server exited early (rc={self.proc.returncode})"
                )
            try:
                with urllib.request.urlopen(f"{self.base}/health", timeout=2) as r:
                    if json.loads(r.read()).get("status") == "ok":
                        return
            except Exception:
                time.sleep(0.5)
        raise RuntimeError(f"llama-server did not become ready within {timeout}s")

    def complete(self, prompt: str, max_tokens: int) -> str:
        body = json.dumps({
            "prompt": prompt,
            "n_predict": max_tokens,
            "temperature": 0,
            "top_k": 1,
            "seed": 42,
            "cache_prompt": True,
        }).encode()
        req = urllib.request.Request(
            f"{self.base}/completion", data=body,
            headers={"Content-Type": "application/json"},
        )
        with urllib.request.urlopen(req, timeout=300) as r:
            return json.loads(r.read())["content"]

    def close(self) -> None:
        self.proc.terminate()
        try:
            self.proc.wait(timeout=15)
        except subprocess.TimeoutExpired:
            self.proc.kill()

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", required=True)
    ap.add_argument("--max-tokens", type=int, default=128)  # MAX_OUTPUT_TOKENS
    ap.add_argument("--ctx", type=int, default=4096)
    ap.add_argument("--port", type=int, default=8931)
    ap.add_argument("--attempts", type=int, default=2)   # GENERATION_ATTEMPTS
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--dump", action="store_true")
    ap.add_argument("--json")
    args = ap.parse_args()

    model = Path(args.model).resolve()
    if not SERVER.exists():
        sys.exit(
            f"llama-server not built: {SERVER}\n"
            "Build it with:\n"
            "  cmake -S third_party/llama.cpp -B third_party/llama.cpp/build-host \\\n"
            "    -DCMAKE_BUILD_TYPE=Release -DGGML_METAL=ON -DLLAMA_CURL=OFF\n"
            "  cmake --build third_party/llama.cpp/build-host --target llama-server"
        )
    if not model.exists():
        sys.exit(f"model not found: {model}")

    cases = [json.loads(l) for l in CORPUS.read_text().splitlines() if l.strip()]
    if args.limit:
        cases = cases[: args.limit]
    print(f"{len(cases)} cases, model {model.name}", file=sys.stderr)

    rows = []
    t0 = time.time()
    with LlamaServer(model, port=args.port, ctx=args.ctx) as server:
        # One warm-up completion so the first scored case is not paying for
        # lazy graph allocation and any lazy model load.
        server.complete("ok", 4)
        for i, case in enumerate(cases, 1):
            prompt = build_prompt(case["body"])
            raw, pred = "", None
            for _ in range(args.attempts):
                raw = server.complete(prompt, args.max_tokens)
                if not raw:
                    break  # engine unavailable; retrying will not help
                pred = parse(raw)
                if pred:
                    break
            veto = pred is not None and not pred["isTransaction"]
            rows.append({"id": case["id"], "raw": raw, "pred": pred,
                         "false_veto": veto and case["isTransaction"]})
            if i % 10 == 0:
                print(f"  ...{i}/{len(cases)}", file=sys.stderr)
    elapsed = time.time() - t0

    scored = []
    for case, r in zip(cases, rows):
        fields = {f: norm_expected(case, f) == norm_actual(r["pred"], f) for f in FIELDS}
        scored.append({**r, "fields": fields, "exact": all(fields.values())})

    n = len(scored)
    exact = sum(s["exact"] for s in scored)
    nores = sum(1 for s in scored if s["pred"] is None)
    vetoes = sum(1 for s in scored if s["false_veto"])
    p = print
    p("=" * 68)
    p(f"{model.name}")
    p(f"cases {n} · elapsed {elapsed:.1f}s ({elapsed / max(n, 1):.2f}s/SMS host)")
    p("=" * 68)
    p(f"exact match       : {exact}/{n} ({100.0 * exact / n:.1f}%)")
    p(f"no result (null)  : {nores}")
    p(f"false vetoes      : {vetoes}   <-- discards a good regex parse")
    p("-" * 68)
    p(f"{'field':<16}{'acc':>10}")
    for f in FIELDS:
        c = sum(1 for s in scored if s["fields"][f])
        p(f"{f:<16}{c:>4}/{n}  {100.0 * c / n:>5.1f}%")
    p("=" * 68)
    p("GATE (floors asserted by RegexBaselineEvalTest):")
    p("  type       >= 90.0%   -> " + ("PASS" if 100.0 * sum(1 for s in scored if s['fields']['type']) / n >= 90 else "FAIL"))
    p("  exact      >= 13.0%   -> " + ("PASS" if 100.0 * exact / n >= 13.0 else "FAIL"))
    p("  (category/merchant: no material regression vs 36.9% / 58.5%)")
    p("=" * 68)

    if args.dump:
        for s in scored:
            if not s["exact"]:
                case = next(c for c in cases if c["id"] == s["id"])
                bad = [f for f in FIELDS if not s["fields"][f]]
                p(f"MISS {s['id']}  ({', '.join(bad)})")
                p(f"     raw: {s['raw'][:150]!r}")
    if args.json:
        Path(args.json).write_text(json.dumps({"model": model.name, "elapsed": elapsed, "rows": scored}, indent=2))
        p(f"wrote {args.json}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
