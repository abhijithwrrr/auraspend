# 0010 — UI Revamp: Locale, Type, Theme, States, Enforcement

**Date:** 2026-09-28 · **Branch:** `phase-0-aurora-foundation`
**Status:** Complete. Systemic UI quality pass across currency, typography,
theming, accessibility, i18n and visual-regression coverage. 28 new tests.

Follows `0009-crash-paths-and-safety-nets.md`. Nothing here changes data,
classification, navigation routes or user-data behaviour — this is presentation
and the guardrails around it.

## 1. What changed

### Money is now actually locale-correct

`formatMoney` hardcoded `₹` and grouped with `String.format(Locale.US, "%,d")`.
Its KDoc claimed "Indian grouping" — **the code did the opposite**. Any user
outside the US saw `₹123,456.78` instead of `₹1,23,456.78`.

- New `ui/designsystem/Money.kt`: `CurrencyStyle`, `formatMoney`,
  `formatMoneyCompact`, `rememberCurrencyStyle`.
- Grouping, decimal separators, symbol placement and minor-unit digits all come
  from the locale + currency. JPY renders without decimals; a German locale puts
  the symbol after the number.
- **Indian lakh/crore grouping is implemented explicitly** (`indianGroup`).
  `NumberFormat` and a `#,##,##0` pattern both returned *Western* grouping for
  `en_IN` and `hi_IN` on this project's JDK, so relying on platform CLDR would
  have left the original bug in place. Money rendering is now deterministic.
- Currency is provided by `AuraSpendTheme` (CompositionLocal + `MoneyConfig` for
  non-composable call sites), defaults to the device locale's currency, and is
  overridable via the `currency_code` preference.

### Typography is on the scale

**68 hardcoded `fontSize = N.sp` sites** were bypassing the complete Material 3
type scale that already existed in `ui/theme/Theme.kt`. All are now
`MaterialTheme.typography.*` / `AuraType.*`. Weights are preserved where they
differ from the token. Migration script kept at `tools/convert_typography.py`.

### Theme

- **Added `AppThemeMode.SYSTEM`**, and made it the default. Previously a user
  with system dark mode got a *light* app on first launch, with no way to opt
  into following the OS.
- `AuraSpendTheme` resolves `SYSTEM` reactively via `isSystemInDarkTheme()`.
- **AMOLED gets its own semantic colours.** `AmoledExtendedColors` existed as a
  copy of `DarkExtendedColors`; income/expense/warning were contrast-matched to a
  `#121212` surface and read washed out on true black. New tokens in `Color.kt`.
- Fixed the unguarded `AppThemeMode.valueOf(...)` on a `SharedPreferences` string
  in `MainActivity` — a corrupt value crashed at startup. Now
  `AppThemeMode.fromName()`, defaulting to `SYSTEM`.

### Design system completeness

- New `ui/designsystem/State.kt`:
  - **`AuraErrorState`** — the system had an empty state but no error state, so a
    failed load was indistinguishable from "no data yet". Worst possible for a
    user checking a bank balance. Icon medallion + title + message + retry.
  - **`AuraStatTile`** — a labelled metric on a tonal container, with
    `heightIn` (not `height`) so it grows at 200% font scale, and label/value
    merged into one accessibility node.
  - **`AuraProgressLabel`** — percentage exposed as `stateDescription`.
- `AuraSectionHeader` now carries `heading()` semantics.
- `HideAmountIconButton` content descriptions were hardcoded English; now
  localized via `a11y_show_amounts` / `a11y_hide_amounts`.
- **Removed the 🏷️ emoji fallback** in `CategoryAvatar` (an AGENTS.md rule-5
  violation). Unknown categories now draw a neutral Material vector;
  user-created emoji categories still render as text.

### Accessibility

- **`heading()` on 12 screens** that had none (only 4 of 16 had it). Every screen
  now has a real title node for TalkBack heading navigation. Splash is exempt — it
  is a brand moment, not a document.
- Converted the three fixed heights that hold text (`OnboardingScreen` CTA,
  `ClassificationScreen` action, `SettingsScreen` theme tile) to `heightIn`, so
  they no longer clip their own label at 200% font scale. The rest of the fixed
  heights are skeletons, dividers and charts, where fixed sizing is correct.
- `AnimatedMoney` exposes its rendered text as `contentDescription`, so TalkBack
  reads a money value as one utterance instead of digit-by-digit.

### i18n

Restored **full EN↔HI key parity**: 13 keys were missing from Hindi, including
the three Paging-3 strings (`activity_load_error`, `activity_loading_more`,
`activity_retry`) that handoff 0006 had claimed were complete. Both locales now
have 360 keys.

### Visual regression now covers real states and whole screens

The screenshot suite only rendered a hand-built component gallery, so the whole
screen layer was outside the net.

- `ScreenScreenshotTest` renders the **empty, error and stat-tile states in all
  three themes** (9 baselines), with values chosen to exercise lakh grouping,
  negatives and semantic colours.
