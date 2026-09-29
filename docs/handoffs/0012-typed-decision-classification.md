# 0012 — Typed-decision classification

**Date:** 2026-09-29 · **Branch:** `phase-0-aurora-foundation`
**Status:** Tasks 1–4 shipped. Task 5 executed: **the model is not deleted, and
the veto is still the open problem.** Every accuracy number is below.

Follows `0011-on-device-ai-foundation.md`. Plan:
`docs/superpowers/plans/2026-09-29-typed-decision-classification.md`.

## 1. Why this session exists

0011 ended with two things unproven. First, its NO-GO on Needle 3 was recorded
in a body that cited "Appendix A/B" which did not exist in the file, and whose
§9 simultaneously listed the device measurement as pending. Second — and this is
what drove everything — **no accuracy number had ever been measured for the model
the app actually ships.** The harness existed; it had only ever been pointed at a
scripted fake in CI.

So the first act was measurement, not change.

## 2. What the measurements said

`tools/gguf_eval.py` drives the **real** prompt (extracted from
`QwenMessageCategorizer.buildPrompt` so it cannot drift) through a host
`llama-server` built from the vendored submodule, and scores with the **real**
`parse()` rules. `FusedAiEval` then runs the same corpus through the **production
pipeline** — `isOtpOrAlertMessage` → `BankMessageParser.parse` →
`AiSignalFusion.fuse` → the unresolved-fields gate.

| runtime | exact | type | category | destroyed | rescued |
|---|---|---|---|---|---|
| regex only (no AI) | 13.8 % | 95.4 % | 36.9 % | 0 | — |
| **Qwen2.5-0.5B (shipped)** | **10.8 %** | 84.6 % | 38.5 % | **7** | 1 |
| SmolLM2-135M | 1.5 % | 33.8 % | 3.1 % | **40** | 0 |

**The model currently in the app makes it worse on four of five fields.**

Standalone, both models score an identical 29.2 % exact — and reach opposite
conclusions (Qwen mildly harmful, SmolLM2 catastrophic). That gap is entirely
`AiSignalFusion`.

A model "not a transaction" verdict nulls `amount` and `type`, the pipeline's
unresolved-fields gate then drops the row, and the transaction is **deleted,
not mis-filed**. Measured veto rates on real transactions: 7/46 (Qwen),
40/46 (SmolLM2).

### A trap worth recording

`llama-cli` and `llama-completion` both **echo the prompt to stdout**, and this
prompt's second line is a JSON template containing a literal `{`. A
balanced-brace scanner latches onto the echo and scores the *template*. The
first run reported `type 0.0 %` — a fictional number that looked like a real
result. `llama-completion` additionally mangles `/` into `/\x08/\x08`. Only the
HTTP server returns the generation cleanly, so the harness uses it and aborts if
it cannot confirm what it is reading.

## 3. Reference architecture: PennyWise

`sarim2000/pennywiseai-tracker` (498 stars, Kotlin) is the closest comparable
app. Reading its code rather than its README: **it makes zero LLM calls in its
SMS pipeline.** 200+ per-bank parser classes, routed by sender ID
(`BankParserRegistry.getParser(sender)`); a 907-line keyword map for category;
`LlmService` is conversation-only (the chat assistant).

That is the shape this session adopted. It is AGPL-3.0, so the routing idea was
learned and the code is ours.

## 4. What shipped

**Task 1 — calibrated veto seam.** `SmsExtraction.isTransactionProbability:
Float?`. Null = "this runtime cannot say". `mayDiscard` requires *both*
confidence ≥ 0.9 **and** that the regex layer found no amount; confidence alone
is insufficient, because the parser reading a message correctly is the stronger
signal. llama.cpp reports null, so this is **behaviour-preserving for the
shipped model** — the seam is the deliverable.

**Task 2 — sender-routed parsers** (Axis, Canara, SBI). The corpus gained
`sender` for the first time: free metadata the app already receives and never
captured, which is why routing had been unmeasurable. Canara's parser never
reads the `Dial 1930 to report cyber fraud` footer that decorates every genuine
Canara debit. Measured **7/7 type and 7/7 amount** on the routed cases.

**Task 3 — keyword category map.** Whole-word, longest-key-first, separate
income table (a refund from Swiggy is not a Swiggy expense). Resolves to `null`
on no match, never `cat_other`. Category **36.9 % → 38.5 %**, type unchanged.

**Task 4 — unrecognized SMS table.** A message carrying an amount and a
movement verb that no parser could read is recorded rather than dropped, reusing
the same amount+movement test `SmsAutoClassifier` uses to rescue a debit ending
in an OTP. Room v7→v8, backup format v3→v4 (tolerant; a v3 payload restores
empty), wired into the atomic Drive restore.

**Reusable, committed:** `FusedAiEval` + `FusedAiEvalTest` (null-model path must
reproduce `RegexBaselineEval` exactly — a fixed point), and a control test
pinning that a blanket veto destroys exactly the saveable set.

## 5. Decisions

- **Kept the model; did not delete it.** Task 5's bar is *zero destroyed
  transactions*, and Qwen still destroys 7. But deleting 468 MiB, twelve `.so`
  per ABI and the native-audit surface is a one-way decision that should not be
  taken while a cheap fix (a constrained veto) is untried and unmeasured.
- **Jev (TypeSafe AI) rejected.** Hosted, proprietary, waitlisted, no open
  weights. It would ship every bank SMS to a third party, contradicting the
  product's central promise. Its *shape* — typed values with calibrated
  probabilities — is what `isTransactionProbability` implements locally.
- **No further GGUF swap.** Two measured and rejected. A third 0.5B is not
  expected to change the verdict.
- **Sent `~/Downloads/transactions.csv` nowhere.** 1,000 rows of real names and
  UPI handles tied to phone numbers. The 250k-row file is synthetic and was used
  only to read the real category distribution; neither has message bodies, so
  neither is training data.

## 6. Verification

| Check | Result |
|---|---|
| `./gradlew :app:compileFreeDebugKotlin` | ✅ |
| `./gradlew testFreeDebugUnitTest` | ✅ **278 tests, 0 failures** |
| `FusedAiEval` after Tasks 1–4 | category 38.5 % (was 36.9 %); **7 destroyed, unchanged** |
| Per-bank parsers, routed corpus cases | ✅ 7/7 type, 7/7 amount |

## 7. Open

1. **The veto is still unfixed at the model level.** Task 1 built the seam;
   nothing yet *supplies* a probability, because llama.cpp cannot. The next real
   step is a runtime that can read one — which is what the plan's out-of-scope
   embedding/kNN classifier would do, and it remains a hypothesis with no
   measurement.
2. **Bank parsers cover three banks.** Real users bank elsewhere; those messages
   fall through to the global parser and, if unreadable, to the unrecognized
   table. Axis/Canara/SBI were chosen because the corpus has real production
   messages from them.
3. **The corpus is 65 cases** and now 7 of them exercise routing. The keyword
   map is not yet measured per-category.
4. **No DAO or migration test exists** (`exportSchema = false`). The v7→v8
   migration is unverified against a real v7 database.

## 8. Memory

- `AGENTS.md`: the calibrated-veto rule and the per-bank parser invariant.
- `CLAUDE.md`: the fused-vs-standalone distinction, which is the trap this
  project keeps falling into.
