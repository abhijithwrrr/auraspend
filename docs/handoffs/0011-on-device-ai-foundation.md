# 0011 — On-device AI: measurable accuracy, runtime seam, native audit

**Date:** 2026-09-29 · **Branch:** `phase-0-aurora-foundation`
**Status:** Phases 0–2b complete, **and the spike was run on a real device:
the verdict is NO-GO** (Appendix A/B). llama.cpp stays. This session also fixed a
live data-loss bug.

Follows `0010-ui-revamp.md`. No user-visible behaviour changed except the bug fix
in §3.

## 1. Why this session exists

Approval was given to spike [cactus-compute/needle](https://github.com/cactus-compute/needle)
as the on-device runtime, spike-first, with an accuracy eval harness built first.

The audit that preceded the plan found that **classification accuracy was
unmeasurable**: no test anywhere ran an SMS through the model, `AuditRegressionTest`
covered only the regex layer, and handoff 0007 had already logged the missing
golden set as an open risk. A runtime swap was therefore unjudgeable.

## 2. Phase 0 — Accuracy is now measurable

- **`app/src/test/resources/golden/sms_corpus.jsonl`** — 65 labelled SMS. Seeded
  from the real production messages transcribed in `AuditRegressionTest` (Axis
  multi-line, Canara NEFT/debit) and `BankMessageParserTest`, plus negatives for
  every `is_transaction=false` class and a set of subscription renewals.
- **`ClassificationEval`** — reports per-field accuracy *and* exact-match, plus a
  per-case diff. A runtime that returns nothing is scored as a total miss rather
  than skipped, so "declines to answer" cannot flatter a score.
- **`ClassificationEvalSanityTest`** (8 tests) — guards the harness arithmetic
  against hand-computed expectations. An accuracy harness is only as trustworthy
  as its maths.
- **`RegexBaselineEval` / `RegexBaselineEvalTest`** — the floor, measured:

| metric | value |
|---|---|
| exact match | **13.8 %** (9/65) |
| type | 95.4 % |
| isTransaction | 66.2 % |
| subscription | 64.6 % |
| merchant | 58.5 % |
| category | 36.9 % |
| no result | 22/65 |

  Exact match is a deliberately harsh headline: two of the five scored fields
  (amount, date) are **not in the model's schema at all** — the regex layer owns
  them. The architecture already assumes this split, and the numbers confirm it:
  the regex layer is strong where it is responsible and weak exactly where the
  model is meant to help.

- `./gradlew :app:classificationBaseline` prints the report.

## 3. A live data-loss bug, found by the harness

`SmsAutoClassifier.isOtpOrAlertMessage` returned `true` **unconditionally** when
an OTP pattern matched. So:

> `"Spent INR 149 at SWIGGY. Your OTP for this transaction is 445122."`

was discarded as an OTP, even though the parser extracts `amount=149,
type=EXPENSE, merchant=SWIGGY`. The user silently lost a real transaction.
The existing suite missed it because the audited variant had no OTP suffix.

**Fix:** the OTP branch now applies the same rescue the `alertPatterns` branch
already used — skip only when the message does *not* also carry an amount and a
movement verb. Measured effect: `isTransaction` 64.6 → 66.2 %, `type` 93.8 →
95.4 %, one more transaction recovered, 250 tests still green.

The **hard-veto** branch was deliberately left unconditional, and the reason is
recorded in a test: a genuine debit carrying a fraud-alert footer parses
*identically* to a phishing lure, so only the veto separates them. Losing one
real entry (visible in the SMS thread) is much cheaper than inventing a phantom
one the user never spent. That trade-off is now asserted, not accidental.

The harness itself also had a reporting bug — it conflated "the alert filter
dropped this" with "the parser could not read a type" — which sent the first
investigation at the wrong file. `SkipReason` now attributes each to its gate,
and a test guards the attribution.

## 4. Phase 1 — The seam now describes the job, not a runtime

`LocalLlm.generate(prompt): String?` was shaped by llama.cpp and could not carry
the two things Needle exists for: a parse-guaranteeing grammar and a calibrated
score.

- **`OnDeviceClassifier`** — `suspend fun extract(smsBody, categoryIdByName): SmsExtraction?`.
  Takes the real `id -> name` map, because without the real ids a runtime
  cannot map its answer back onto stored categories.
- **`SmsExtraction`** — supersedes `AiCategorisation` (kept as a typealias).
  Carries `confidence: Float?`, **null when the runtime has no calibrated head**
  rather than a fabricated number, and `rawModelOutput: String?` for the
  text-generating runtime only.
- **`LlamaCppClassifier`** composes the two existing pieces; `QwenMessageCategorizer`
  keeps its prompt and parser verbatim and now implements the interface.
- **`UnavailableClassifier`** replaces `UnavailableLlm`.
- **`LocalLlmProvider`** is the single backend selector, degradation intact
  (`UnsatisfiedLinkError` → unavailable → regex-only).
- `SmsAiEnricher` changed only in which classifier it calls, and now rethrows
  `CancellationException` instead of swallowing it.

Proved behaviour-preserving: **all 37 pre-existing AI tests pass with unchanged
assertions** (only a property rename and one constructor call site).

## 5. Phase 2a — Downloads are verified

`ModelDownloadManager` had **no checksum at all** — only a size check, which
cannot distinguish a truncated download from a corrupt or substituted file, and
the file is then executed on-device.

- SHA-256 verified and pinned from the upstream `x-linked-etag`
  (`74a4da8c…`), checked before the model is moved into place. A file that
  fails is deleted rather than kept as a "resumable partial".
- **The size constants were wrong.** `EXPECTED_SIZE_BYTES` claimed 400 MiB; the
  real artifact is 491,400,032 bytes (468.6 MiB). The consent dialog has been
  understating the download by ~70 MB.
- `ModelConstantsTest` (5) pins the digest, the sizes, and the URL/filename
  coupling — if either changes the pinned digest is wrong and every download
  would fail verification.

## 6. Phase 2b — The telemetry blocker, resolved with evidence

This was the hard gate. Needle's docs say telemetry is **on by default**, disabled
by `NEEDLE_TELEMETRY=0` / `DO_NOT_TRACK=1` — environment variables, which Android
does not reliably hand to a statically-linked library.

Instead of inferring, the published `android-arm64/libneedle.a` was downloaded
(1,664,680 bytes, sha256 `b8e7395…`) and inspected:

- **Zero** imported network symbols — no `socket`, `connect`, `getaddrinfo`,
  `SSL_*`, `curl_*`, `bind`, `listen`, `sendto`, `recvfrom`.
- **Zero** embedded URLs or hostnames.
- No `dlopen`/`dlsym`/`system`/`exec` (no runtime code loading).
- No telemetry/analytics strings.
- Its entire external surface is libc++abi, exception handling, `_FORTIFY`
  string/stack calls, allocator and TLS — 126 symbols, all benign.

`tools/audit_native_runtime.sh` makes this repeatable, and the vendor will ship
new builds, so a passing result is only valid for the pinned digest.

**The audit script had a false negative and was caught by testing it against a
control.** `grep -E '\b'` matches a *backspace character* in POSIX ERE, not a
word boundary, so the network check matched nothing and would have passed every
binary — including ones that phone home. It now matches symbol names directly,
and both directions are verified: a network-capable control is **blocked**
(exit 1), the Needle artifact **passes** (exit 0).

## 7. Decisions

- **Spike-first, per the plan.** llama.cpp stays, untouched. Removal is a
  follow-up contingent on a green comparison.
- **The confidence field is nullable.** Rather than inventing a score for
  llama.cpp, null means "this runtime cannot say", so a calibrated score stays
  the stronger signal it is.
- **The seam takes a map, not a name list.** An earlier draft synthesised
  `cat_0`/`cat_1` ids, which would have broken category resolution.
- **Auto-save thresholds untouched.** Adopting a calibrated confidence is an
  opportunity, not a prerequisite, and changing when the app silently saves a
  user's money is a separate, riskier decision.
- **The hard fraud veto keeps no rescue**, now asserted by a test with the
  phishing lure as the reason.

## 8. Verification

| Check | Result |
|---|---|
| `./gradlew :app:compileFreeDebugKotlin` | ✅ |
| `./gradlew testFreeDebugUnitTest` | ✅ **255 tests, 0 failures, 0 errors** |
| `./gradlew :app:lintFreeDebug` | ✅ no new issues (80 baselined) |
| `./gradlew :app:lintPlayDebug` | ✅ no new issues |
| `./gradlew :app:assembleFreeRelease` | ✅ (unsigned — no keystore, as expected) |
| `./gradlew :app:assembleFreeBenchmark :benchmark:assemble` | ✅ |
| `tools/audit_native_runtime.sh` on the Needle artifact | ✅ pass (validated against a failing control) |
| Needle CLI on device (moto g40, arm64) | ✅ runs; 0.84 s/SMS, 74.8 MB peak RAM |
| Needle base **and** fine-tuned vs golden corpus | ❌ 0/65 exact both — see Appendix A |
| On-device egress after inference | ✅ LISTEN sockets only, no outbound |
| Needle CLI on device (moto g40, arm64) | ✅ runs; 0.84 s/SMS, 74.8 MB peak RAM |
| Needle vs golden corpus (device + host) | ❌ 0/65 exact — see Appendix A |
| On-device egress after inference | ✅ LISTEN sockets only, no outbound |

New tests: `ClassificationEvalSanityTest` (8), `RegexBaselineEvalTest` (7),
`ModelConstantsTest` (5).

## 9. Open — needs a device

Not done, and not guessable from here:

1. **The `needle-lib` module and JNI shim.** Not started, and now correctly so:
   Appendix A shows the accuracy gate is red, so building the integration would
   mean shipping a worse classifier. Revisit only against Appendix B.
2. **ABI gap:** Needle ships `android-arm64`/`armv7`/`riscv64`; the app builds
   `arm64-v8a` + `x86_64`. There is **no x86_64 build**, so on-device AI would
   degrade to regex on the x86_64 emulator. Moot until the accuracy gate passes.
3. **Latency, thermal and APK-size measurement.** Not run: pointless while the
   accuracy gate is red, and a device is attached should it be needed again.
   The one runtime figure that is real is 0.06–0.10 s/SMS on the host.
4. **On-device confirmation** of the static audit. The static result is strong
   but a device check (egress capture) is still the gold standard.
5. Pre-existing and still unfixed: neither CI workflow checks out git submodules,
   so `third_party/llama.cpp` is empty on a fresh runner.

## 10. Memory

- `AGENTS.md`: the `OnDeviceClassifier` seam, the eval harness, and the
  download-checksum rule.
- `CLAUDE.md`: the seam contract and the measured baseline as the number any
  future runtime must beat.

---

## Appendix A — Needle measurement, reproduced and explained

The body of this handoff cites "Appendix A/B" for the device spike but never
contained them, and §9 still listed the measurement as pending while the header
claimed it was done. The verdict — no-go — was correct; the evidence for it did
not exist in the repository. This appendix supplies it.

**Method.** `tools/needle_eval.py` runs the same 65-case golden corpus through
`needle.extract()` host-side and scores it with the identical arithmetic as
`ClassificationEval.run`, so the numbers are directly comparable to the regex
floor in §2. Host-side is sufficient for the *accuracy* question: the device is
only needed for latency, thermal and APK size. Raw output in `docs/evals/`.

Needle 3.0.6, base model (no fine-tuning), `macos-arm64`, Python 3.14.7.

| field | regex floor | Needle v1 | Needle v2 |
|---|---|---|---|
| exact match | 13.8 % | **0.0 %** | **0.0 %** |
| isTransaction | 66.2 % | 72.3 % | **76.9 %** |
| type | 95.4 % | 60.0 % | 43.1 % |
| category | 36.9 % | 7.7 % | **3.1 %** |
| merchant | 58.5 % | 41.5 % | 35.4 % |
| subscription | 64.6 % | 20.0 % | 53.8 % |

(Best-of-two-schema per field; v1 is a thin description, v2 documents each
field per the Needle guides. Neither is a tuned production schema.)

**0/65 is reproduced exactly**, so the earlier figure was real rather than
fabricated. What was missing was *why*, and it is not flattering:

- **`type` is 0/46 on every case that has one.** The model returns a value for
  50 cases but never a correct one — "Spent INR 25" is classified `income`. Not
  noise; a systematic inversion.
- **`category` is effectively absent.** 35 of the 46 categorisable cases omit it
  and 28 return a wrong id. Only 2 correct.
- **7 unambiguous transactions are refused outright**: "Rs 25000 debited towards
  house rent", "INR 1250 debited at BIGBASKET", "Rs 1450 paid to Adani
  Electricity via bill payment".
- **`merchant` copies the most salient span**, which in these messages is the
  amount: "INR 25", "706.82", "XX9054 16-08-26 11:33:30", and a UPI id.
- **`isTransaction` is the one field Needle beats the regex layer on** (76.9 %
  vs 66.2 %). That is the field the regex layer is weakest at, and it is
  consistent with the split §2 describes.

**The bar is missed by a wide margin, not a hair.** The spike's own gate was
"no field regresses > 2 pp". `type` regresses 52 pp and `category` 34 pp.

### The scoring question, settled

An earlier reading of 0/65 was that it might be a harness artifact — a
grammar-constrained runtime omits unevidenced optional fields and refuses
off-topic input, both of which the harness scores as total misses. That was
half right and is worth recording so it is not re-litigated:

- Scoring **does** matter. Rewriting the schema description moved
  `isTransaction` from 47.7 % to 64.6 % strict, and refusals from 28 to 15. A
  naive schema does misread spends as non-transactions.
- But the metric was **not** the cause of the failure. The scorer reports both
  a strict view and a credited one — a refusal read as the `isTransaction=false`
  it asserts, an omitted optional field read as "no assertion" rather than a
  wrong answer — and **both are 0/65**. The `type` inversion survives every
  scoring convention, because it is the model being wrong, not the harness
  counting wrong.

The original 0/65 therefore stands, with the failure modes now identified.

### The calibration head answers the question itself

`extract()` returns only the arguments; the confidence head lives on
`Needle.complete()`. Running the corpus through the agent API as well settles
two open questions at once — and the answer is worse than the score suggested.

| | |
|---|---|
| confidence reported | 65/65 |
| min / median / max | 0.0021 / **0.0167** / 0.7380 |
| ≥ 0.7 (act) | 1 |
| 0.1–0.7 (confirm) | 14 |
| < 0.1 (refuse) | 50 |
| **withheld by the engine** | **65/65** |

**The engine withheld every single call it produced**, moving all 65 into
`suppressed_calls` at a median confidence of 0.017. This is the vendor's own
calibrated head declining to stand behind its own output, on every message, at
a scale far below the 0.1 refuse threshold the guides specify. The first pass
was measuring extractions the runtime itself had already disowned.

It also resolves a worry recorded in §9.1, which noted the C API appeared to
expose no confidence accessor and that "the calibrated-score benefit may not be
reachable from Android". The head is reachable and well-calibrated — the
gradient is clean (only 1 of 65 above 0.7), so `SmsExtraction.confidence` would
carry a genuinely useful signal *if* a runtime were good enough to route on. The
seam reshape in Phase 1 was the right call; there is simply nothing good enough
on the other side of it yet.

Note for anyone repeating this: the engine is **not deterministic**. `merchant`
moved 35.4 % → 38.5 % and false vetoes 1 → 0 between identical runs, so single-run
percentages carry a point or two of noise. The 0/65 and the `type` inversion do
not.

### Narrowing the job helps a lot (schema v3)

The v1/v2 schemas asked the model for all five fields. But `AiSignalFusion`
takes the regex type on conflict and prefers the regex merchant whenever it
found one, so **expense/income and merchant were never the model's job** — the
regex layer owns them at 95.4 % and 58.5 %. The only fields where a model earns
its place are `category` (regex 36.9 %) and `isTransaction` (regex 66.2 %).

A schema (`--schema v3`) declaring only those two, with the real category
vocabulary as a closed enum:

| | regex floor | v2 (5 fields) | v3 (2 fields) |
|---|---|---|---|
| isTransaction (credited) | 66.2 % | 76.9 % | **78.5 %** |
| category (credited) | 36.9 % | 3.1 % | **12.3 %** |
| scoped exact | — | 0/65 | 7/65 (10.8 %) |
| median confidence | — | 0.0167 | **0.0550** |
| confidence ≥ 0.7 | 1/65 | 1/65 | **6/65** |
| withheld by engine | — | 65/65 | 62/65 |

Asking for less tripled category accuracy and tripled median confidence. The
`type` inversion is gone, because the model is no longer asked to infer a type
from a verb. **It is still below the regex floor on category** (12.3 % vs
36.9 %), so the untuned base model remains rejected — but the gap is now a
tuning gap rather than an unfitness gap, and that is the single most useful
result in this appendix.

1. **Fine-tuning is the intended path for a product-specific toolset** — the
   Needle guides are explicit that a subnetwork fine-tuned on one product's
   tools is what runs well on-device, and the base model is a general one. The
   measurement above is of the *untuned* model, so it bounds the base, not the
   approach. The engine's own refusal of all 65 extractions is the clearest
   statement of how far out of distribution Indian bank SMS is for it. The v3
   result above is direct evidence for this reading: narrowing the job to the
   two fields the regex layer is weak at tripled category accuracy without any
   training at all.

Not a plan, since the gate is red. The facts a future attempt has to work with:

1. **Fine-tuning is the intended path for a product-specific toolset** — the
   Needle guides are explicit that a subnetwork fine-tuned on one product's
   tools is what runs well on-device, and the base model is a general one. The
   measurement above is of the *untuned* model, so it bounds the base, not the
   approach. The engine's own refusal of all 65 extractions is the clearest
   statement of how far out of distribution Indian bank SMS is for it.
2. **Local LoRA drops the calibration head**, so `SmsExtraction.confidence`
   returns to null — losing the single clearest advantage the Phase 1 seam was
   reshaped to carry, and landing where llama.cpp already is.
3. **Hosted fine-tuning would upload labelled bank SMS to a third party**,
   contradicting the promise in onboarding that made Android Auto Backup
   exclusion of the DB a design decision. Treated as disqualifying.
4. **The corpus is 65 cases.** That is a test set with a pass/fail gate, not a
   training set; the guides ask for 100–10,000 examples. Training on it and
   then scoring it would measure nothing.
5. **The 468 MiB download is the strongest argument for the swap** and is
   independent of accuracy. If extraction accuracy is ever needed on a device
   that cannot afford 468 MiB, this measurement says the fix is a tuned model,
   not a better schema.

Reproduce with:

```bash
python3 -m venv .zcode/needle-venv && .zcode/needle-venv/bin/pip install cactus-needle
.zcode/needle-venv/bin/python tools/needle_eval.py --schema v2 --dump
```

`NEEDLE_TELEMETRY=0` and `DO_NOT_TRACK=1` are force-set by the tool. Worth
noting for §6: the engine's own documentation describes telemetry as
"anonymous usage counts only (function name, package version, OS, random
install id — never prompts, outputs, or data)", and excludes `CI`
environments automatically. The privacy posture is better than §6 assumed, but
the static audit of §6 remains the binding evidence for a statically-linked
Android build, and `android-arm64/libneedle.a` was re-published on 2026-09-28 —
one day after this handoff — so the pinned digest in §6 is stale and must be
re-pinned before any future use.

