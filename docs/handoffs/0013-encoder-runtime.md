# 0013 — 22 MB encoder replaces the 468 MB generator

**Date:** 2026-09-29 · **Branch:** `phase-0-aurora-foundation`
**Status:** the on-device runtime is now an embedding model. First runtime in
the project to pass the accuracy gate. **258 tests, 0 failures**, both lints
clean, native audit passing. Not yet verified on-device.

Follows `0012-typed-decision-classification.md`.

## 1. Why this session exists

0012 built the safety machinery and left the decision open. Its Task 5 said:
delete the model, or keep it and make it safe. The measurements pointed at
deletion but nothing had supplied a calibrated probability, so the honest
options were "remove the model" and "research a runtime that isn't a
generator".

The user chose a third framing — keep a model, but make it small — and pointed
at cactus-compute/needle. That led to measuring candidates on size, and the
size ordering turned out to be the least interesting thing about them.

## 2. The finding that decided it

Every generative candidate failed the same way, and the failure is
**destructive, not cosmetic**. `AiSignalFusion` nulls `amount` and `type` on a
"not a transaction" verdict, the pipeline's unresolved-fields gate then drops
the row, and the transaction is *deleted*.

Measured through the production pipeline (`FusedAiEval`, 65-case golden corpus):

| runtime | size | exact | category | **destroyed** | rescued |
|---|---|---|---|---|---|
| SmolLM2-135M | 100 MB | 29.2 % | 3.1 % | **43/46** | 0 |
| FunctionGemma-270M | 241 MB | 24.6 % | 36.9 % | **5/46** | 0 |
| Qwen2.5-0.5B (was shipped) | 468 MB | 29.2 % | 56.9 % | **8/46** | 1 |
| **MiniLM-L6-v2 (shipped)** | **22 MB** | **16.9 %** | **49.2 %** | **0/46** | **4** |

Two things worth recording because they cost time:

- **Size does not predict quality.** 43 → 8 → 5 → 0 across a 21× size range, in
  no monotonic relationship with size. The 4.7×-smaller SmolLM2 deleted *more*
  than Qwen.
- **Standalone and fused scores point opposite ways.** Qwen and SmolLM2 both
  score 29.2 % standalone and reach opposite conclusions. Only the fused view
  predicted the damage. Any future runtime decision must be made fused.

An encoder is the right shape for this job, and not because it is smaller. The
job is a closed-vocabulary classification over SMS text, not text generation, so
a decoder was always over-provisioned — and an encoder **cannot express the
destructive failure at all**, because it emits no text. There is no output in
which a spurious verdict can be fabricated, only a similarity score, which is
low honestly rather than high falsely.

## 3. What shipped

**`EmbeddingClassifier : OnDeviceClassifier`** — nearest-centroid over cosine
similarity, ONNX Runtime Mobile. Centroids are precomputed offline
(`tools/build_embedding_asset.py` → 23 KB asset) because encoding the ~44 seed
phrases at startup would cost 44 encoder passes before the first message.

- **`isTransactionProbability` is populated for the first time.** The field has
  existed since 0012 and every runtime reported null, so `mayDiscard` took the
  legacy unconditional-veto branch. It is now a softmax over the transaction
  centroids — the constraint built in 0012 is finally load-bearing.
- `merchant` is deliberately `null`: a centroid model has no notion of a
  merchant string, and inventing one from the nearest centroid's label would be
  a fabricated value shown to the user as fact.
- Seed phrases are hand-written from the category definitions, **not** sampled
  from the corpus — seeding and scoring on the same data would measure
  memorisation. A real deployment learns from `ClassificationMemory` instead.

**Removed:** `:llama`, `third_party/llama.cpp`, `llama-lib/`,
`LlamaCppLlm`, `LocalLlm`, `LlamaCppClassifier`, `QwenMessageCategorizer`, its
14 parsing tests, `ModelConsentDialog`, and `tools/needle_eval.py`.

**`UnrecognizedSmsScreen`** — the `unrecognized_sms` table shipped in 0012 with
**no UI**, so a message the app could not read vanished silently. That is the
exact failure the veto constraint exists to prevent, so leaving it unsurfaced
was the largest gap in the project. Shows the raw body only; filing by hand
opens the existing editor and dismisses the row.

**Consent dialog removed.** A blocking modal is not warranted for a 22 MB
download, and the app is fully functional without the model, so the offer is
inline on the classification tab. Three strings hardcoded `~380 MB` and were
silently wrong; all copy now reflects the real size, in both locales.

