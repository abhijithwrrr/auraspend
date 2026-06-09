# AuraSpend Accessibility Guide

This document describes the current accessibility features and known limitations of AuraSpend.

---

## Current Accessibility Features

### Content Descriptions

- All icons in bottom navigation, top app bars, and buttons have `contentDescription` parameters set.
- The FAB and action buttons use descriptive labels (e.g., "Analytics", "Search", "Delete").
- Category cards and transaction items include meaningful text descriptions.

### Color and Contrast

- Material 3 color schemes provide sufficient contrast ratios between text and background in all three modes (Light, Dark, AMOLED).
- Error states use distinct colors (red) and are supplemented with icons.
- Progress bars use color coding (green/orange/red) plus numerical text values ("₹1,200 / ₹5,000"), ensuring information isn't conveyed by color alone.

### Touch Targets

- All interactive elements use Material 3 minimum touch target sizes (48dp).
- Card-based layouts provide generous tap areas for list items.
- Segmented button rows for theme selection maintain accessible touch spacing.

### Text Scaling

- Typography uses Material 3 type scale, respecting system font size settings.
- All text elements use `sp` units for dynamic scaling.
- Layouts use `fillMaxWidth` and adaptive sizing to accommodate text scaling.

### Navigation

- Standard back navigation via top app bar back button on all screens.
- Bottom navigation bar with four clearly labeled tabs.
- Consistent layout patterns across screens (predictable navigation).

### Animations

- Animations use standard `tween` durations (300–500ms) rather than extreme values.
- Content fades in rather than abruptly appearing.
- No parallax, complex 3D transforms, or motion that could trigger vestibular disorders.

---

## Known Limitations

### Screen Reader Compatibility

- Canvas-drawn charts (weekly bar chart, pie chart) lack `contentDescription` or accessibility delegation — chart data is not exposed to TalkBack.
- The weekly chart only renders visual bars without semantic data labels for screen readers.
- The pie chart renders category names as text labels in the legend, which are accessible, but the visual arc segments are not.

### Keyboard Navigation

- The app is designed primarily for touch input. Full keyboard/trackpad navigation has not been explicitly tested.
- Focus indicators use Material 3 defaults but have not been customized.

### Color Blindness

- While text labels supplement color-coded elements in most places, the weekly bar chart relies on color intensity for empty vs. filled bars.
- Category colors in pie chart legends are labeled with text and percentages, mitigating reliance on color alone.

### Dynamic Type

- Canvas charts do not adapt to large text sizes — chart dimensions are fixed (e.g., `200.dp` for the pie chart, `120.dp` height for bar chart).
- Some card layouts may truncate text at very large font sizes.

### Focus Order

- Custom composables (bottom sheet, onboarding HorizontalPager) may not follow an optimal focus order for screen readers.

### Reduced Motion

- There is no system-level "Reduce motion" setting override — all animations play regardless of user preferences.

### Captioning

- The app has no audio or video content, so captioning is not applicable.

---

## Recommended Improvements

| Priority | Issue | Impact | Effort |
|----------|-------|--------|--------|
| High | Add TalkBack delegation for Canvas charts via `Modifier.semantics` | Screen reader users cannot access chart data | Medium |
| High | Respect `animationScale` system setting for reduced motion | Affects users with vestibular disorders | Low |
| Medium | Add `contentDescription` for bar chart and pie chart Canvas elements | Screen reader users miss spending trends | Medium |
| Medium | Ensure all `contentDescription` values are localized | Non-English users need translated labels | Low |
| Medium | Test and fix focus order in custom composables | Keyboard/TalkBack navigation flow | Medium |
| Low | Provide alternative text representation for chart data below Canvas | All users benefit from data in text form | Low |
| Low | Use `Modifier.semantics` to expose SwipeToDismissBox actions | Swipe-to-delete not discoverable via TalkBack | Low |

---

See [ACCESSIBILITY_CHECKLIST.md](ACCESSIBILITY_CHECKLIST.md) for the developer checklist to maintain WCAG 2.1 AA compliance.
