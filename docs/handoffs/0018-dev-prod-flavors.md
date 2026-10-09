# 0018 — dev/prod flavors (four variants) and a public-surface audit

**Branch:** `phase-agpl-oss-readiness` · **Date:** 2026-10-10

## What changed

### 1. Build flavors: `free`/`play` → `dev`/`prod`, exactly four variants

- `app/build.gradle.kts`: flavor dimension `env`; **dev** gets
  `applicationIdSuffix ".dev"`, `versionNameSuffix "-dev"` and an
  "AuraSpend Dev" label (`app/src/dev/res/`, EN + HI); **prod** is plain — the
  only distributed build. Variants are now exactly `devDebug`, `devRelease`,
  `prodDebug`, `prodRelease`.
- The `benchmark` build type was **removed from `:app`**. The macrobenchmark
  module keeps its `benchmark` variant and links the app's **prod release**
  build via `matchingFallbacks`.
- `app/src/free` → `app/src/dev`, `app/src/play` → `app/src/prod` (both still
  manifest-only source sets).
- CI (`.github/workflows/`): `testProdDebugUnitTest`, lints on both debug
  flavors, builds dev/prod debug, and compiles
  `:app:assembleProdRelease :benchmark:assemble` so profile drift stays visible.
- `tools/seed_demo_data.sh` targets the dev flavor (`com.awbuilds.auraspend.dev`;
  the seeder class keeps its source package, which is NOT derived from the
  app id). `tools/verify_r8_release.sh` now defaults to `prod`.
- Docs: README, CONTRIBUTING, PR template, memory files, CLAUDE/AGENTS, ADR
  0010 (new) with an annotation on ADR 0008 (kept as written).

### 2. Release workflow follows prod

`release.yml` builds `:app:bundleProdRelease :app:assembleProdRelease` and
attaches `AuraSpend-V<version>.Alpha.{aab,apk}`. The workflow itself landed
earlier in the same session — see handoff 0017.

### 3. Public-surface audit (README, website, legal pages)

- **F-Droid claims removed** from README, `docs/index.html` and
  `docs/terms.html` — the app is not on F-Droid (f-droid.org returns 404 for
  the package). The site now says "build it yourself from source".
- **`secrets.properties.example` dropped `WEB_CLIENT_ID` / `DRIVE_API_KEY`** —
  nothing in the build or code ever read them; the README setup section no
  longer tells contributors to create Google Cloud credentials they do not need.
- `docs/index.html`: SDK stat corrected **Android 16 → Android 17** (API 37);
  `og:image` / `twitter:image` added (the `summary_large_image` card had no
  image).
- README: stale LLM-era fusion wording replaced with the actual rule (veto only
  at ≥ 0.9 **and** no parser amount); commands and screenshot provenance
  updated to dev/prod.
- `memory/context/tech-stack.md`: llama.cpp rows replaced with the ONNX Runtime
  reality (the module was removed in 0013).
- Link scan over README, CHANGELOG, the three site pages, ADR index: **0 broken
  relative links**.

### 4. Lint baseline no longer churns on version releases

`lintProdDebug` failed on 7 errors that were all "newer version available"
nags: their messages embed exact version numbers, so a Gradle release
(`9.8.0` → `9.8.1`) plus five library releases broke a baseline that had pinned
the older text. `GradleDependency`, `NewerVersionAvailable` and
`AndroidGradlePluginVersion` are now **informational** — Dependabot owns
updates — and the baseline was regenerated deliberately: **84 → 78 entries**,
diff reviewed (line refreshes, resolved/stale entries dropped).

## Decisions

- **dev is a development environment, not a debug menu.** It may not carry
  features absent from prod — that is a flavor-only capability in reverse and
  ADR 0008/0010 forbids it. Configuration (id, label, signing) only.
- **Benchmarks measure the shipping artifact.** Removing `benchmark` costs
  "run benchmarks with only the debug keystore"; the win is one fewer variant
  and a benchmark of the exact prod release users run.
- **Tests and lint run on `prodDebug`** in CI, matching what ships.
- ADR 0010 records the rename; ADR 0008 is annotated, not rewritten.

## Verification (measured)

| Check | Result |
|---|---|
| `./gradlew :app:compileProdDebugKotlin testProdDebugUnitTest :app:lintProdDebug :app:assembleDevDebug :app:bundleProdRelease :app:assembleProdRelease :benchmark:assemble` | **BUILD SUCCESSFUL** |
| Unit tests (prodDebug) | **274 tests, 0 failures** |
| Artifacts | `app-dev-debug.apk` built; `app-prod-release.aab` + `app-prod-release.apk` built and **signed** (`jarsigner` / `apksigner` exit 0) |
| `:app:lintProdDebug` | "Lint found no new issues (57 errors, 20 hints filtered by the 78-entry baseline)" |
| `tools/check_license.sh` | exit 0 — licence metadata consistent |
| Relative links | 0 broken |

## Open risks / next step

- **Local macrobenchmarks now need the release keystore** (`secrets.properties`),
  because `:benchmark` links the prod release build. Documented in
  `benchmark/build.gradle.kts` and CLAUDE.md.
- **Drive sign-in in dev** needs its own OAuth client (package
  `com.awbuilds.auraspend.dev` + debug certificate). Prod is unaffected.
- Play Console was not touched by this rename: the staged draft release is the
  prod flavor (`com.awbuilds.auraspend`), versionCode 2. **Next uploaded build:
  versionCode 3.**
- The staged draft release notes/name still read
  `AuraSpend-V0.1.1.Alpha` — unrelated to flavor naming and still correct.