**Tokenizer download.** The encoder needs a WordPiece vocabulary as a second
file with its own pinned SHA-256. `isDownloaded` requires both, so a partial
download reports "not ready" and the UI keeps offering it.

## 4. Tooling problems found and fixed

- **`llama-cli` and `llama-completion` echo the prompt to stdout.** This
  prompt's second line is a JSON template containing `{`, so the balanced-brace
  scanner latched onto the echo and scored the *template*. The first
  FunctionGemma run reported `type 0.0 %` — a fictional number that looked real.
  Only the HTTP server returns the generation cleanly.
- **`FusedAiEval` dropped the probability.** `toExtraction` did not read
  `isTransactionProbability`, so the encoder was measured through the *legacy*
  unconditional-veto path — 1 deletion — and the fix moved it to 0. The harness
  was measuring a worse system than the one that ships and blaming the model.
- **`audit_native_runtime.sh` could not read a stripped `.so`.** It correctly
  refused to pass a clean result (good), but then flagged 79 arXiv/scipy URLs
  in kernel attribution comments and `N11onnxruntime9TelemetryE` — a mangled C++
  type name, not a service. Both were false positives that would have trained
  people to ignore the check. Now tries `nm` then `nm -D`, and filters
  documentation hosts and vendor telemetry names only. **Verified in both
  directions**: onnxruntime arm64 passes with zero network symbol imports.

## 5. Decisions

- **Removed llama.cpp rather than keeping it as a fallback.** The measured case
  was 468 MB → 22 MB with 8 → 0 deletions, and keeping a 165 MB vendored C++
  tree for a runtime that deletes transactions is not a fallback worth having.
- **ONNX Runtime pinned to 1.22.0.** 1.30 ships a 50 MB AAR for no capability
  used here; 1.20 fails lint because its arm64 `.so` is not 16 KB aligned.
- **Jev (TypeSafe AI) rejected.** Hosted, proprietary, waitlisted, no open
  weights — it would ship every bank SMS to a third party, contradicting the
  product's central promise. Its *shape* (typed values with calibrated
  probabilities) is what `isTransactionProbability` implements locally.
- **`type` left to the regex layer**, which measures 95.4 % against the
  encoder's 84.6 %. `AiSignalFusion` already takes the parser's answer on
  conflict; the encoder fills gaps.

## 6. Verification

| Check | Result |
|---|---|
| `./gradlew :app:compileFreeDebugKotlin` | ✅ |
| `./gradlew testFreeDebugUnitTest` | ✅ **258 tests, 0 failures** |
| `./gradlew :app:lintFreeDebug` / `:lintPlayDebug` | ✅ no new issues |
| `./gradlew :app:assembleFreeDebug` | ✅ |
| `FusedAiEvalTest` — shipped runtime destroys nothing | ✅ **0 transactions** |
| `tools/audit_native_runtime.sh` on onnxruntime arm64 | ✅ pass, zero network imports |
| Digest of the shipped encoder | ✅ matches the pinned SHA-256 |
| Host/tooling hygiene | 11 GB → 3.0 GB reclaimed |

## 7. Open

1. **The encoder has never run on a device.** Every number here is host-side ONNX
   Runtime on an arm64 Mac. The `arm64` quant build scored identically to the
   x86 one on the corpus, which is reassuring but is not the same as running on
   the phone. This is the first thing to verify.
2. **APK grew by ~18 MB of native code.** ONNX Runtime's arm64 library is in the
   APK whether or not a user opts in. The trade is 446 MB off the download for
   18 MB onto the install, which is favourable, but it is not free and it added a
   prebuilt-binary audit surface.
3. **Three bank parsers** (Axis, Canara, SBI). Other senders fall through to the
   global parser and, if unreadable, to the `unrecognized_sms` table.
4. **The corpus is 65 cases** and 7 of them exercise sender routing. The keyword
   map is not measured per-category.
5. **No DAO or migration test exists** (`exportSchema = false`).

## 8. Memory

- `AGENTS.md`: the destructive-veto invariant, now with a runtime that passes it.
- `CLAUDE.md`: score the system, never the model — with the Qwen/SmolLM2
  identical-standalone, opposite-fused example.
- `docs/evals/README.md`: the measured table, so a future proposal argues against
  a number rather than a summary line.
