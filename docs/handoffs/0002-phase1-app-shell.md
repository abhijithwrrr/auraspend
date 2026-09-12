# 0002 — Phase 1: App Shell

**Date:** 2026-09-12 · **Branch:** `phase-0-aurora-foundation` (P0 branch carries P1)
**Status:** Complete (visual on-device check pending — device disconnected mid-phase)
**Plan:** `docs/handoffs/0000-master-plan.md`

## What landed

### Navigation (`ui/navigation/NavGraph.kt`, rewritten)
- Single `NavHost` with a typed `Routes` object: `home`, `activity`, `plan`,
  `insights` top-level; `settings`, `add_transaction`, `classification`,
  `budgets`, `subscriptions`, `categories` pushed.
- Tab switching uses `popUpTo(HOME) { saveState = true }` +
  `launchSingleTop` + `restoreState`, so each tab keeps its own scroll/state and
  back always returns to Home instead of exiting.
- Subtle transitions: cross-fade between tabs; horizontal push/fade for pushed
  destinations; system predictive back enabled via the manifest.
- Chrome only renders on top-level destinations; flows get the full screen.

### Chrome (`ui/core/AuraSpendScaffold.kt` → `AuraAppChrome`)
- Compact: bottom bar, 4 tabs + gradient FAB. Expanded (≥600dp): navigation
  rail with FAB header and content beside it.
- Settings is no longer a tab; it is reached from the Home header avatar.

### New Plan hub (`ui/plan/PlanHubScreen.kt`)
- Budget-health hero with animated ring, recurring monthly total, and rows into
  Budgets / Subscriptions / Categories.

### Supporting changes
- `AddTransactionSheet` extracted to `ui/core/AddTransactionSheet.kt`
  (surfaceContainerLow container — fixes the AMOLED invisibility found in P0).
- `SettingsScreen` gained a real back arrow (it is a pushed screen now).
- `DashboardScreen` gained the header avatar (`onOpenSettings`).

## Verification

| Check | Result |
|---|---|
| `./gradlew :app:compileFreeDebugKotlin` | ✅ green |
| `./gradlew testFreeDebugUnitTest` | ✅ green |
| `./gradlew assembleFreeDebug` | ✅ green |
| On-device walkthrough | ⏸ device disconnected during the phase; to re-verify at P2 start |

## Notes / debt

- Pushed screens that used to be tabs (`Settings`, `Analytics`) will be fully
  redesigned in P3; only the frame changed here.
- Smart Add / Manual still open the legacy full screens until P2's Quick Add.
- `onBack` for Activity now navigates to Home (no visible back affordance).

## Next — P2: Core loop

Home rebuild (aurora hero, animated counters, sparkline, budget carousel),
Activity rebuild (sticky headers, flat lazy rows, multi-select), **new
Transaction Detail/Edit screen**, Quick Add sheet, then Smart Add wizard and
triage inbox.
