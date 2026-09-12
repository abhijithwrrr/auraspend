# ADR 0002 — Four tabs + center FAB with adaptive chrome

**Status:** Accepted · 2026-09

## Context

The old shell switched content with a `currentTab` string inside one route:
the system back button exited the app from any tab, tab state was lost on
process death, and budgets/subscriptions/goals were buried behind Settings.
Settings itself was a tab while core finance features were pushed screens.

## Decision

- One `NavHost` owns navigation. Top-level destinations are `Home`, `Activity`,
  `Plan`, `Insights`; everything else is pushed with explicit transitions.
- Tab switching uses `popUpTo(HOME) { saveState }` + `restoreState`, so each
  tab keeps its own scroll/state and back always returns to Home.
- Chrome (bottom bar with a center gradient FAB, or a navigation rail on
  ≥600dp) renders only on top-level destinations.
- Settings lives behind a header avatar present on every tab.
- The FAB opens the amount-first Quick Add sheet; Smart Add/Manual are one tap
  deeper.

## Consequences

- Predictable back behaviour and state restoration on all devices.
- Tablets and foldables get a native-feeling rail instead of a stretched bar.
- Adding a top-level destination means updating `TopLevelDestination` plus the
  chrome, which is intentionally a visible product decision.
