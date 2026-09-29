# 0008 — Aurora Command Center Refresh

**Date:** 2026-09-28 · **Branch:** `phase-0-aurora-foundation`
**Status:** Complete. Refreshed the core visual hierarchy and app chrome while
preserving the existing finance flows and design-system architecture.

## 1. What changed

- **Home** now reads as a daily money command centre: greeting and identity →
  balance position → income/expense split → actions → movement → plans →
  recent activity. The balance hero has a softer aurora treatment, a stronger
  32dp silhouette, and a clearer divider between cash-in and cash-out.
- **Bottom navigation** is now a floating inset capsule with a hairline border,
  giving the app a distinct spatial signature while keeping the centre add
  action and existing destinations intact.
- **Typography** on Home, Activity, Plan, and Insights now consistently uses
  the existing Material/Aura typography tokens instead of screen-local sizes.
- Removed unused Home imports and kept all existing click/navigation behavior.

## 2. Decisions

- Reused the existing Aurora gradient and shared design-system primitives rather
  than introducing a second visual language.
- Kept the redesign reversible and data-neutral: no repository, database,
  classification, navigation route, or user-data behavior changed.
- The existing `gradle.properties` working-tree change was preserved because it
  predates this session's UI work.

## 3. Verification

| Check | Result |
|---|---|
| `./gradlew :app:compileFreeDebugKotlin` | ✅ build successful after Kotlin daemon cache fallback |
| `./gradlew testFreeDebugUnitTest` | ✅ green |
| `git diff --check` | ✅ clean |

The build emitted existing deprecation warnings for Google Sign-In and Compose
swipe APIs; no new compilation errors remain.

## 4. Open risks / next step

- The visual refresh has been validated at source/build level. A physical-device
  screenshot pass would still be useful for checking the new bottom capsule at
  small widths and with large font scales.