- `FullScreenScreenshotTest` drives the **real `DashboardScreen`** end-to-end
  against a deterministic in-memory repository, in all three themes (3
  baselines). This is the piece that was missing entirely.

**This immediately caught a real bug.** The first full render showed the balance
hero as `₹80,882.00` directly above sub-totals reading `−$85,000.00` — because
`AnimatedMoney` defaulted to a hardcoded `CurrencyStyle.INR` while `formatMoney`
used the theme's currency. Fixed by defaulting the composable parameter to
`LocalCurrencyStyle.current`, and pinned with a regression test asserting the
two agree. Neither the unit tests nor the component screenshots could have
found this; only rendering the assembled screen did.

Fixture dates are fixed rather than `now()`-derived so the committed baselines do
not drift. The trade-off is that the cash-flow chart renders empty in the
baseline (the fixture week is outside the current week) — a drifting baseline
that fails CI weekly would be worse than one stable baseline with a blank chart.

### Errors are typed, localized, and actually rendered

The audit found that **two screens set an error state and then silently dropped
it**. `DashboardViewState.error` and `BudgetViewState.error` were both populated
by their ViewModels and never read by any composable: a failed dashboard load
rendered as an empty-looking screen, and a failed budget save made the sheet
simply stop responding. Nothing failed loudly, so nothing was caught.

- New `ui/core/UiError.kt`: a typed reason (`LOAD_FAILED`, `SAVE_FAILED`,
  `DELETE_FAILED`, `RESTORE_FAILED`, `NO_TRANSACTION_FOUND`, `MODEL_UNAVAILABLE`,
  `PERMISSION_DENIED`, `UNKNOWN`) carrying a string resource and a `retryable`
  flag. All three MVI states moved from `error: String?` to `error: UiError?`.
- **No screen ever sees an exception message.** The ViewModels used to do
  `error = e.message`, which put strings like
  `SQLiteConstraintException: UNIQUE constraint failed ...` in front of users.
  They now log the cause through `AuraLog` and report a reason.
- **Dashboard** renders `AuraErrorState` with a retry when a load fails.
- **Budget** and **Classification** render a new inline `AuraErrorBanner`
  (error-container, icon, dismiss, `liveRegion` so TalkBack announces the
  failure). Inline rather than full-screen because the user keeps their data.
- Added `ClearError` intents to both ViewModels so the banner is dismissible.

### Design system: the new primitives got used

- `AuraErrorBanner` for in-screen action failures (above).
- `AuraStatTile` gained a `contained = false` mode for use inside an existing
  `AuraCard` (a container inside a container reads as a rendering bug). Adopted
  in the Budget "all budgets" health block.
- Corrected an earlier claim: the analytics, plan and savings screens were
  already composing `AuraCard` + `AuraType` tokens properly. They were not
  duplicating tiles, and were left alone.

### ClassificationScreen split

The largest file in the app (1211 lines, 20 composables) is now three files:

| File | Lines | Contents |
|---|---|---|
| `ClassificationScreen.kt` | 198 | Main screen, tab host, error banner |
| `ClassificationReviewTab.kt` | 593 | Step indicator, analysing, result card, category selector, manual entry |
| `ClassificationSmsTab.kt` | 550 | Permission gate, message list, swipe triage, AI download status |

Cross-file functions became `internal`; nothing else changed behaviourally.

### Visual defects found by rendering the screens, and fixed

The full-screen harness was then used to *review* the screens that were previously
never rendered. Three real defects, all found by looking rather than by testing:

1. **A completed savings goal still offered a prominent "Add funds" button.**
   "Japan trip" sat at 100% with a green "Reached" badge directly above a full-width
   purple button inviting the user to fund a goal that was already met. The button
   is now hidden when the goal is complete, with the edit/delete actions right-aligned
   in the space it would have occupied.
2. **Budget cards wasted half their width.** The bottom row is `SpaceBetween` with
   the period on the left and a conditional warning on the right — but for any
   healthy budget under 80% the right side was empty. It now shows the headroom
   (`$4,851.00 left`), which is the number people actually want on a budget.
3. **The same expense was two different colours on one screen.** In Insights the
   category legend rendered amounts in `onSurface` while "Top merchants" rendered
   them in `expenseAmount`. The app's established convention is that an expense
   renders in `expenseAmount` (16 call sites, including the transaction list), so
   the legend was the outlier and was corrected.

### Enforcement

New `DesignSystemGuardTest` (6 checks) turns the documented-but-unenforced rules
into build failures: no hardcoded `fontSize`, no raw hex in screens, no
`Modifier.shadow` on content, no emoji in chrome, an error state must exist, no
empty `catch {}`.

New `ErrorStateWiringTest` (2 checks) closes the loop on the bug above:
- every MVI state that declares an `error` field must be read by a composable in
  the same feature;
- no screen state may be assigned a raw `.message`.

## 2. Decisions