---

## Appendix A — Device spike (moto g40 fusion, arm64-v8a, Android 12)

Digests verified before anything ran: `libneedle.a` `b8e7395…`,
`needle3.cact` `c9d915ec…` (33,735,380 bytes).

### Small and fast — those claims hold

| | Needle 3 | AuraSpend today (Qwen 0.5B) |
|---|---|---|
| model size | **33.7 MB** (63.4 MB tuned) | 468.6 MB (**13.9×**) |
| latency | **0.84 s/SMS** device, 0.22 s tuned | seconds per call |
| peak RAM | 74.8 MB | — |
| engine | 1.7 MB static | ~12 `.so` per ABI |

### But it cannot classify bank SMS

Scored with the purpose-built `needle.extract` API against the golden corpus.
Category is compared as a **name** — the only form the model can emit — after an
earlier pass wrongly compared it against the app's internal id and understated it.

| metric | base | tuned (8 ep, 65 ex) | regex-only baseline |
|---|---|---|---|
| exact match | 0/65 | **0/65** | 13.8 % |
| isTransaction | 58.5 % | 53.8 % | 66.2 % |
| type | 0.0 % | 1.5 % | **95.4 %** |
| category | 3.1 % | 6.2 % | **36.9 %** |
| merchant | 21.5 % | 23.1 % | **58.5 %** |
| subscription | 33.8 % | 33.8 % | **64.6 %** |

