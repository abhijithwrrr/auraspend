# 0025 — Drive backup wired; website redesigned to match the app

**Status:** done 2026-10-10 — **everything in this session is uncommitted** by
owner instruction (no commits, no pushes)

## Drive backup (app)

The app could restore from Google Drive but had **no way to create a backup**:
`DriveSyncManager.backupLocalData()` has existed since the first release and
was never called from any screen. Restore itself was reachable only during
onboarding. Both are fixed.

- **`BackupCreateManager`** (`data/local/BackupCreateManager.kt`) — reads every
  user-owned table inside one Room `withTransaction` (transactions, categories,
  budgets, subscriptions, savings goals, SMS queue, classification memory,
  unrecognized SMS) and serializes via `BackupSerializer` (format v4). Failure
  is a logged boundary returning null, never a crash.
- Six one-shot `getAllOnce()` suspend DAO queries added; the UI keeps its Flows.
  The two test fakes implementing `SmsMessageDao` gained the new member.
- `AuraSpendApp` exposes `backupCreateManager`.
- **Settings → Data** gains "Back up to Google Drive" and "Restore from Google
  Drive" rows (busy spinner while either runs). Restore is a replace, so it
  asks for confirmation first.
- `NavGraph.SettingsDestination` orchestrates both with one shared sign-in
  launcher that remembers whether the pending action is backup or restore;
  every step is boundary-guarded and results land in a snackbar. Restore reuses
  the same atomic `BackupRestoreManager` path as onboarding.
- Strings in EN + HI; restore result messages reuse the existing onboarding
  strings. `BackupCreateManagerTest` seeds **one row per table** so a section
  the manager forgets to read fails the build — restore deletes tables
  wholesale, so a forgotten section would otherwise delete data.

## Website (docs/) — redesigned, accurate to code

`index.html`, `privacy.html`, `terms.html`, `css/style.css` rewritten; site
font is self-hosted Plus Jakarta Sans (`docs/fonts/`, OFL already in
`docs/licenses/`) instead of Google-hosted Inter. `sitemap.xml` lastmod bumped.

The design now uses the app's own Aurora palette (`ui/theme/Color.kt`: purple
`#5E3A8B`, violet, teal, lavender on ink/cream) and the raw 1080×2400 captures
in `docs/store/raw/`. Accessibility: skip link, `aria-current`, visible focus,
reduced-motion, ≥48 px targets.

Copy was checked claim-by-claim against the code. Removed from the old pages:
"forward bank SMS" (the app reads the inbox), budget-limit notifications (no
such code), "secure backups to an app-specific folder" (the backup is plain
JSON in the user's Drive root, now stated plainly), "TLS 1.3" (unverifiable),
"delete all data" button (does not exist — uninstall or Android storage
settings instead). The privacy page now describes backup and restore from
Settings → Data, the Drive `drive.file` scope, and that the backup is
unencrypted.

Legal texts: kept the owner's existing positions (Kerala governing law,
liability cap) but **flagged them for owner review**; dropped the drifting
"Data Protection Officer" and donation-channel wording the app never had.

`tools/check_license.sh` passes (the terms page keeps the lowercase
`agpl-3.0` link the check requires).

### Follow-up adjustments (same session)

- **International headline.** The hero said "Know where every rupee goes." →
  "Know where your money goes."; the lakh/crore sentence in the "Your look"
  section became "Amounts are formatted for your locale and currency." No
  India-specific wording is left on any page (the app screenshots still show
  ₹ amounts — re-capturing them in another locale is a separate task).
- **Every phone in a bezel at the hero's size.** The three section screenshots
  moved from the full-column `.shot` frame to the hero's device frame
  (`.phone` + `.phone-shot`, full-height image, 280 px like the hero; 240 px on
  mobile). The bezel gained a border so it stays visible on the dark theme.
- **Responsive pass.** The six nav links wrapped to two lines between ~721–780
  px, so the hamburger breakpoint moved 720 → 860 px (the old 720 block keeps
  the grid/section/footer stacking). The 7-card feature grid centres its
  trailing card at 3 columns and makes it full-width at 2. Audited at
  1280/1024/960/900/860/780/740/500 and 390 px.

### Second follow-up (same session)

- **Real app icon + wordmark.** The header/footer logo mark now uses
  `store/icon-512.png` (the launcher art) instead of a hand-drawn SVG. The
  wordmark is one flex item — `Aura<em>Spend</em>` used to be two, which the
  10 px logo gap rendered as "Aura Spend". "Spend" keeps its accent colour.
- **Theme toggle.** The site follows the system theme by default; a nav button
  (moon/sun) switches light↔dark, stored in `localStorage`. A head script
  applies the stored choice before first paint, every page carries a
  `theme-color` meta pair, and the CSS gained
  `:root[data-theme="dark"]` + `:root:not([data-theme="light"])`.
- **Theme-matched screenshots.** Every phone on the homepage shows the screen
  in the active theme: home-light/dark (existing) plus new emulator captures
  `quick-add-light`, `plan-light`, `settings-light`, `settings-dark`
  (1080×2400, prod app, demo data seeded) in `docs/store/raw/`. The "Your
  look" section now shows the Settings appearance screen, not the home screen.
- Verification: both themes rendered at 1280 px (hero and all three section
  phones swap); the attribute→CSS chain checked with a static
  `data-theme="dark"` page; the click handler checked live; HTML + licence
  checks green.

## Verification

- `./gradlew :app:compileProdDebugKotlin --rerun-tasks` — clean; the only `w:`
  lines are pre-existing (GoogleSignIn deprecations, `OpenInNew`/`MenuBook`
  icon deprecations, UiError annotation).
- `./gradlew :app:testProdDebugUnitTest` — **256/256 green** (was 254; +2 for
  `BackupCreateManagerTest`).
- `./gradlew :app:lintProdDebug` — no new issues.
- Licence check green; all three pages parse; every internal link/anchor and
  asset path resolves; renders verified at 1440 px (light + dark) and 390 px.

## Open risks / pending owner calls

- **Backup/restore UI is not yet exercised on a device** — Drive sign-in cannot
  run in unit tests. Needs one manual pass: sign-in prompt, backup snackbar,
  restore confirmation + summary, EN + HI.
- **"Download APK" points at GitHub Releases, which has nothing published**
  (only a draft `v0.1.0`). Publish a release or repoint the button before the
  site is public.
- Privacy/terms still contain "published on Google Play" wording in places —
  fine once the listing ships, but the owner should confirm.
- The unrecognized-SMS 30-day purge (`UnrecognizedSmsRepository.purgeOlderThan`)
  is defined but never called; the privacy page does not promise it runs.

## Next step

Owner reviews the redesign and the backup feature, then decides on committing
(branch off `dev`). Play launch steps from 0024 are unchanged: upload-key
reset → replace the staged AAB → send for review.
