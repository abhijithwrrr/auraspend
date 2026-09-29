# Decisions

Durable decisions with context. Latest first.

## D11 — Play's Feb 2027 R8 thresholds do not bind, and here is the number (2026-09-29)
Google requires ≥25% optimization/obfuscation/shrinking from Feb 2027, but only for
bundles over **10 MB uncompressed DEX**. AuraSpend's release bundle is **3.68 MB**
(R8 takes the debug build's 71.5 MB down to that), so the requirement does not apply —
and it clears the thresholds anyway at ~99% each. Verified by
`tools/verify_r8_release.sh`, which also confirms 16 KB alignment and that the JNI
classes survive un-renamed. Re-run it after touching dependencies or keep rules; it is
**not** in CI yet, deliberately deferred.

Corollary worth keeping: **the ONNX Runtime AAR ships no consumer ProGuard rules and
registers its natives dynamically in `JNI_OnLoad`**, which looks like a guaranteed
release crash. It is not — AGP's default file keeps any class declaring a `native`
method, names included. Do **not** "fix" this with a broad
`-keep class ai.onnxruntime.** { *; }`; it would suppress the obfuscation score for
nothing. Note the package is `ai.onnxruntime`, not `com.microsoft.onnxruntime`.

## D10 — Score the system, never the model (2026-09-29)
The measured lesson of the runtime bake-off, and the one most likely to be
relearned expensively. Qwen and SmolLM2 both scored **29.2% standalone** and
reached opposite conclusions once fused into the pipeline, because the pipeline
is where a veto becomes a deletion. Judge every candidate with `FusedAiEval`
against the fused regex floor (13.8% exact, type 95.4%, category 36.9%), not
with a standalone prompt benchmark. Corollary: **size does not predict
quality.** Across a 21× size range the destruction rate ran 43 → 8 → 5 → 0,
non-monotonic — the 4.7×-smaller SmolLM2 deleted *more* than Qwen.

## D9 — A model may never delete a transaction on a bare boolean (2026-09-12 → refined 2026-09-29)
`AiSignalFusion` nulls `amount` and `type` on a "not a transaction" verdict, and
the pipeline's unresolved-fields gate then drops the row — so a false veto
*deletes* a real debit rather than mis-filing it. Measured destruction rates:
43/46 (SmolLM2), 8/46 (Qwen2.5-0.5B), 5/46 (FunctionGemma-270M), **0/46 (the
shipped encoder)**. `mayDiscard` therefore requires *both*
`isTransactionProbability >= 0.9` *and* `base.amount == null`. Confidence alone
is not enough, because the regex layer reading a message correctly is the
stronger signal. A runtime that cannot supply a probability reports `null` and
keeps the old behaviour — never fabricate one, since a higher probability is
licence to discard. "Destroyed" outranks "exact match" in every model decision.

## D8 — 22 MB encoder replaces the 468 MB decoder (2026-09-29)
Five runtimes were measured through the production pipeline; a quantised
MiniLM-L6-v2 ONNX **encoder** won. It is the first runtime that is not a text
generator, which matters structurally: an encoder emits no text, so it cannot
fabricate a "not a transaction" verdict in the first place. It is also the only
one that supplies `isTransactionProbability`, the field `mayDiscard` has
required since D9. llama.cpp was removed entirely (`:llama`,
`third_party/llama.cpp`, `llama-lib/`, the GGUF prompt/parser,
`ModelConsentDialog`) rather than kept as a fallback, and the blocking consent
modal was replaced by an inline offer — a 22 MB download does not warrant a
modal. Handoffs 0011–0013.

## D7 — Distribution flavors carry no features, no analytics, no ads (2026-09-29)
Question: should the Play build add analytics, ads and premium gating that the
GitHub/F-Droid build lacks? A standard open-source model, and it was considered
rather than refused. Three findings decided it. (1) The app's promise is an
*on-screen* claim, not a policy page: onboarding says a local AI "never uploads
your data", so an SDK in one flavor makes the Play listing contradict the first
run screen of the same APK. (2) D4 already ruled on gating. (3) Both flavor
source sets were empty, so nothing needed unwinding. Both published builds are
feature-identical; flavors carry distribution config only. No feature may live
in `src/play/` that is absent from `main/` — that invariant is what makes the
free build incapable of being a degraded build, and F-Droid (which builds from
source and accepts only FOSS) requires it. See ADR 0008. Consequence accepted:
no crash insight beyond what users volunteer.

## D1 — Aurora purple replaces Cashew blue (2026-09-12)
The app icon and landing page already use purple (#5E3A8B / #6750A4 / #C4A6E6);
the in-app theme was Cashew blue (#1B447A). The rebuild adopts the existing
brand palette as the source of truth so app, icon and site finally match.
Dynamic color stays available but brand-first.

## D2 — Four tabs + center FAB; Settings behind header avatar (2026-09-12)
Bottom nav becomes Home / Activity / Plan / Insights with the FAB in the
middle. Settings moves behind the header avatar. Rationale: Budgets,
Subscriptions and Goals were buried in Settings or dead-end routes; the new
`Plan` hub makes them first-class while keeping four thumb-reachable tabs.

## D3 — material3 1.4.0 via Compose BOM 2026.09.00 (2026-09-12)
M3 1.4.0 is the newest stable and includes M3 Expressive APIs. BOM pins
Compose UI 1.12.1 and Material3 1.4.0 together. Alpha 1.5.x intentionally not
used.

## D4 — No paywall anywhere (2026-09-12)
PremiumGate, PremiumUpgradeScreen and the BillingManager stubs were unused or
stubbed; the free flavor unlocked everything already. For an Apache-2.0 project
the paywall undermined trust. Removed. `free`/`play` flavors remain for
distribution differences.

## D5 — Try/catch at every boundary is a hard rule (2026-09-12)
Every IO/platform call must catch exceptions, log via `AuraLog`, and degrade to
a typed fallback. `CancellationException` is always rethrown. Helpers live in
`core/AuraLog.kt`. See AGENTS.md.

## D6 — Phased rebuild with handoffs (2026-09-12)
The UI revamp is one phase per branch (P0–P6). Each phase ends with a handoff
doc and starts only after the previous gate passes. Agent memory
(`CLAUDE.md` + `memory/`) carries durable context between sessions.