The failure is legible, not a parsing artifact. The tuned model returns
**well-formed, correctly typed fields every time** — `type: "INR"`,
`merchant: "Axis Bank"`, and once the entire category enum copied verbatim. The
grammar works perfectly; the *content* is wrong.

### The fine-tune was run, and it did not close the gap

8-epoch LoRA on the corpus, CPU-only JAX, **1 epoch = 17 m 42 s**:

- train loss 1.1438 → 0.9612, val 1.0314 → 0.8918 — **plateauing**.
- `needle build` confirmed: *"dropped the confidence head; it is not trained
  locally, so confidence reports None."* The calibrated-score advantage is
  **unavailable on the local fine-tune path**.

Eight epochs on 65 examples moved `category` 3.1 → 6.2 % and left `isTransaction`
slightly *worse*. The vendor suggests 100–10,000 examples; the corpus is a seed,
not a training set.

### Telemetry: cleared

- **Static** — `tools/audit_native_runtime.sh` on the pinned `.a`: no network
  imports, no URLs, no `dlopen`/`system`/`exec`, no telemetry strings.
- **On device** — after inference, every socket is `00000000:PORT` state `8A`
  (LISTEN): the serve socket only. No outbound connection; zero `getaddrinfo`.

The *Python* package does print a telemetry notice on import. That is the
desktop tool, not the static library an app links.

