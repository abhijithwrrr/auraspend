# AGENTS.md — AuraSpend agent contract

> Read this file and the latest file in `docs/handoffs/` before touching anything.
> Update both (plus `CLAUDE.md` / `memory/` when you learn something durable) before ending a session.

## What this project is

AuraSpend is an offline-first personal finance app for Android (Kotlin + Jetpack
Compose). It classifies bank SMS on-device (regex + optional on-device LLM),
tracks expenses/budgets/subscriptions/goals, and backs up to Google Drive.
Apache-2.0, open source.

## Current phase

**Phase 0 — Aurora Foundation** (branch `phase-0-aurora-foundation`).
Phases P0–P6 are defined in `docs/handoffs/0000-master-plan.md`.
Do not start phase N+1 until phase N's gate passes and its handoff is written.

## Module map

| Module | What lives there |
|---|---|
| `:app` | Everything (UI, domain, data) |
| `:llama` (`llama-lib/`) | Vendored llama.cpp Android runtime |
| `third_party/llama.cpp` | Upstream submodule (never edit) |

Kotlin source root: `app/src/main/java/com/awbuilds/auraspend/`

| Package | Contents |
|---|---|
| `core/` | Cross-cutting helpers (`AuraLog`, boundaries) |
| `data/` | Room, classification pipeline, AI, Drive, privacy |
| `domain/` | Models, repository interface, use cases |
| `ui/designsystem/` | Aurora tokens + reusable components (**use these**) |
| `ui/theme/` | Color palette, typography, shapes, `AuraSpendTheme` |
| `ui/<feature>/` | Feature screens (home, transaction, classification, …) |

## Commands

```bash
./gradlew :app:compileFreeDebugKotlin      # fast compile check
./gradlew testFreeDebugUnitTest            # unit tests (required before handoff)
./gradlew assembleFreeDebug                # APK (native build; slow)
./gradlew assemblePlayDebug                # Play flavor
```

Flavors: `free` (all features, F-Droid/self-build) and `play` (Play Store).
The Play paywall was removed in Phase 0 — do not reintroduce premium gating.

## Hard requirements (non-negotiable)

1. **Errors never crash the UI.** Every IO/platform boundary (Room, network,
   Drive, LLM, CSV, SMS, WorkManager) must catch its exceptions, log via
   `AuraLog`, and degrade to a typed fallback/error state. Never write an empty
   `catch {}`. Always rethrow `CancellationException`. Use the `boundary { }` /
   `boundaryOrNull { }` helpers from `core/AuraLog.kt` for new code.
2. **No `!!` in new code.** No `lateinit` where a constructor/`requireNotNull`
   with a real error path works.
3. **Handoff discipline.** Every session ends with a `docs/handoffs/NNNN-*.md`
   entry: what changed, decisions made, verification evidence (commands +
   results), open risks, next step. Every session starts by reading the latest
   handoff.
4. **Memory discipline.** Keep `CLAUDE.md` as a ≤100-line hot cache. Durable
   knowledge (decisions, glossary, project state) goes to `memory/`.
5. **Design system discipline.** Screens use `ui/designsystem` components and
   `ui/theme` tokens. No raw hex colors, no one-off `fontSize =`, no
   `Modifier.shadow` on content surfaces, no new emoji in chrome. New shared
   UI belongs in `ui/designsystem`, not in a screen file.
6. **Green build.** `:app:compileFreeDebugKotlin` and `testFreeDebugUnitTest`
   must pass before a handoff. Never commit red.
7. **One phase per branch** (`phase-N-name`), small commits, conventional
   commit subjects (`feat:`, `fix:`, `chore:`, `docs:`, `perf:`, `refactor:`).

## Design system quick reference

| Need | Use |
|---|---|
| Card | `AuraCard` (`Filled` / `Tonal` / `Outlined` / `Glass`) |
| Money text (animated) | `AnimatedMoney` + `AuraType.money*` |
| Category badge | `CategoryAvatar` |
| Filter/range selector | `AuraSegmentedControl` |
| Progress / donut / area | `AuraProgressRing`, `AuraDonutChart`, `AuraAreaChart` |
| Loading placeholder | `AuraSkeleton` |
| Empty state | `AuraEmptyState` |
| Spacing | `AuraSpacing.*` (4dp grid, `gutter = 20dp`) |
| Motion | `AuraMotion.*` (never invent durations) |
| Brand gradient | `AuraGradients.aurora` |

Legacy `ui/core/CashewComponents.kt` is a Phase 0 compatibility shim. Do not
add new usage; migrate call sites to the design system as each screen is
rebuilt (P1–P3), then delete the shim.
