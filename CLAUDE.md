# Memory — AuraSpend

Hot cache for agents. Keep this under ~100 lines. Deep detail lives in `memory/`.
**Start every session by reading `AGENTS.md` + the latest `docs/handoffs/` file.**

## Project
AuraSpend — offline-first Android expense manager (Kotlin, Jetpack Compose,
Room, a 22 MB on-device encoder via ONNX Runtime). **AGPL-3.0-or-later** as of
0.1.1 (was Apache-2.0; see ADR 0009). Do not weaken the copyleft, and do not
"fix" third-party Apache-2.0 mentions — those are correct.
Repo: `github.com/auraspend/auraspend`. Branch strategy: `main` is
release-ready (protected, default); day-to-day work lands on `dev`. Work
branches come off `dev` (`phase-N-name` or `feat|fix|chore|docs|perf|refactor/<topic>`)
and are deleted once merged.
Personal identifiers must never be committed: history is scrubbed, the signing
key is `CN=AuraSpend`, and commits use `AuraSpend <auraspend@users.noreply.github.com>`
(handoff 0020).

## Glossary
| Term | Meaning |
|------|---------|
| **Aurora** | The new design language + theme (purple/lavender/teal, Plus Jakarta Sans) |
| **Phase N (P0–P6)** | Rebuild phases; see `docs/handoffs/0000-master-plan.md` |
| **Handoff** | End-of-session doc in `docs/handoffs/NNNN-*.md` |
| **Gate** | Objective exit criteria a phase must pass before the next starts |
| **Activity** | The transaction list tab (new IA name) |
| **Plan hub** | Tab grouping Budgets + Subscriptions + Goals (P3) |
| **Insights** | Analytics + insight feed tab (P3) |
| **Triage inbox** | Swipe accept/reject queue for auto-detected SMS (P2) |
| **Shim** | `ui/core/CashewComponents.kt` — Phase 0 compat layer, deleted after P3 |
| **Boundary** | try/catch wrapper (`boundary {}` in `core/AuraLog.kt`) required at every IO edge |
| **AuraCard / tokens** | Core design-system primitives (`ui/designsystem/`) |
| **Flavor** | `dev` (development, `.dev` id, "AuraSpend Dev" label) / `prod` (ships) — **feature-identical, no analytics/ads in either** (ADR 0010) |

## Active work
| Item | State |
|------|-------|
| **Aurora rebuild + backlog** | ✅ Complete — `docs/handoffs/0006-backlog-completion.md` |
| **Classification accuracy pass** | ✅ Complete — `docs/handoffs/0007-classification-accuracy.md` |
| **Crash paths + CI safety nets** | ✅ Complete — `docs/handoffs/0009-crash-paths-and-safety-nets.md` |
| **UI revamp (locale/type/theme/states/a11y)** | ✅ Complete — `docs/handoffs/0010-ui-revamp.md` |
| **On-device AI: eval + seam + download integrity** | ✅ Complete — `docs/handoffs/0011-on-device-ai-foundation.md` |
| **Needle runtime swap** | ❌ **No-go, measured** — 0/65 exact, `type` 0/46, engine withheld all 65 calls at median confidence 0.017. See 0011 App. A/B |
| **Typed-decision classification** | ✅ Tasks 1–4 shipped — 0012. Calibrated veto seam, sender-routed bank parsers (Axis/Canara/SBI), keyword category map, unrecognized-SMS table |
| **Encoder replaces the generator** | ✅ 0013 — MiniLM-L6-v2 int8 (22 MB) on ONNX Runtime replaces Qwen 468 MB. **Zero transactions destroyed, 4 rescued.** llama.cpp removed. Not yet run on-device |
| **Next: error-handling debt** | `CancellationException` swallowed at most catch sites; `runBlocking` in `LlamaCppLlm`; dead duplicate-SMS guard; no DAO/migration tests (`exportSchema = false`) |
| **Play Store (0.1.1)** | ✅ Staged, **not submitted** — see `docs/handoffs/0017-play-console-setup.md`. Launch needs: send-for-review → SMS permission justification at review → 12 testers × 14 days → apply for production. Listing assets + strings: `docs/store/` |
| **dev/prod flavors** | ✅ 0018 — four variants (`devDebug`…`prodRelease`); `benchmark` build type removed (macros run vs prod release); F-Droid/site claims audited |

## Hard-won invariants (do not regress)
- **CSV export writes to the SAF uri, never public Downloads** (minSdk 30 forbids it).
- **Drive restore = atomic replace** via `BackupRestoreManager` (`withTransaction`).
  Not a merge. `BackupData` is format v3 and must carry every user-owned table.
- **Auto Backup excludes `auraspend_db`** (`res/xml/backup_rules.xml`) — the app
  promises data stays on-device.
- **Every write path goes through `TransactionRepositoryImpl.sanitized()`**
  (the only `SensitiveDataMasker` choke-point). Restore re-masks too.
- **ViewModels use `viewModel(factory = ...)`**, never `remember { }` — see the
  `factoryOf` helper in `NavGraph.kt`.
- **`assembleProdRelease` is only shippable if the APK is not `-unsigned`**
  (keystore in gitignored `secrets.properties`).
- **Lint:** 78 pre-existing issues are baselined in `app/lint-baseline.xml`;
  new ones fail CI. Do not casually run `updateLintBaseline`.
- **Money is never hardcoded.** `formatMoney` (`ui/designsystem/Money.kt`)
  implements Indian lakh/crore grouping by hand — `NumberFormat` returns Western
  grouping for `en_IN`/`hi_IN` on this JDK. Currency comes from the theme.
