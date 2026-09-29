# 0015 — On-device UI review: three functional bugs, one privacy bug, and three of my own false claims

## 1. Why this session exists

Asked to install the app on the connected moto g40, run it with dummy data, and
report which screens need UI alignment and polishing. The review found far more than
polish: **three functional bugs, one privacy/consent bug, and two data-correctness
bugs**, none of which any test covered, because every one of them only shows up on a
real device with real data.

The method is worth recording: a `DemoDataSeeder` instrumented test wrote realistic
data into the **real** app database, then the app was driven by hand with `adb input`
and inspected via `uiautomator dump` for exact bounds rather than guessed coordinates.

## 2. The privacy bug — the worst thing found

**On a fresh install, opening Smart Add silently downloaded the 22 MB model, enabled
the auto-read toggle and ran an inbox scan. Nothing was rendered. No prompt, no
dialog, no card.**

`ClassificationScreen.kt:161` was:

```kotlin
if (state.consentRequired) {
    LaunchedEffect(Unit) {
        notificationPermissionLauncher.launch(POST_NOTIFICATIONS)
        viewModel.handleIntent(ClassificationViewIntent.ConsentResult(true))  // no tap
    }
}
```

The state flag was set correctly, and the `classification_local_ai_title` /
`classification_local_ai_message` strings existed for exactly this card — but the
screen accepted on the user's behalf instead of asking. Verified on device: after one
tap, `files/models/minilm-l6-v2-q8.onnx` was **23,026,053 bytes** with no prompt on
screen.

This directly contradicted the app's own claim (ADR 0008) that nothing happens without
the user choosing it, and it contradicted a statement I had made to the user earlier in
the same session. See §6.

Fixed: the auto-accept is gone and a real inline card is rendered with **Not now** /
**Add (~22 MB)**, requiring an actual tap. Added `ClassificationViewIntent.DismissModelConsent`
so declining clears the offer *without* recording consent — Settings can still start the
download later. Re-verified on a wiped install: **0 files** in `files/models` before the
tap, and a `.part` file growing after it.

## 3. Functional bugs

**Activity screen: every row was dark red with a trash icon on top of the amount.**
`TransactionEntryRow` (`TransactionRow.kt`) is a bare `Row` with no background, used as
the foreground of a `SwipeToDismissBox` whose background is `errorContainer` plus a
delete icon. A transparent foreground cannot hide its own background, so the delete
state showed through on all 32 rows at rest and the amounts were unreadable. It looked
fine on the Dashboard's "Recent" list — which is why nothing caught it. Fixed by giving
the row an opaque `colorScheme.background`; both call sites sit directly on the page
background, and the comment says so for the next caller.

**Plan hub reported ₹0 spent while the Dashboard reported ₹4,928.** `spentAmount` is a
denormalised column that **nothing ever writes back** — `BudgetViewModel` and
`DashboardViewModel` each recompute it in memory, and `PlanHubScreen` just summed the
stored column. Now uses the existing `BudgetSpending.withFreshSpent`, the same helper
the Budgets screen uses, so the two screens cannot disagree. Verified: Plan now shows
₹33,467 of ₹26,000 at 100%.

**Settings' last row sat under the system navigation bar.** The list had
`statusBarsPadding()` at the top and nothing at the bottom; `navigationBarsPadding()` was
on the SnackbarHost only. Fixed on the scroll container.

## 4. Data-correctness bugs

**One bad enum string permanently bricked the dashboard.** All three *write* paths
validate — `BackupSerializer` uses `enumOrNull ?: return@forEachObject`, `CsvManager`
try/catches, `SmsAiEnricher` uses `runCatching` — but the *read* path used bare
`Enum.valueOf` in four places in `Mappers.kt`. A subscription row with
`billingCycle = "ANNUAL"` threw inside a Room `Flow.map`, propagated out of the
dashboard aggregation, and blanked the home screen with no recovery short of
reinstalling. Found on-device, not theorised.

Fixed with a `toEnumOrDefault` helper that logs and falls back. The fallback for
`TransactionType` is deliberately `EXPENSE`, because a mis-parsed `INCOME` would inflate
a balance. `recurrenceFrequency` stays `null` when the column is null — defaulting it
would make every non-recurring transaction look like it recurs monthly. Pinned by
`MappersEnumSafetyTest` (7 tests).

**Two screens computed "recurring monthly" differently.** The Dashboard summed raw
amounts, counting an annual charge twelve times over: **₹3,745/mo** against the Plan
hub's **₹2,370.90/mo**. The Dashboard was wrong. Extracted `RecurringCost` and pointed
both at it; pinned by `RecurringCostTest` (5 tests), which asserts the raw sum and the
monthly-equivalent are deliberately *not* the same number.

