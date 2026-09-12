# 0006 — Backlog Completion: Paging 3, Baseline Profiles, i18n, Verify CI

**Date:** 2026-09-12 · **Branch:** `phase-0-aurora-foundation`
**Status:** All four backlog items complete. Full gate green; baseline profile
generated on-device.

## 1. Paging 3 for Activity
- Room `PagingSource` with SQL filters (query/type/category), `ORDER BY
  dateTimestamp DESC`; repository exposes `pagedTransactions(filter)` via
  `Pager(PagingConfig(50, enablePlaceholders = false))`.
- New `TransactionListViewModel`: debounced query (250 ms), type/category
  filters through `flatMapLatest` + `cachedIn`; delete/restore under
  `boundary`; month strip from `observeSummary`.
- Screen keeps the Aurora look, sticky day headers (index peek), initial
  skeleton, append row, retry row, swipe-undo. The old 200-row bounded load
  and its "Load earlier" control are gone.

## 2. Baseline profile + Macrobenchmark
- New `:benchmark` module (`com.android.test`, flavor-matched with `:app`):
  - `StartupBenchmark` — cold start, `StartupTimingMetric`, 5 iterations.
  - `BaselineProfileGenerator` — warms Home/Activity tabs and the Quick Add
    sheet using contentDescription targets (locale-independent).
- `:app` gained a `benchmark` build type (`initWith(release)`, debug signing,
  `matchingFallbacks = release`) and a `<profileable android:shell="true"/>`
  manifest entry.
- **Profile generated on emulator-5554: 8,932 rules**, installed at
  `app/src/main/baseline-prof.txt`; AGP merges it for release builds.
- Commands documented in the README.

## 3. Localization, accessibility, ADRs
- `values-hi/strings.xml`: 344 Hindi entries with full key parity (brand/token
  strings intentionally identical).
- Headings semantics on all four tab titles; touch targets grow with font
  scale (`heightIn` instead of fixed heights); Quick Add sheet scrolls.
- ADRs 0001–0007 (`docs/adr/`): design system, app shell, SQL aggregates,
  error boundaries, screenshot testing, localization, no-paywall.
- README: Quick Add GIF, screenshot gallery, testing and localization docs.

## 4. Screenshot verify in CI
- Baselines committed at `app/src/test/screenshots/aurora_{light,dark,amoled}.png`.
- Unit tests regenerate them; the CI "Verify Aurora screenshots" step fails if
  `git status --porcelain` shows any change, so visual drift must be reviewed
  and committed deliberately. (The Roborazzi Gradle plugin was evaluated and
  dropped for a deterministic property-based path that works with the
  configuration cache.)

## Verification

| Check | Result |
|---|---|
| `testFreeDebugUnitTest` (150 tests incl. screenshots) | ✅ green |
| `assembleFreeDebug` / `assembleFreeRelease` | ✅ green |
| `:benchmark:assembleFreeBenchmark` | ✅ green |
| Baseline profile generation on emulator | ✅ 8,932 rules |
| CI workflow | unit tests → screenshot drift gate → builds |

## Notes / future
- Run `:benchmark:connectedFreeBenchmarkAndroidTest` on a physical device for
  representative startup numbers (emulator timings are indicative only).
- Font-scale: compact controls ellipsize gracefully; a full 200% sweep with
  Accessibility Scanner remains a nice-to-have.
- More locales are drop-in `values-<code>` files now that all strings are
  externalized.