- **Explicit Indian grouping over platform CLDR.** Deterministic money rendering
  beats platform-dependent formatting; a finance app cannot ship "1,23,456" that
  sometimes renders as "123,456".
- **Currency defaults to the device locale, not INR.** Amounts are stored as bare
  numbers, so presentation is the only place currency belongs. An Indian default
  is a user preference, not a hardcoded assumption.
- **Baseline lint rather than fixing all 81 remaining issues.** The revamp
  incidentally cleared 13 (94 → 81); the rest are `MissingTranslation`,
  `NewerVersionAvailable`, `GradleDependency` and similar.
- **SYSTEM is the default theme.** Following the OS is the least surprising
  behaviour and removes a class of support questions.
- Guard tests are **source-level**, not runtime: they need no emulator, run in
  under a second, and fail on every unit-test invocation.
- Skipped the Sleek design API — it needs a paid key and would send app data to a
  third party. Worked from Material 3 guidance instead.
- **A bare apostrophe breaks the resource build.** `strings.xml` in this project
  uses Android's `\'` escape (`Couldn\'t`); a literal `'` — or a `&#39;`
  entity — fails `mergeFreeDebugResources` with the misleading
  *"Invalid unicode escape sequence"*, and the error names the *resource*, not
  the offending character. Every other string in the file already uses the
  escaped form; new strings must too.

## 3. Verification

| Check | Result |
|---|---|
| `./gradlew :app:compileFreeDebugKotlin` | ✅ BUILD SUCCESSFUL |
| `./gradlew testFreeDebugUnitTest` | ✅ **234 tests, 0 failures, 0 errors** |
| `./gradlew :app:lintFreeDebug` | ✅ no new issues (80 filtered by baseline, down from 94) |
| `./gradlew :app:lintPlayDebug` | ✅ no new issues |
| `./gradlew :app:assembleFreeRelease` | ✅ (unsigned — no keystore configured, as expected) |
| `./gradlew :app:assembleFreeBenchmark :benchmark:assemble` | ✅ |
| `./gradlew :app:assemblePlayDebug` | ✅ |
| EN↔HI string parity | ✅ 0 missing / 0 extra |
| `fontSize` outside `ui/theme` | ✅ 0 (was 68) |
| Raw hex in screens / emoji in UI code | ✅ 0 / 0 |
| Screens with `heading()` | ✅ 12 added, only Splash exempt |
| Screens dropping a declared `error` | ✅ 0 (was 2) |
| Screenshot baselines | ✅ 15 committed (3 gallery + 9 state + 3 dashboard) |
| Screen review renders | ✅ 5 more written to `build/inspect/` (gitignored, not committed) |

New tests: `DesignSystemGuardTest` (6), `ErrorStateWiringTest` (2),
`MoneyFormattingTest` (13), `ScreenScreenshotTest` (9 renders),
`FullScreenScreenshotTest` (3 renders).

## 4. Open risks / next step

- **Only `DashboardScreen` is covered by *committed* full-screen regression.**
  Analytics, Plan, Budget, Goals and Settings were rendered and reviewed, but their
  baselines are deliberately **not** committed: they depend on `LocalDate.now()`
  (period-scoped queries) and would drift into a weekly CI failure. The
  `build/inspect/` renders are throwaway. If they need a real gate, give each a
  fixed-date fixture rather than letting the baseline drift.
- **Activity (`TransactionListScreen`) is still never rendered at all** — it takes a
  Paging-backed ViewModel, which the current harness cannot feed without a real
  Room/Paging source. This is the most important screen after Home and the last
  big gap in visual coverage.
- **The cash-flow chart is empty in the committed baseline** (fixture week is
  outside the current week). Deliberate: a `now()`-derived baseline would drift
  and fail CI weekly. If chart regressions need catching, add a dedicated
  deterministic chart fixture rather than making the screen baseline drift.
- **~11 fixed `.height()` values remain** — all skeletons, dividers and charts,
  which is correct, but worth a pass if any start holding text.
- **`stateDescription` is used only a handful of times.** `AuraAreaChart` and
  `AuraDonutChart` still convey data visually only; the donut has a
  `contentDescription` but the area chart's series is not described.
- **Screenshots are rendered on macOS while CI runs ubuntu.** The cross-platform
  Roborazzi drift risk flagged in 0009 is now more material, because whole-screen
  baselines have far more pixels to differ. Verify the gate on CI before relying
  on it.
- **`ClassificationViewModel` is 495 lines** and holds most of the SMS + save +
  batch logic. The screen split did not touch it; it is the next structural
  target.
- `AuraErrorState` is used on Dashboard only. The remaining list-style screens
  (Activity, Insights) have paging-level error handling that should adopt it.
- The non-UI debt from handoff 0009 §4 is untouched: `CancellationException`
  swallowing, `runBlocking` in `LlamaCppLlm`, the dead duplicate-SMS guard, and
  the absent DAO/migration tests.
