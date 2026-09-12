# 0003 — Phases 2–5 Rollout: Core Loop, Insights, Settings, First-Run, Perf

**Date:** 2026-09-12 · **Branch:** `phase-0-aurora-foundation`
**Status:** P2 core loop ✅ · P3 (Insights + Settings) ✅ · P4 (onboarding/splash) ✅ · P5 partial
**Commits:** `40fa427` (P2), `fd6f392` (P3), onboarding/perf commit (P4 + P5 indices)

## What landed

### P2 — Core loop
- **Quick Add** (`ui/transaction/QuickAddSheet.kt`): amount-first keypad with
  press-scale + haptics, Expense/Income toggle, category chips, merchant input,
  Smart Add/Manual escape hatches. FAB opens it; Home quick actions preset type.
- **Transaction Detail/Edit** (`ui/transaction/TransactionDetailScreen.kt`):
  hero amount, category, details, edit mode, duplicate, delete confirmation.
  Every transaction row in the app opens it (Home + Activity).
- **Reusable editor** (`TransactionEditor.kt`) + `NewTransactionScreen.kt`
  replace the legacy Add screen (FlowRow categories, skeleton loading).
- **Home rebuilt**: aurora gradient balance hero with animated counters, quick
  actions, animated cash-flow area chart, budget carousel with rings,
  subscriptions card, recent activity with `animateItem`, skeleton loading,
  `AuraEmptyState`.
- **Activity rebuilt**: sticky date headers, flat lazy rows with keys +
  `contentType`, swipe-to-delete with undo, search/filter retained, empty states.

### P3 — Insights + Settings
- **Insights** (`AnalyticsScreen.kt`): memoized period scoping, animated donut
  with legend, net/savings hero, month-over-month delta badge, top merchants.
- **Settings**: live theme preview tiles (Light/Dark/AMOLED), grouped Aurora
  cards, animated AI-model progress, GitHub link, version/license footer.

### P4 — First-run
- **Onboarding**: parallax pager, gradient medallions, animated page dots,
  restore-backup card; existing Drive/permission logic preserved.
- **Splash**: spring-settled mark, aurora glow, tagline, sub-2s hand-off.

### P5 — Performance (partial)
- **Room v7** with migration: indices on `transactions(dateTimestamp)`,
  `(categoryId)`, `(type, dateTimestamp)` and `budgets(categoryId)`.
- **Dashboard aggregation** reduced from ~12 full scans to 3 single passes;
  weekly chart now DST-safe bucketing.
- Deleted dead code: `AuraSegmentedAddSheet`, empty `ui/insights`/`ui/savings`
  dirs.

## Verification

| Check | Result |
|---|---|
| `:app:compileFreeDebugKotlin` | ✅ green after each commit |
| `testFreeDebugUnitTest` | ✅ green (grouping tests intact) |
| `assembleFreeDebug` | ✅ green |
| On-device visual check | ⏸ device disconnected at P1; **not re-verified since** |

## Remaining work (next sessions)

1. **Smart Add / Classification rebuild** — 998-line legacy screen; wizard +
   triage inbox (P2 remainder). Highest remaining UX priority.
2. **Supporting screens pass** — Budget, Subscriptions, Categories still use the
   compat shim; migrate to design system, then delete `CashewComponents.kt`.
3. **P5 remainder** — Paging 3 for Activity, SQL aggregate DAO queries for
   Home/Insights, startup deferral (LLM/worker off main), baseline profile +
   Macrobenchmark module, frame budgets in CI.
4. **P6** — TalkBack/font-scale audit, strings.xml extraction, Roborazzi
   screenshot tests, README GIFs, ADRs.
5. **Device re-verification** of P1–P5 surfaces once hardware is available.

**Resume:** read `AGENTS.md` → this file → `docs/handoffs/0000-master-plan.md`.
