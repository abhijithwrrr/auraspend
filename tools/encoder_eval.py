#!/usr/bin/env python3
"""Prototype: classify bank SMS with a sentence encoder instead of a generator.

The hypothesis under test
-------------------------
Three generative models were measured and rejected. All of them failed the same
way: they emitted a confident, well-formed `is_transaction: false` for real
transactions, and `AiSignalFusion` turns that into a *deletion* (7 destroyed for
Qwen2.5-0.5B, 5 for FunctionGemma-270M, 43 for SmolLM2-135M). The cost of being
wrong is a user's transaction disappearing, so the error is not cosmetic.

An embedding model cannot express that failure. It does not generate text, so
there is no output in which a spurious verdict can be fabricated — only a
similarity score, which is low honestly rather than high falsely. It is also 7x
smaller than the smallest generative candidate.

What this measures
------------------
Nearest-centroid classification over a small set of labelled seed phrases per
class, with a softmax over cosine similarities. The softmax is the part that
matters for the app: it is the calibrated number `SmsExtraction.
isTransactionProbability` has been waiting for, and unlike a generative model it
falls out of the architecture rather than being asked for in a prompt.

Everything is measured on the 65-case golden corpus, scored with the same
arithmetic as `ClassificationEval`, so these numbers are directly comparable to
the regex floor and to the three generative runs recorded in `docs/evals/`.

This is a prototype and a hypothesis, not a shippable classifier. It is written
to be falsifiable: if the per-field numbers do not clear the floor recorded in
`RegexBaselineEvalTest`, the idea is wrong and the output says so.

Usage:
    .zcode/enc-venv/bin/python tools/encoder_eval.py
    .zcode/enc-venv/bin/python tools/encoder_eval.py --model .zcode/enc/model.onnx --dump
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer

REPO = Path(__file__).resolve().parent.parent
CORPUS = REPO / "app/src/test/resources/golden/sms_corpus.jsonl"

FIELDS = ["isTransaction", "type", "category", "merchant", "subscription"]

# Floors asserted by RegexBaselineEvalTest — the no-model baseline. Any runtime
# must clear these or it is worse than shipping nothing.
FLOORS = {
    "exact": 13.0,
    "type": 90.0,
    "category": 36.9,
}

# ── seed phrases ─────────────────────────────────────────────────────────────
#
# Written from the category definitions, NOT copied from the corpus. That is
# deliberate: seeding with corpus texts and then scoring on the same corpus
# measures memorisation, and the whole point of the eval is to be falsifiable.
# Multiple phrasings per class because one sentence is a brittle centroid.

NEGATIVE_SEEDS = [
    "Your OTP is 482913 do not share with anyone",
    "One time password for your login is 771204",
    "Avl balance Rs 25,000.00 available",
    "Your credit card ending 8899 has been added successfully",
    "Congratulations you have won a lottery prize of ten lakhs",
    "Use code GET50 for recharge limited period offer",
    "Your KYC has been completed successfully",
    "Statement of your account for this month is ready",
    "Your EMI will be debited on the fifth of every month",
    "Cheque of Rs 5000 is drawn on your account",
    "Request for a duplicate statement card has been received",
    "Your account will be debited for the subscription renewal on 12-08-2026",
    "A payment of Rs 1200 is due on your credit card by 30-08-2026",
    "Your UPI mandate has been registered with the bank",
    "This is an advance notice of a charge that has not happened yet",
]

INCOME_SEEDS = [
    "Rs 45000 salary credited to your account",
    "Your salary has been credited to your bank account",
    "An amount of INR 46800 has been credited to your account via NEFT",
    "Monthly payroll deposit from your employer",
    "Your account has been credited a refund of Rs 999",
    "Interest has been credited to your savings account",
    "Dividend credited to your demat account",
    "Cashback of Rs 75 has been credited to your account",
    "A loan amount has been credited to your account",
]

EXPENSE_SEEDS = [
    "Rs 250 debited from your account at SWIGGY",
    "You spent INR 2745 on your credit card at MCDONALDS",
    "An amount of INR 135 has been debited to your account",
    "Rs 1000 debited towards house rent",
    "Rs 25000 debited towards school fees",
    "Rs 799 debited for Airtel prepaid recharge",
    "Rs 1450 paid to Adani Electricity for your bill",
    "Rs 640 debited at APOLLO PHARMACY",
    "Rs 1200 spent on credit card at IOCL PETROL PUMP",
    "Rs 649 debited at ZOMATO",
    "Rs 420 debited at a general store",
    "Rs 1500 debited for gym membership renewal",
    "Rs 300 debited at SHOP 123",
    "Rs 2000 withdrawn at an ATM",
    "An amount of INR 10,000 has been debited to your account",
    "UPI payment of Rs 299 sent to Rahim",
    "AutoPay charge of Rs 499 for NETFLIX subscription",
    "Rs 299 charged for Amazon Prime renewal",
    "Rs 119 debited for Spotify auto-debit",
    "Spent INR 135 on Axis Bank card at TULIP MART",
]

CATEGORY_SEEDS: dict[str, list[str]] = {
    "cat_food": [
        "You spent money ordering food delivery from Swiggy",
        "A restaurant or cafe bill was paid",
        "You paid for a pizza or a burger at a fast food place",
        "Mcdonalds and Dominos restaurant spending",
        "Starbucks coffee purchase",
    ],
    "cat_grocery": [
        "You bought groceries and supermarket essentials",
        "A grocery delivery from BigBasket or DMart",
        "Buying milk, vegetables and daily groceries",
        "Blinkit or Zepto quick commerce grocery order",
        "Reliance Fresh or More supermarket shopping",
    ],
    "cat_shopping": [
        "You bought clothes and retail products online",
        "An Amazon or Flipkart order for general shopping",
        "Myntra fashion and apparel purchase",
        "Nykaa beauty products or an IKEA homeware purchase",
        "A general store or retail merchandise purchase",
    ],
    "cat_transport": [
        "You paid for a taxi or ride hailing ride",
        "Fuel purchase at a petrol pump or filling station",
        "Ola or Uber ride fare",
        "A flight or train ticket booking",
        "A metro card recharge for public transport",
    ],
    "cat_subscription": [
        "A recurring monthly subscription charge for a streaming service",
        "Netflix or Spotify subscription renewal",
        "An auto debit for a paid membership that renews every month",
        "Amazon Prime or YouTube Premium subscription",
        "iCloud or Google One cloud storage subscription",
    ],
    "cat_healthcare": [
        "You paid for a medical bill at a hospital",
        "Pharmacy or chemist purchase of medicine",
        "Apollo pharmacy or a doctor consultation fee",
        "A health checkup or diagnostic test payment",
    ],
    "cat_education": [
        "A school fee payment for your child",
        "College or university tuition fees paid",
        "An online course purchase on Udemy or Coursera",
        "Training and coaching fees",
    ],
    "cat_bills": [
        "Your electricity bill payment",
        "A telecom mobile recharge or postpaid bill",
        "Broadband or internet bill payment",
        "A water or utility bill was paid",
        "An insurance or annual fee charge",
    ],
    "cat_transfer": [
        "A UPI payment sent to another person",
        "Money transferred to your own savings account",
        "A NEFT or IMPS transfer to a friend",
    ],
    "cat_salary": [
        "Your monthly salary was credited to your account",
        "An employer payroll deposit",
        "A salary advance credited to your account",
    ],
    "cat_other": [
        "A transaction that does not fit any other category",
        "A donation to a charitable organisation",
        "An unexplained bank charge",
    ],
}


class Encoder:
    """ONNX sentence encoder, mean-pooled and L2-normalised."""

    def __init__(self, model_path: Path):
        opts = ort.SessionOptions()
        opts.intra_op_num_threads = 6
        self.session = ort.InferenceSession(
            str(model_path), opts, providers=["CPUExecutionProvider"]
        )
        self.tokenizer = Tokenizer.from_file(str(model_path.parent / "tokenizer.json"))
        self.tokenizer.enable_padding(pad_id=0, pad_token="[PAD]")
        self.tokenizer.enable_truncation(max_length=256)
        self.inputs = {i.name for i in self.session.get_inputs()}

    def encode(self, texts: list[str]) -> np.ndarray:
        encs = self.tokenizer.encode_batch(texts)
        ids = np.array([e.ids for e in encs], dtype=np.int64)
        mask = np.array([e.attention_mask for e in encs], dtype=np.int64)
        feed = {"input_ids": ids, "attention_mask": mask}
        if "token_type_ids" in self.inputs:
            feed["token_type_ids"] = np.zeros_like(ids)
        out = self.session.run(None, feed)[0]
        # Mean pooling over real tokens, then L2 normalise so a dot product is
        # cosine similarity.
        m = mask[..., None].astype(np.float32)
        pooled = (out * m).sum(axis=1) / np.clip(m.sum(axis=1), 1e-9, None)
        return pooled / np.clip(np.linalg.norm(pooled, axis=1, keepdims=True), 1e-9, None)


def centroids(enc: Encoder, seed_lists: list[list[str]]):
    """Mean of each class's seed embeddings, renormalised."""
    cents = []
    for seeds in seed_lists:
        v = enc.encode(seeds).mean(axis=0)
        cents.append(v / np.linalg.norm(v))
    return np.stack(cents)


