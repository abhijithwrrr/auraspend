# 0004 — Savings Goals, Vector Icons, Reactive Data & Device Verification

**Date:** 2026-09-12 · **Branch:** `phase-0-aurora-foundation`
**Status:** App verified end-to-end on emulator. Remaining work is listed below.

## What landed this session

### Savings goals (Plan hub completion)
- `ui/savings/SavingsGoalsScreen.kt`: total-saved hero with overall ring,
  per-goal cards (progress ring, "Reached" milestone badge, target date),
  add-funds dialog, create/edit sheet, delete confirmation.
- Plan hub: goals row with live summary; new `/goals` route.

### Fixes found by device testing
- **Reactive dashboard**: `DashboardViewModel` now `combine`s transactions,
  categories, budgets and subscriptions flows and re-aggregates on every
  write. Previously, saving from Quick Add while Home was visible left the
  balance stale until a tab switch.
- **Settings on every tab**: `SettingsAvatarButton` moved into the design
  system and added to Activity, Plan and Insights headers (was Home-only).
- **Category vector icons**: Material icon names from the seeded categories
  (`restaurant`, `directions_car`, …) now render as tinted vector icons in
  `CategoryAvatar`; emoji remains only for user-created categories. Kills the
  🏷️ fallback everywhere.

## Device verification (emulator-5554, 1080×2400 @ 420dpi)

| Flow | Result |
|---|---|
| Onboarding (parallax, gradient medallion, dots) | ✅ |
| Home: hero counters, sparkline, quick actions, FAB | ✅ |
| Quick Add: keypad → ₹250 expense → **Home updates live** | ✅ |
| Activity: sticky header, row → Transaction Detail (hero, edit/delete/duplicate) | ✅ |
| Plan hub + Budgets empty state | ✅ |
| Insights: period control, animated donut, top merchants | ✅ |
| Settings: theme preview tiles, Dark applied | ✅ |
| Category icons render as vectors | ✅ |

Screenshots: `…/T/opencode/auraspend-verify/20–39*.png` (temp, not committed).

## Remaining for world-class completion

1. **Classification flow**: visually Aurora now, but still three tabs; the
   planned wizard + swipe **triage inbox** for auto-detected SMS is the last
   major UX item.
2. **P5 performance**: Paging 3 for Activity, SQL aggregate DAO queries for
   Home/Insights, startup deferral is done; baseline profile + Macrobenchmark
   module + frame budgets in CI remain.
3. **P6 quality**: TalkBack/font-scale/contrast audit, strings.xml extraction
   (localization + plurals), Roborazzi screenshot tests in CI, README
   screenshots/GIFs, ADRs.
4. **Process**: `phase-0-aurora-foundation` now carries P0–P5; merge to `dev`
   when reviewed (all commits green, working tree clean).

**Resume:** read `AGENTS.md` → this file → `docs/handoffs/0000-master-plan.md`.