- **Design-system rules are build-enforced** by `DesignSystemGuardTest`
  (no raw colors/fontSize/shadow/emoji, missing error state, empty catch) and
  `ErrorStateWiringTest` (a screen must render the `error` it declares; no raw
  `.message` in state — errors are typed `UiError`, logged via `AuraLog`, and
  resolved from `strings.xml` at render time).
- **`strings.xml` needs `\'` for apostrophes** — a bare `'` fails the resource
  build with a misleading "Invalid unicode escape sequence".
- **Theme default is `SYSTEM`**; parse prefs with `AppThemeMode.fromName`.
- **Every string needs a Hindi twin** in `values-hi/strings.xml`.

## Classification hot spots
- `keywordCategoryFor` (DefaultCategories.kt) — word-boundary, longest-key
  keyword scan; NEVER add substring-unsafe keys ("fee"/"credit"/"vi" caused
  real misfiling).
- `MerchantRepository.resolveMerchant` — normalized + token + phrase tiers;
  CSV fields are unquoted in `install()`; wallets/card networks removed on
  purpose (they forced wrong categories).

## Tooling
- Releases: `.github/workflows/release.yml` — a `v*` tag (or manual dispatch)
  builds the **signed** Play AAB + prod APK, names them
  `AuraSpend-V<version>.Alpha.{aab,apk}`, writes `SHA256SUMS`, and attaches
  everything to a GitHub Release via the Release Drafter draft. It **never
  uploads to Google Play** — sending for review stays a human click in the
  console. Signing comes from four repo secrets (`KEYSTORE_BASE64` etc.; the
  upload key, alias `key0`). Next uploaded versionCode after 2 (0.1.1) is **3**.
- Store assets: `docs/store/` — the composed Play screenshots (seven 1080×1920
  ads), feature graphic + its HTML source, raw 1080×2400 captures, and the
  editor project state (`app-store-screenshots.json`) + deterministic exporter
  (`export.js`). The editor itself lives at
  `~/projects/auraspend-store-assets/` (keep it out of `docs/` — that
  is the Pages root). Re-export with `node tools/export.js android dist/android.zip`.
- Benchmarks: `./gradlew :benchmark:connectedProdBenchmarkAndroidTest`
  (runs against the prod **release** build; needs the release keystore locally)
- Baseline profile: `app/src/main/baseline-prof.txt` (generate via the `:benchmark` BaselineProfileGenerator)
- Design system: `ui/designsystem/`; ADRs in `docs/adr/`
- Master plan: `docs/handoffs/0000-master-plan.md`. Paywall removed in P0
  (PremiumGate, PremiumUpgradeScreen, BillingManager deleted). Public pages
  (`docs/index.html`, `docs/terms.html`, `docs/privacy.html`) state the same
  no-purchase, no-tracking promise as the app — keep them true.

## Locked decisions
- Aurora purple brand (matches app icon + landing page), not Cashew blue.
- 4 tabs + center FAB; Settings moves behind header avatar in P1.
- `material3 1.4.0` stable via Compose BOM `2026.09.00` (Compose UI 1.12.1).
- No premium/paywall anywhere; `dev` and `prod` flavors remain
  **feature-identical** — dev only adds a `.dev` id and "AuraSpend Dev" label,
  and no build may carry analytics, ads or telemetry (ADR 0007, ADR 0010).
- Try/catch at every boundary is a hard rule (see `AGENTS.md`).

## Preferences / rules
- Verify with `./gradlew :app:compileProdDebugKotlin` + `testProdDebugUnitTest` +
  `:app:lintProdDebug` before claiming done.
- Visual changes need a manual look in all 3 themes; there is no automated
  screenshot gate (removed in 0024).
- Keep commits small and conventional; never commit a red build.
- CI (`.github/workflows/pr_check.yml`) runs tests, both lints, both debug
  assembles, and `:benchmark` — keep it green, not just locally.
- **On-device AI:** `OnDeviceClassifier` is the only runtime seam. Accuracy floor
  with no model is **13.8 % exact match** (`./gradlew :app:classificationBaseline`);
  any new runtime must beat it. Golden corpus:
  `app/src/test/resources/golden/sms_corpus.jsonl`. Prebuilt native libs must
  pass `tools/audit_native_runtime.sh`.
- **Needle was measured and rejected — do not re-litigate from the summary.**
  `tools/needle_eval.py` reproduces 0/65 and names the failure modes. Re-run it
  before proposing the swap again; a new argument needs a new number.
- **Score the SYSTEM, never the model.** A standalone model score and a fused
  one can point opposite ways: Qwen and SmolLM2 both score 29.2 % standalone
  and one is mildly harmful while the other is catastrophic. `FusedAiEval` runs
  the production path; a green unit suite proves the *parser*, never the model.
  Measured table: `docs/evals/README.md`.
- **A non-generative model for classification.** The job is a closed-vocabulary
  decision over SMS text, so `EmbeddingClassifier` (nearest-centroid over
  cosine) beats a decoder LM on every field at 1/21st the size — and cannot
  express a destructive "not a transaction" verdict, because it emits no text.
  Centroids are precomputed by `tools/build_embedding_asset.py`; seed phrases
  are hand-written, never sampled from the corpus it is scored on.

→ Full glossary: `memory/glossary.md` · Phases: `memory/projects/aurora-rebuild.md`
→ Stack + commands: `memory/context/tech-stack.md` · Decisions: `memory/decisions.md`