**Analytics showed `↑ 46843% vs last month`** with no separator, meaningless against a
near-empty prior month. Capped: a delta beyond ±999% is now suppressed rather than
printed. `Transport 0% ₹2,795` — a real amount truncated to 0% — now floors at 1%.

## 5. Polish

- **Minus sign consolidated.** The sign was re-derived by hand in four places. Now one
  `MINUS_SIGN` (U+2212) and one `formatSignedMoney`, so no call site invents a glyph.
- **Money precision.** Summary figures pass `fractionDigits = 0`; the monthly-equivalent
  would otherwise render `₹2,370.92` after the maths fix.
- **Budget card alignment.** `maxLines = 2` (a previous fix for clipped names) made
  cards different heights, so amounts sat on two different baselines across the
  carousel. Now reserves two lines via `heightIn`, which also grows at 200% font scale.
- **Naming.** One feature had four names — "Local AI", "Download model", "Smart
  categories", "Intelligent features". All now "Smart categories". The Hindi was
  *already* consistent (`स्मार्ट श्रेणियाँ`), so English was aligned to it.
- **Off-brand chrome.** The stock Android robot became `AutoAwesome`, matching the
  Smart Add quick action, and the row is `Alignment.Top` so the glyph no longer floats
  below a two-line title.
- **Insets.** `ModalBottomSheet` in the quick-add sheet now takes
  `contentWindowInsets = { WindowInsets.navigationBars }`.

## 6. Three of my own claims were wrong

Recording these because each was reported to the user as fact and two were not.

1. **"The encoder is completely dead in the release build."** I searched the DEX for
   `com/microsoft/onnxruntime` and got zero. The package is `ai.onnxruntime`. The
   classes are present and un-renamed. Caught by re-reading the imports.
2. **"The minus glyph is inconsistent between screens."** I read a font-size difference
   as a different character. Checked the accessibility text: `U+002D` in both places.
   There was no inconsistency — only duplicated sign logic, which is now consolidated
   anyway.
3. **"The Merchant field is unreachable behind the nav bar."** It was partially visible
   and fully reachable by scrolling; I confirmed Save lands at y=2060..2111, clear of the
   nav bar. The inset change is a robustness improvement, not a bug fix.

The lesson is the one from handoff 0013: **score the system, never the model** — and
its corollary for review work: *a screenshot is a hypothesis, not a measurement.* Two of
these three were caught only by going back to the bytes.

**And a correction to an answer given earlier in this session.** I told the user the
22 MB download "only ever starts from an explicit tap." It did not — §2 shows it
auto-started. My trace of `start()`'s callers was right and the conclusion was wrong,
because the caller was a `LaunchedEffect`, not a user.

## 7. Verification

```
./gradlew :app:compileFreeDebugKotlin testFreeDebugUnitTest :app:lintFreeDebug
                                                # BUILD SUCCESSFUL, no new lint issues
```

**265 unit tests, 0 failures** (was 253; +12 new). Every fix was confirmed on the
device, not just in code: Activity rows no longer red, Plan shows real spend, Dashboard
and Plan both read ₹2,370/mo, budget amounts share a baseline, and a wiped install
leaves `files/models` empty until Add is tapped.

The three `screen_dashboard_*.png` baselines are regenerated. The new one was inspected:
budget amounts align and subscriptions reads `$768/mo`.

## 8. Open

- **Horizontal carousels still clip at the right edge** with no scroll affordance —
  the Budgets row and the Activity filter chips. Judged acceptable Material behaviour;
  left alone rather than adding a peek/fade that may not suit the design.
- **Smart Add's lower ~60% is empty** on the Paste tab. It is a short form in a
  full-height `LazyColumn`; making the field fill the screen or centring the content
  would both be worse, so left alone deliberately.
- **"Saving -93%"** is arithmetically right but reads oddly. Left rather than reworded,
  because it needs a product decision on what a negative savings rate should say.
- **The swipe-to-delete itself is unverified.** `confirmValueChange` is deprecated in
  current Compose foundation and the build warns about it; if it is genuinely ignored,
  swiping may no longer call `deleteWithUndo`. Not tested — worth checking.
- **Nothing is validated by CI.** Four commits touched the release build with no
  workflow firing on branch pushes.

## 9. Memory

- `memory/decisions.md` D13 — consent must be a tap, never a `LaunchedEffect`.
