# Project: Aurora Rebuild

**Status:** Active. Phase 0 in progress.
**Branch:** `phase-0-aurora-foundation` (one branch per phase).
**Master plan:** `docs/handoffs/0000-master-plan.md`.

## Goal
Replace the entire presentation layer of AuraSpend with the Aurora design
language — world-class UI, motion and smoothness — without touching the data
layer, classification pipeline or business logic.

## Phases
| Phase | Scope | State |
|-------|-------|-------|
| P0 | Tokens, theme, fonts, design-system foundation, catalog, agent docs, paywall removal | ✅ Complete |
| P1 | Real NavHost, 4-tab + FAB IA, adaptive layout, shared elements, splash hand-off | ✅ Complete |
| P2 | Home, Activity, Transaction Detail/Edit, Quick Add | ✅ Complete + swipe triage inbox |
| P3 | Plan hub, Insights, Settings | ✅ Complete (+ savings goals) |
| P4 | Onboarding/splash, states | ✅ Onboarding/splash landed (state pass partial) |
| P5 | SQL aggregates, Paging 3, indices, baseline profiles, Macrobenchmarks | 🟡 Aggregates + indices + startup + bounded loading; Paging/baseline remaining |
| P6 | Accessibility, i18n, screenshot tests, README GIFs, OSS polish | 🟡 330 string resources, semantics, Roborazzi suite, README gallery; audit/translations remaining |

## Locked decisions
See `memory/decisions.md`.

## Constraints
- Business logic, Room schema, SMS parser and LLM pipeline stay functionally unchanged.
- Native build (`:llama`) requires NDK 29 + CMake; compile-only verification is acceptable
  for UI phases when the native build is cached or unavailable.
