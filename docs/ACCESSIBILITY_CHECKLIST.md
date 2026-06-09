# AuraSpend Accessibility Checklist

Developer checklist for maintaining **WCAG 2.1 AA** compliance during development. Use this when adding or modifying features.

---

## Perceivable

### Text Alternatives

- [ ] All icons in the UI have `contentDescription` set (use `null` only for decorative icons).
- [ ] Custom Canvas drawings (charts, graphs) expose data via `Modifier.semantics { }`.
- [ ] Text-based alternatives exist for all chart data (e.g., category names and values listed below the chart).

### Time-Based Media

- [ ] Not applicable (no audio/video content).

### Adaptable

- [ ] Information, structure, and relationships conveyed through presentation can be programmatically determined.
- [ ] Color is not the only means of conveying information (supplement with text labels, patterns, or icons).
- [ ] Content sequence makes sense when linearized for screen readers.

### Distinguishable

- [ ] Color contrast ratio is at least **4.5:1** for normal text (AA) and **3:1** for large text (18pt+ or 14pt+ bold).
- [ ] Non-text content (icons, charts) has sufficient contrast against adjacent colors.
- [ ] Text can be resized up to 200% without loss of content or functionality (use `sp` units, avoid fixed-height containers for text).
- [ ] Images of text are not used (all text is real text).

---

## Operable

### Keyboard Accessible

- [ ] All functionality is operable through a keyboard interface.
- [ ] No keyboard traps — focus can move away from any component using standard navigation.
- [ ] Custom swipe gestures (e.g., swipe-to-delete) have an alternative accessible action (e.g., long-press or button).

### Enough Time

- [ ] If time limits exist, the user can turn off, adjust, or extend them.
- [ ] Auto-updating content (e.g., dashboard) can be paused, stopped, or hidden.
- [ ] Respect `animationScale` system setting — do not force animations when the user has reduced motion enabled.

### Seizures and Physical Reactions

- [ ] No content flashes more than three times per second.
- [ ] All animations use standard Material 3 `tween` durations (300–500ms).

### Navigable

- [ ] Page titles describe the screen's purpose (`TopAppBar` title is set on every screen).
- [ ] Focus order is logical and preserves meaning.
- [ ] Purpose of each link can be determined from the link text alone or from the link text plus its programmatically determined context.
- [ ] More than one way is available to locate content (e.g., navigation tabs + search).
- [ ] Headings and labels describe topic or purpose (`SectionHeader` composable, `titleMedium` headings).
- [ ] Focus indicators are visible (use Material 3 defaults).

### Input Modalities

- [ ] Touch targets are at least **48x48dp** (Material 3 default).
- [ ] Gesture-dependent interactions (swipe, pinch) have single-point activation alternatives.

---

## Understandable

### Readable

- [ ] Language of the page is programmatically set (Android `locale` configuration).
- [ ] Unusual words, abbreviations, and jargon are defined on first use.

### Predictable

- [ ] Navigation patterns are consistent across screens.
- [ ] Components that have the same functionality across screens are labeled consistently.
- [ ] No unexpected context changes on input focus.

### Input Assistance

- [ ] Input errors are identified and described to the user in text (error colors are supplemented with messages).
- [ ] Labels or instructions are provided when input is required.
- [ ] For error correction, suggestions are provided when known (e.g., category autocomplete).
- [ ] For important data submissions, the action is reversible, checked, or confirmed.

---

## Robust

### Compatible

- [ ] All UI components use standard Material 3 composables that expose proper accessibility semantics.
- [ ] Custom composables include `Modifier.semantics { }` to expose their role, state, and value.
- [ ] State changes are announced via `Modifier.semantics { liveRegion = ... }` or equivalent.
- [ ] Test with TalkBack on a physical device before shipping.

---

## Feature-Specific Checklist

### When Adding a New Screen

- [ ] All interactive elements have `contentDescription` set.
- [ ] `TopAppBar` title is set and descriptive.
- [ ] Color contrast meets AA standards for all text.
- [ ] Scrolling content works with keyboard navigation.
- [ ] Back navigation is present and functional.
- [ ] Focus order is logical.

### When Adding a Canvas Chart

- [ ] Chart data is available as accessible text (e.g., a data table or list beneath the chart).
- [ ] Use `Modifier.semantics { }` to expose chart summary to TalkBack.
- [ ] Provide a `contentDescription` on the Canvas composable.
- [ ] Chart dimensions do not break at large font sizes (use relative sizing where possible).

### When Adding Animations

- [ ] Respect `Settings.Global.ANIMATOR_DURATION_SCALE` — disable or reduce animations when the user has set it to 0.
- [ ] Use `animateContentSize()` with `AnimationSpec` rather than hardcoded delays.
- [ ] No stroboscopic or flashing effects.
- [ ] Animation duration does not exceed 500ms for functional transitions.

### When Adding Color-Coded Elements

- [ ] Information is not conveyed by color alone — supplement with text, icons, or patterns.
- [ ] Color vision deficiency simulation has been checked (use Android Studio's Accessibility Scanner or a design tool).
- [ ] Focus and selection states are visually distinct.

---

## Testing

- [ ] Test all flows with **TalkBack** enabled.
- [ ] Test with **font size set to Largest** in system settings.
- [ ] Test in **Dark mode** and **AMOLED mode**.
- [ ] Test with **animations disabled** (Developer options → Animator duration scale → 0).
- [ ] Run **Android Accessibility Scanner** on all screens.
- [ ] Verify all `contentDescription` values are accurate and descriptive.
- [ ] Check that swipe-to-delete has a fallback accessible action.
- [ ] Verify Canvas chart data is exposed to accessibility services.

---

## Quick Reference

### Code Patterns

```kotlin
// Icon with contentDescription
Icon(
    Icons.Default.Search,
    contentDescription = "Search transactions"
)

// Decorative icon (not read by TalkBack)
Icon(
    Icons.Default.Circle,
    contentDescription = null
)

// Canvas with semantics
Canvas(
    modifier = Modifier
        .size(200.dp)
        .semantics {
            contentDescription = "Weekly spending chart: Food ₹1200, Transport ₹800"
        }
) {
    // drawing commands
}

// Respect reduced motion
val animScale = Settings.Global.getFloat(
    contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
)
val animationEnabled = animScale > 0f
if (animationEnabled) {
    // play animation
}
```

### Resources

- [WCAG 2.1 AA Quick Reference](https://www.w3.org/WAI/WCAG21/quickref/)
- [Android Accessibility Developer Guide](https://developer.android.com/guide/topics/ui/accessibility)
- [Material 3 Accessibility Guidelines](https://m3.material.io/foundations/accessible-design)
