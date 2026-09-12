# Memory — AuraSpend

Hot cache for agents. Keep this under ~100 lines. Deep detail lives in `memory/`.
**Start every session by reading `AGENTS.md` + the latest `docs/handoffs/` file.**

## Project
AuraSpend — offline-first Android expense manager (Kotlin, Jetpack Compose,
Room, on-device Qwen2.5 via llama.cpp). Apache-2.0.
Repo: `github.com/abhijithwrrr/auraspend`. Branch strategy: one phase per branch.

## Glossary
| Term | Meaning |
|------|---------|
| **Aurora** | The new design language + theme (purple/lavender/teal, Plus Jakarta Sans) |
| **Phase N (P0–P6)** | Rebuild phases; see `docs/handoffs/0000-master-plan.md` |
| **Handoff** | End-of-session doc in `docs/handoffs/NNNN-*.md` |
| **Gate** | Objective exit criteria a phase must pass before the next starts |
| **Activity** | The transaction list tab (new IA name) |
| **Plan hub** | Tab grouping Budgets + Subscriptions + Goals (P3) |
| **Insights** | Analytics + insight feed tab (P3) |
| **Triage inbox** | Swipe accept/reject queue for auto-detected SMS (P2) |
| **Shim** | `ui/core/CashewComponents.kt` — Phase 0 compat layer, deleted after P3 |
| **Boundary** | try/catch wrapper (`boundary {}` in `core/AuraLog.kt`) required at every IO edge |
| **AuraCard / tokens** | Core design-system primitives (`ui/designsystem/`) |
| **Flavor** | `free` (everything unlocked) / `play` (Play Store) build variants |

## Active work
| Item | State |
|------|-------|
| **P0–P5 rollout** | ✅ Landed — `docs/handoffs/0003-phases-2-5-rollout.md` |
| **Remaining** | Classification rebuild · supporting screens · Paging/aggregates/baselines · a11y + screenshot tests |
| **Paywall removal** | Done in P0 (PremiumGate, PremiumUpgradeScreen, BillingManager deleted) |
| **Master plan** | `docs/handoffs/0000-master-plan.md` |

## Locked decisions
- Aurora purple brand (matches app icon + landing page), not Cashew blue.
- 4 tabs + center FAB; Settings moves behind header avatar in P1.
- `material3 1.4.0` stable via Compose BOM `2026.09.00` (Compose UI 1.12.1).
- No premium/paywall anywhere; `free` and `play` flavors remain.
- Try/catch at every boundary is a hard rule (see `AGENTS.md`).

## Preferences / rules
- Verify with `./gradlew :app:compileFreeDebugKotlin` + `testFreeDebugUnitTest`.
- Visual changes are not "done" until screenshots are captured in all 3 themes.
- No raw colors/font sizes/shadows in screens — design system or nothing.
- Keep commits small and conventional; never commit a red build.

→ Full glossary: `memory/glossary.md` · Phases: `memory/projects/aurora-rebuild.md`
→ Stack + commands: `memory/context/tech-stack.md` · Decisions: `memory/decisions.md`
