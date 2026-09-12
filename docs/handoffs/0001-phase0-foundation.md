# 0001 — Phase 0: Aurora Foundation

**Date:** 2026-09-12 · **Branch:** `phase-0-aurora-foundation` · **Status:** Complete
**Commits:** `fa9938e` (code), `f900e1a` (docs/memory), plus this handoff.
**Plan:** `docs/handoffs/0000-master-plan.md` · **Design spec:** `docs/design/aurora.md`

## What landed

### Build & tooling
- `gradle/libs.versions.toml` — single source of truth for versions. Compose BOM
  `2026.09.00` (Compose UI 1.12.1, **Material 3 1.4.0**), Navigation 2.10.1,
  Activity Compose 1.13.0, Lifecycle 2.11.0, Room 2.8.5, core-ktx 1.19.0.
- `build.gradle.kts` / `app/build.gradle.kts` — catalog-based plugins/deps;
  `debugImplementation(ui-tooling)`; unused `appcompat` removed.
- Lottie 6.7.1 and Paging 3.5.1 are staged in the catalog but intentionally not
  on the classpath yet (P4/P5).

### Aurora theme
- `ui/theme/Color.kt` — full brand palette (purple `#5E3A8B`, lavender, teal,
  violet-cast neutrals) with light / dark / AMOLED schemes.
- `ui/theme/Theme.kt` — Plus Jakarta Sans (variable, OFL, 176 KB) with tabular
  figures for numerics, retuned typography, and **dynamic color defaulting to
  off** (brand-first; still user-toggleable).
- `ui/theme/Shape.kt` — 12/16/20/28/36 dp scale.
- XML theme now platform-based (no AppCompat), with a `values-night` variant.

### Design system (`ui/designsystem/`)
`AuraTokens` (spacing 4dp grid with 20dp gutter, motion specs, gradients,
numeric type styles) · `AuraCard` (Filled/Tonal/Outlined/Glass, hairline
borders instead of shadows) · `AnimatedMoney` · `CategoryAvatar` ·
`AuraSegmentedControl` · `AuraProgressRing`, `AuraDonutChart`, `AuraAreaChart`
(sweep/grow-in animations) · `AuraSkeleton` · `AuraEmptyState`.

### Compatibility layer (temporary)
`ui/core/CashewComponents.kt` was rebuilt on the design system with the same
public API, so **every existing screen instantly renders in Aurora** without
file-by-file edits. `ui/core/AuraSpendScaffold.kt` now has the gradient FAB
(press-scale + haptic), tonal selection pill and hairline-topped nav bar.
The shim is deleted when the last screen migrates (end of P3).

### Paywall removal
Deleted `premium/PremiumGate.kt`, `ui/settings/PremiumUpgradeScreen.kt`, both
`BillingManager.kt` stubs, and the `com.android.vending.BILLING` permission.
README feature table/flavors updated.

### Agent infrastructure
`AGENTS.md` (hard rules incl. the try/catch boundary policy), `CLAUDE.md` hot
cache, `memory/` (glossary, project, decisions D1–D6, tech stack),
`core/AuraLog.kt` with `boundary {}` / `boundaryOrNull {}` helpers.

## Verification evidence

| Check | Result |
|---|---|
| `./gradlew :app:compileFreeDebugKotlin` | ✅ green |
| `./gradlew testFreeDebugUnitTest` | ✅ green |
| `./gradlew assembleFreeDebug` | ✅ green (52s with cached native libs) |
| `adb install -r` + manual walkthrough (Pixel-class device, 1080×2376) | ✅ onboarding, Home, Activity, Add sheet, Settings |
| Light / Dark / AMOLED screenshots | ✅ `…/T/opencode/auraspend-verify/01–11*.png` (temp, not committed) |
| Fix found during verification | Add sheet used `background` → invisible on AMOLED; now `surfaceContainerLow` (`NavGraph.kt`) |

Observed on device: Aurora purple + Plus Jakarta Sans render correctly, tonal
cards and hairline borders read well in all three themes, the gradient FAB and
segmented control animate, and the numeric padding fix keeps amounts centered.

## Known issues / debt (owned by later phases)

1. Default category icons are Material names (`"restaurant"`, `"directions_car"`),
   so avatars/chips fall back to 🏷️ — P2 maps names to vectors.
2. Home's empty state and spending graph are still the legacy screen code; P2
   rebuilds Home.
3. Deprecation warnings to resolve during screen rebuilds: `TabRow` (use
   `PrimaryTabRow`), `Icons.Filled.ListAlt`/`KeyboardArrowRight`
   (AutoMirrored), `rememberSwipeToDismissBoxState(confirmValueChange)`.
4. Dynamic color preference previously defaulted to true; on devices where it
   had been stored, the user may still have it on. The Settings toggle (P3
   redesign) will surface this properly.
5. Screenshots live in temp only; the committed screenshot suite arrives with
   Roborazzi in P6.

## Next phase — P1: App shell

Real `NavHost` (type-safe routes, per-tab back stacks, state restore), 4-tab +
FAB IA (Home / Activity / Plan / Insights; Settings behind header avatar),
adaptive rail/two-pane, animated tab transitions, shared-element infrastructure,
splash → Home hand-off. Gate: back never exits from a tab, state survives
process death, tab-switch frame P95 ≤ 8 ms.

**Resume:** read `AGENTS.md`, then this file, then `docs/handoffs/0000-master-plan.md`.