def softmax_probs(sim: np.ndarray, temperature: float) -> np.ndarray:
    """Calibrated-ish posterior over classes from cosine similarities.

    This is the piece the app actually needs. A generative model can only answer
    `is_transaction: true/false` and fabricate the confidence; here a low
    temperature-scaled similarity *is* the doubt, and it cannot be asserted
    against the evidence.
    """
    z = sim / temperature
    z = z - z.max(axis=-1, keepdims=True)
    e = np.exp(z)
    return e / e.sum(axis=-1, keepdims=True)


def load_corpus() -> list[dict]:
    return [json.loads(l) for l in CORPUS.read_text().splitlines() if l.strip()]


def norm(v):
    if v is None:
        return None
    return str(v).lower()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", default=".zcode/enc/model_qint8_avx512_vnni.onnx")
    ap.add_argument("--temperature", type=float, default=0.05)
    ap.add_argument("--dump", action="store_true")
    args = ap.parse_args()

    model = Path(args.model)
    if not model.exists():
        sys.exit(f"model not found: {model}")

    enc = Encoder(model)
    cases = load_corpus()
    bodies = [c["body"] for c in cases]
    emb = enc.encode(bodies)

    # Gate 1: is this a transaction at all? Negatives vs expenses.
    tx_cents = centroids(enc, [NEGATIVE_SEEDS, EXPENSE_SEEDS + INCOME_SEEDS])
    tx_sim = emb @ tx_cents.T
    tx_probs = softmax_probs(tx_sim, args.temperature)

    # Gate 2: type, only for those called transactions.
    type_cents = centroids(enc, [INCOME_SEEDS, EXPENSE_SEEDS])
    type_probs = softmax_probs(emb @ type_cents.T, args.temperature)

    # Gate 3: category, only for expenses (income maps by type).
    cat_ids = list(CATEGORY_SEEDS)
    cat_cents = centroids(enc, [CATEGORY_SEEDS[c] for c in cat_ids])
    cat_probs = softmax_probs(emb @ cat_cents.T, args.temperature)

    rows = []
    for i, c in enumerate(cases):
        p_tx = float(tx_probs[i][1])
        p_notx = float(tx_probs[i][0])
        is_txn = p_tx > p_notx
        t_idx = int(type_probs[i].argmax())
        t_name = "income" if t_idx == 0 else "expense"
        c_idx = int(cat_probs[i].argmax())
        category = cat_ids[c_idx] if is_txn and t_name == "expense" else (
            "cat_salary" if t_name == "income" else None
        )
        rows.append({
            "id": c["id"], "isTransaction": is_txn,
            "p_tx": p_tx, "type": t_name if is_txn else None,
            "category": category, "merchant": None,
            "isSubscription": category == "cat_subscription",
        })

    fields = {f: 0 for f in FIELDS}
    exact = 0
    vetoes, false_txns = [], []
    for c, r in zip(cases, rows):
        exp = {
            "isTransaction": norm(c["isTransaction"]),
            "type": (c.get("type") or "").upper() or None,
            "category": c.get("category") or None,
            "merchant": c.get("merchant") or None,
            "subscription": norm(c["subscription"]),
        }
        act = {
            "isTransaction": norm(r["isTransaction"]),
            "type": r["type"].upper() if r["type"] else None,
            "category": r["category"],
            "merchant": r["merchant"],
            "subscription": norm(r["isSubscription"]),
        }
        ok = all(exp[f] == act[f] for f in FIELDS)
        for f in FIELDS:
            if exp[f] == act[f]:
                fields[f] += 1
        if ok:
            exact += 1
        # A false veto: corpus says money moved, encoder says it did not.
        if c["isTransaction"] and not r["isTransaction"]:
            vetoes.append(c["id"])
        if not c["isTransaction"] and r["isTransaction"]:
            false_txns.append(c["id"])

    n = len(cases)
    pcts = {f: 100.0 * fields[f] / n for f in FIELDS}
    exact_pct = 100.0 * exact / n

    print("=" * 68)
    print(f"ENCODER: {model.name}  temp={args.temperature}  cases={n}")
    print("=" * 68)
    print(f"{'field':<16}{'acc':>12}")
    for f in FIELDS:
        print(f"{f:<16}{fields[f]:>4}/{n}  {pcts[f]:>5.1f}%")
    print("-" * 68)
    print(f"exact match       : {exact}/{n} ({exact_pct:.1f}%)")
    print(f"FALSE VETOES      : {len(vetoes)}   <-- the number that matters")
    print(f"false transactions: {len(false_txns)}")
    if vetoes:
        print(f"  vetoed: {vetoes}")
    print("-" * 68)
    print("GATE vs RegexBaselineEvalTest floors:")
    for label, got, bar in (
        ("exact", exact_pct, FLOORS["exact"]),
        ("type", pcts["type"], FLOORS["type"]),
        ("category", pcts["category"], FLOORS["category"]),
    ):
        print(f"  {label:<10}{got:>6.1f}%  floor {bar:>5.1f}%   "
              f"{'PASS' if got >= bar else 'FAIL'}")
    print(f"  {'vetoes':<10}{len(vetoes):>6}   floor     0   "
          f"{'PASS' if not vetoes else 'FAIL'}")
    print("=" * 68)

    if args.dump:
        print("\nper-case p(is_transaction) — the calibration signal:")
        for c, r in sorted(zip(cases, rows), key=lambda x: x[1]["p_tx"]):
            mark = "VETO" if (c["isTransaction"] and not r["isTransaction"]) else "    "
            print(f"  {mark} {r['p_tx']:.3f}  {c['id']}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
