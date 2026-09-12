# 0005 — Four-Iteration Hardening: Triage, Perf, i18n, Screenshots

**Date:** 2026-09-12 · **Branch:** `phase-0-aurora-foundation`
**Status:** All gates green; device-verified. Ready for review/merge.

## Iteration 1 — Smart Add triage inbox
- Auto Detect list is now a swipe inbox: right = save, left = dismiss, with
  colored action backgrounds, check/close icons and a hint line. Buttons kept
  as 48dp accessible fallbacks. (`ClassificationScreen.kt`)

## Iteration 2 — Performance: SQL aggregates + bounded loading
- New DAO aggregate Flows (all indexed): range summary (income/expense/counts),
  net balance, expense-by-category, daily expense via `localtime`.
- `DashboardViewModel` is driven entirely by aggregates — no full-table load.
  Budget spend is derived per budget period from the category totals.
- `getRecentTransactions(limit)` added; Activity loads 200 newest rows with a
  "Load earlier transactions" growth control; the month strip uses the SQL
  aggregate so it stays correct while bounded.
- Test fakes share `TestTransactionRepositoryDefaults`.

## Iteration 3 — Localization, accessibility, visual tests
- **i18n:** 322 strings + 8 plurals extracted across 17 screens;
  `stringResource`/`pluralStringResource` with typed args; English unchanged.
- **a11y:** AuraCard clickable role, segmented control uses `selectable` with
  Tab role, transaction rows merge semantics for TalkBack, chart descriptions.
- **Screenshot tests:** Robolectric + Roborazzi (NATIVE graphics) render the
  Aurora gallery to `app/build/screenshots/aurora_{light,dark,amoled}.png`; CI
  uploads them as artifacts. `TestApplication` keeps WorkManager/Room out of
  unit tests.

## Iteration 4 — Finalization
- **Shim retired:** `CashewComponents.kt` deleted; `TransactionEntryRow`,
  `softShadow`, `AuraSectionHeader`, `HideAmountIconButton` live in
  `ui/designsystem/`. Every screen now consumes the design system directly.
- **README preview** rebuilt with real screenshots (`docs/screenshots/`),
  design-language summary and corrected stack/API facts.
- Legacy roadmaps marked superseded.

## Verification

| Check | Result |
|---|---|
| `:app:compileFreeDebugKotlin` / `testFreeDebugUnitTest` / `assembleFreeDebug` | ✅ green after every commit |
| Robolectric screenshot suite (light/dark/AMOLED) | ✅ renders real fonts/resources |
| Emulator smoke test (emulator-5554): install → launch → Home/tabs | ✅ |
| Full-app regression checks | triage swipe, aggregates, i18n build all green |

## Remaining (future sessions)

1. **P5 remainder:** Paging 3, baseline profile + Macrobenchmark module,
   startup/jank budgets in CI.
2. **P6 remainder:** full TalkBack/font-scale audit, translated locales
   (strings are ready), Roborazzi verify-mode CI, README GIFs, ADRs.
3. **Classification wizard:** the triage inbox landed; the 3-step wizard is the
   remaining polish for Smart Add.
4. **Merge:** branch carries P0–P6 work; merge to `dev` once reviewed.

**Resume:** read `AGENTS.md` → this file → `docs/handoffs/0000-master-plan.md`.
