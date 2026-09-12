# 0000 — Aurora Master Plan

**Status:** Approved 2026-09-12 · supersedes the UI sections of `ROADMAP.md` and `IMPROVEMENT_ROADMAP.md`.
**Owner:** AuraSpend · **Execution:** agent sessions with handoffs (see `AGENTS.md`).

## Goal

Replace AuraSpend's entire presentation layer with the **Aurora** design language — world-class UI, motion and smoothness — while keeping the data layer, SMS/AI pipeline and business logic functionally unchanged. Target: the best open-source expense manager on Android.

## Locked decisions

| # | Decision |
|---|----------|
| 1 | Aurora purple brand palette (matches app icon + landing page), dark-first |
| 2 | 4 tabs + center FAB: Home / Activity / Plan / Insights; Settings behind header avatar |
| 3 | `material3 1.4.0` stable via Compose BOM `2026.09.00` (M3 Expressive APIs available) |
| 4 | No premium/paywall anywhere (`free`/`play` flavors remain for distribution) |
| 5 | Phased rebuild, one branch per phase, gates + handoffs |
| 6 | Try/catch at every IO boundary is a hard rule (`core/AuraLog.kt`, AGENTS.md) |
| 7 | Agent memory: `CLAUDE.md` hot cache + `memory/` deep store |

Full visual spec: `docs/design/aurora.md`.

## Phases

| Phase | Deliverables | Gate |
|-------|--------------|------|
| **P0 Foundation** | Version catalog; Aurora theme + Plus Jakarta Sans; `ui/designsystem` foundation; strings/theme XML; paywall removal; agent docs + memory + handoffs | Compile + unit tests green; screenshots in 3 themes |
| **P1 App shell** | Real NavHost (type-safe routes, per-tab back stacks, state restore); 4-tab + FAB IA; adaptive rail/2-pane; animated tab transitions; shared-element infra; splash hand-off | Back never exits from a tab; state survives process death; tab-switch frame P95 ≤ 8ms |
| **P2 Core loop** | Home rebuild; Activity rebuild (sticky headers, flat lazy rows, multi-select, sort/filter sheet, swipe+undo); Transaction Detail/Edit; Quick Add sheet; Smart Add wizard; triage inbox | Full create→view→edit→delete loop animated; smooth fling over 10k seeded rows |
| **P3 Plan + Insights** | Plan hub (budgets/subs/goals); Insights (animated charts, MoM deltas); category manager | Zero legacy components in the app |
| **P4 First-run + polish** | Onboarding/splash with Lottie + parallax; permission education; theme picker; about/licenses; all empty/loading/error/offline states | First-run flow reviewed end-to-end |
| **P5 Perf hardening** | SQL aggregate DAO queries + Flow; indices + migration; Paging 3; stability config; `drawWithCache`; startup deferral; baseline profile + Macrobenchmarks | Cold start TTID < 700ms; frame P95 < 12ms under fling/chart/sheet; zero frames > 32ms |
| **P6 QA + OSS** | TalkBack/font-scale/contrast/haptics/reduced motion; i18n; Roborazzi screenshot tests in CI; README GIFs | Accessibility Scanner clean; screenshot suite green in CI |

## Anti-goals

- No heavy chart library; no gradients/shadows on content; no emoji in chrome;
  no screen-specific one-off styles; no animation that delays input;
  no rewriting business logic or DB schema beyond the P5 index migration.

## Verification protocol

Per phase: `./gradlew :app:compileFreeDebugKotlin` + `testFreeDebugUnitTest`
(+ Macrobenchmark from P5). Handoff doc written with evidence. Screenshots
(light/dark/AMOLED) required for any visual change from P1 on.
