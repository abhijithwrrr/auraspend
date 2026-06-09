# AuraSpend Design System Guide

This document describes the design tokens, component library, theming system, and usage conventions for AuraSpend.

---

## Table of Contents

- [Design Tokens](#design-tokens)
- [Color System](#color-system)
- [Typography](#typography)
- [Shapes](#shapes)
- [Elevation](#elevation)
- [Spacing & Dimensions](#spacing--dimensions)
- [Theming Guide](#theming-guide)
- [Component Patterns](#component-patterns)

---

## Design Tokens

All design tokens are defined in `app/src/main/java/com/awbuilds/auraspend/ui/theme/`.

### Color Tokens

**File**: `Color.kt`

Three complete Material 3 color palettes are defined:

| Token | Light | Dark | AMOLED |
|-------|-------|------|--------|
| Primary | `#006B5E` | `#5DDBBF` | `#5DDBBF` |
| On Primary | `#FFFFFF` | `#003830` | `#003830` |
| Primary Container | `#7CF8E1` | `#005046` | `#005046` |
| On Primary Container | `#00201B` | `#7CF8E1` | `#7CF8E1` |
| Secondary | `#4A635C` | `#B1CCC3` | `#B1CCC3` |
| On Secondary | `#FFFFFF` | `#1C352E` | `#1C352E` |
| Secondary Container | `#CCE9DF` | `#324B44` | `#324B44` |
| On Secondary Container | `#06201A` | `#CCE9DF` | `#CCE9DF` |
| Tertiary | `#426277` | `#AACBFF` | `#AACBFF` |
| On Tertiary | `#FFFFFF` | `#0A3348` | `#0A3348` |
| Tertiary Container | `#C8E7FF` | `#284A5F` | `#284A5F` |
| On Tertiary Container | `#001E30` | `#C8E7FF` | `#C8E7FF` |
| Background | `#FBFDF9` | `#191C1B` | `#000000` |
| On Background | `#191C1B` | `#E1E3E0` | `#E1E3E0` |
| Surface | `#FBFDF9` | `#191C1B` | `#000000` |
| On Surface | `#191C1B` | `#E1E3E0` | `#E1E3E0` |
| Surface Variant | `#DBE5E0` | `#3F4945` | `#1C1E1D` |
| On Surface Variant | `#3F4945` | `#BFC9C4` | `#BFC9C4` |
| Outline | `#6F7975` | `#89938E` | `#89938E` |
| Outline Variant | `#BFC9C4` | `#3F4945` | `#2C3328` |
| Error | `#BA1A1A` | `#FFB4AB` | `#FFB4AB` |
| On Error | `#FFFFFF` | `#690005` | `#690005` |

**AMOLED differences**: Background and Surface use true black (`#000000`), Surface Variant uses very dark grey (`#1C1E1D`). All other accent and content colors match the Dark palette to maintain consistency.

### Category Colors

Categories use ARGB integer colors stored in the database (file: `Entities.kt`, `CategoryEntity.color`):

| Category | Color |
|----------|-------|
| Food & Dining | Red |
| Groceries | Orange |
| Transport | Blue |
| Shopping | Pink |
| Entertainment | Purple |
| Bills & Utilities | Teal |
| Health & Medical | Green |
| Education | Indigo |
| Rent & Housing | Brown |
| Income | Green |
| Other Expense | Grey |
| Other Income | Green |

---

## Typography

AuraSpend uses **Material 3 default typography** via `MaterialTheme.typography`. No custom type scale is defined in `Type.kt`.

### Usage Guidelines

| Style | Usage |
|-------|-------|
| `displayMedium` | Large numbers (balance card amount) |
| `headlineMedium` | Screen titles, onboarding page titles |
| `titleLarge` | Bottom sheet titles, premium upgrade heading |
| `titleMedium` | Section headers, card titles |
| `titleSmall` | Stat card values, budget progress numbers, subscription amounts |
| `bodyLarge` | Theme selector label, settings item text |
| `bodyMedium` | Transaction merchant/note, card descriptions, category names |
| `bodySmall` | Subtitles, metadata (dates, version info) |
| `labelLarge` | Button text, smart add/manual labels |
| `labelMedium` | Dashboard subtitle, category icon text |
| `labelSmall` | Chip labels, stat card labels, date labels, filter text |

**Font Weight**:
- `FontWeight.Bold` — Primary amounts, titles, emphasis
- `FontWeight.SemiBold` — Section headers, card headers
- `FontWeight.Medium` — Transaction merchant, list items
- Default (Normal) — Body content, descriptions

---

## Shapes

**File**: `Shape.kt` (Material 3 default shapes used throughout)

| Component | Shape | Value |
|-----------|-------|-------|
| Cards — Balance, Stats, Transaction | `RoundedCornerShape(12.dp)` | 12dp radius |
| Cards — Prominent (Charts, Overview) | `RoundedCornerShape(16.dp)` | 16dp radius |
| Balance Card | `RoundedCornerShape(24.dp)` | 24dp radius |
| Buttons | `RoundedCornerShape(16.dp)` | 16dp radius |
| Progress bar track | `RoundedCornerShape(3.dp)` | 3dp radius |
| Category/stat icon container | `CircleShape` | Fully round |
| Settings cards | `MaterialTheme.shapes.medium` | M3 default medium |

---

## Elevation

No custom elevation is defined in `Elevation.kt`. The app relies on Material 3 card defaults:

- Cards use default elevation (0dp surface color differentiation via `surfaceVariant` and `primaryContainer` rather than shadow).
- Buttons use standard Material 3 button elevation.
- Bottom sheets use default modal bottom sheet elevation.

---

## Spacing & Dimensions

**File**: `Dimens.kt` (no custom dimens file — values are hardcoded in composables)

### Common Spacing Values

| dp Value | Usage |
|----------|-------|
| 4.dp | Small gaps, icon spacing |
| 8.dp | Between elements, section spacing |
| 12.dp | Card internal padding, between icon and text, between row items |
| 16.dp | Screen horizontal padding, card padding, between sections |
| 20.dp | Alternative card internal spacing |
| 24.dp | Large section padding, balance card padding, onboarding horizontal padding |
| 32.dp | Bottom sheet padding, empty state padding, bottom spacing |
| 48.dp | Onboarding page spacing after icon |

### Common Size Values

| dp Value | Component |
|----------|-----------|
| 6.dp | Progress bar height |
| 8.dp | Page indicator dot size (inactive) |
| 10.dp | Category color dot |
| 12.dp | Legend color indicator |
| 16.dp | Circular progress indicator size |
| 18.dp | Button icon size |
| 20.dp | Stat card icon size |
| 24.dp | Settings item icon size |
| 32.dp | Icon in add sheet |
| 36.dp | Category icon container |
| 40.dp | Transaction item icon (colored circle) |
| 48.dp | Button height |
| 56.dp | Premium upgrade button height |
| 64.dp | Empty state icon size |
| 72.dp | Spacer width for page indicator balance |
| 120.dp | Weekly chart height |
| 140.dp | Modal bottom sheet card size (width) |
| 200.dp | Pie chart size |

---

## Theming Guide

### Theme Architecture

**File**: `Theme.kt`

The theme is controlled by `AuraSpendTheme` composable:

```kotlin
@Composable
fun AuraSpendTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
)
```

**Parameters**:
- `themeMode`: One of `LIGHT`, `DARK`, `AMOLED`
- `dynamicColor`: When `true` (default) and running on Android 12+, uses system Monet palette instead of the static palette (except AMOLED mode)

**Color scheme resolution order**:
1. If `themeMode == AMOLED` → always use static AMOLED palette
2. If `dynamicColor && Android 12+` → use `dynamicDarkColorScheme` or `dynamicLightColorScheme` based on theme mode
3. If `themeMode == DARK` or system is in dark theme → use static Dark palette
4. Otherwise → use static Light palette

**Status bar**: Light status bar icons when in Light mode; light icons in Dark/AMOLED.

### Theme Persistence

The selected `AppThemeMode` is persisted in `SharedPreferences`. On app restart, the saved mode is restored.

---

## Component Patterns

### Card Patterns

Cards are the primary container pattern. Three background styles are used:

| Style | Color Source | Use Case |
|-------|-------------|----------|
| Surface Variant | `MaterialTheme.colorScheme.surfaceVariant` | Transaction items, stat cards, chart cards, settings items, budget cards |
| Primary Container | `MaterialTheme.colorScheme.primaryContainer` | Balance card, income summary card |
| Secondary Container | `MaterialTheme.colorScheme.secondaryContainer` | Subscription summary card |
| Error Container | `MaterialTheme.colorScheme.errorContainer` | Expense summary card |

### Surface Variant vs. Card Elevation

The app avoids elevation-based backgrounds. Cards use `surfaceVariant` for visual distinction instead of shadow elevation. This means cards are flat by design.

### MVI Pattern

Each feature screen with state management follows this structure:

```
FeatureMvi.kt       → ViewState (data class) + ViewIntent (sealed interface)
FeatureViewModel.kt → Processes intents, manages StateFlow<ViewState>
FeatureScreen.kt    → Composable observing state, dispatching intents
```

**ViewState**: Immutable data class with all UI state fields.
**ViewIntent**: Sealed interface with data objects/classes for each user action.
**ViewModel**: Extends `androidx.lifecycle.ViewModel`, uses `StateFlow` for state, exposes `fun onIntent(intent: ViewIntent)` as the single entry point.
**Screen**: Uses `collectAsStateWithLifecycle()` to observe state.

### Canvas Chart Pattern

Charts are drawn via Compose `Canvas` with `drawIntoCanvas`:

```kotlin
Canvas(modifier = modifier) {
    // Use drawRect, drawArc, drawRoundRect, drawText for chart elements
}
```

See [CHARTING.md](CHARTING.md) for specific chart implementations.

### Shimmer Loading

Loading states use `ShimmerCard` from `ui/core/ShimmerEffect.kt`, typically showing 4 placeholder cards while data loads.