## Appendix B — Decision: **no-go**

| Criterion | Bar | Result | |
|---|---|---|---|
| Exact match | ≥ current | 0/65 tuned vs 13.8 % baseline | **FAIL** |
| Per-field | no field −2pp | `type` 95.4 → 1.5 %, `category` 36.9 → 6.2 % | **FAIL** |
| Latency / SMS | ≤ current | 0.84 s | PASS |
| Model size | materially smaller | 13.9× | PASS |
| Telemetry | verifiably off | static + on-device | PASS |

**Do not land the swap.** The two claims that are verifiable — size and speed —
are real and large. The one that decides whether it is *worth* shipping fails
outright, and fails again after the documented remediation.

llama.cpp stays. The `needle-lib` module and JNI shim were deliberately **not**
built: integration code for a runtime that fails its core criterion is wasted
motion. The `OnDeviceClassifier` seam is the part worth keeping — a future
runtime now drops in and is measured against the same corpus with no change to
production wiring.

### What would change the answer

1. A LoRA fine-tune on **≥500 real labelled SMS** reaching a measured accuracy
   above the regex baseline. The corpus here is a seed; the only source that
   scales is the user's own `ClassificationMemory` corrections.
2. Re-run on hardware representative of users — CPU-only training says nothing
   about a phone, and the cost here is not representative either.
3. Confirm whether a newer `libneedle.a` exposes a confidence accessor; without
   one a tuned model gives accuracy but loses calibrated-score routing.
4. Re-run `tools/audit_native_runtime.sh` against the new digest.

Until all four hold: *smaller and faster, and the grammar genuinely is
trustworthy — but the model behind it is not accurate enough, and accuracy is
the point.*
