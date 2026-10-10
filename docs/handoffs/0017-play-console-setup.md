# 0017 — Play Console setup for 0.1.1 (staged, not submitted)

**Branch:** `phase-agpl-oss-readiness` (continues the 0.1.1 release cut)
**Date:** 2026-10-09

## What changed

AuraSpend (the 0.1.1 build from handoff 0016) is now **fully staged in the Play
Console** — store listing, all app-content declarations, store settings, and a
closed-testing track with a **draft release containing the signed AAB**. Per
explicit instruction, **nothing was sent for review**; every change sits in
"Changes not yet submitted for review" on the Publishing overview.

Play Console identifiers, for the next session:

| Thing | Value |
|---|---|
| Developer account | AW Builds (ID `REDACTED`) |
| App | AuraSpend, `com.awbuilds.auraspend` |
| App ID | `REDACTED` |
| Closed testing track | `REDACTED` (only track, "Inactive") |
| Draft release | `2 (0.1.1-play)`, saved as draft |

### 1. Store listing (default en-US)

- **Title:** `AuraSpend: Expense Tracker` (26/30)
- **Short description:** `Offline expense tracker: budgets, subscriptions, insights. No ads, no tracking.` (79/80)
- **Full description:** 2355/4000 chars, written from the README/website copy —
  feature list, privacy promise, permissions explained honestly.
- **Icon** `512×512` (from `app/src/main/res/drawable/app_icon.png`),
  **feature graphic** `1024×500` (new; source is
  `docs/store/feature-graphic.html`), **7 phone screenshots** `1080×1920`.
- All of it was saved with "Save as draft" and verified to persist across a
  reload.

### 2. App content — 10 of 10 declarations

Privacy policy (GitHub Pages URL), no ads, no restricted access (all features
usable without sign-in), IARC content rating (PEGI 3 / Everyone / Rated for 3+;
questionnaire answered all-No), target audience **18 and over**, Data safety
**no collection and no sharing**, advertising ID **not used**, not a government
app, **no financial features**, no health features.

### 3. Store settings

App / **Finance**, tags left empty, contact email `awbuilds.support@gmail.com`,
website `https://auraspend.github.io/auraspend/`, external marketing left on
(Google may advertise the app outside Play).

### 4. Closed testing track

- **Countries:** all 178 countries / regions targeted.
- **Testers:** a private Google Group used as the tester list (address
  withheld; pre-filled by the console — see Decisions) and feedback email
  `awbuilds.support@gmail.com`.
- **Draft release:** AAB `app-play-release.aab` (21,533,826 bytes,
  sha256 `0ff65abd…c51d390`), release notes `<en-US>First closed testing
  release. Thanks for helping test AuraSpend!</en-US>`, saved as **draft** —
  "Changes saved. You can now preview your release before sending it for review."

## Decisions

1. **Nothing submitted, by instruction.** The dashboard now reads: setting up
   the app "10 of 11", closed testing "3 of 5", remaining steps locked behind
   submission ("Preview and confirm" / "Send the release to Google for review").
   The one unchecked setup item, "Set up your store listing", only ticks after
   the listing is submitted — the listing itself is complete and saved.
2. **Data safety = no collection/no sharing.** On-device SMS processing is
   exempt ("data accessed by your app that is only processed locally… does not
   need to be disclosed"); the optional Drive backup is the user's own account
   ("user chooses to upload their data directly to their own… account … does not
   need to declare the collection", Data safety FAQ) and the developer has no
   access; no analytics/ads/crash SDK exists in any build. This matches the
   privacy policy's "we do not collect, sell, or share".
3. **Financial features: none selected.** The form lists regulated services
   (lending, payments, trading…). AuraSpend is a passive tracker that never
   moves money, so "My app doesn't provide any financial features" is the
   accurate answer; no documentation was required as a result.
4. **Target audience: 18 and over.** A finance app with no child-directed
   content; avoids Families-policy obligations.
5. **Tester Google Group reused.** The console pre-filled the developer
   account's existing private Google Group and it was kept, so the closed
   test has a live distribution list from day one. **Swap it in the Testers
   tab before rolling out if AuraSpend testers should be a separate group.**
6. **Screenshots re-captured at 1080×1920.** The repo's `docs/screenshots/*`
   are 720×1600 (9:20) and not valid Play sizes (Play requires 16:9/9:16).
   New captures: `aura_spike` AVD (Play image) with `wm size 1080x1920`, dark
   theme, INR formatting, demo data from `DemoDataSeeder`. One extra ₹88,000
   "Consulting invoice" income row was inserted into the emulator DB only, to
   make the balance positive. Assets are archived in `docs/store/`.
7. **App name gained a descriptor** (`AuraSpend: Expense Tracker`) for store
   search; trivially editable later if brand purity wins.

## Verification (measured)

| Check | Result |
|---|---|
| `./gradlew :app:bundlePlayRelease` | BUILD SUCCESSFUL, `:app:signPlayReleaseBundle` ran |
| Bundle file | 21,533,826 bytes; `jarsigner -verify` → "jar verified", cert CN=AuraSpend (self-signed upload key; the key was replaced before launch — see handoff 0020) |
| Upload key | `auraspend.jks`, alias `key0`; passwords fetched from macOS login keychain entries made by Android Studio; `secrets.properties` written locally (gitignored) |
| Play upload | Console listed `app-play-release.aab`, "1 app bundle uploaded", no warnings; release name auto-filled `2 (0.1.1-play)` |
| Draft save | "Changes saved. You can now preview your release before sending it for review." |
| Store listing reload | Counters persisted: 26/30, 79/80, 2355/4000; icon 1/1, feature 1/1, screenshots 7/8 in order |
| App content overview | "You're all caught up" (10/10 actioned) |
| Countries | "Targeted (178)" with per-country list |
| Play App Signing | Protected with Play → "Good protection · Automatic protection · 1 of 1 service active" |
| AAB sha256 | `0ff65abd257f9da02465bdc39ea9f017a47a07eed5318c20d01a54941c51d390` |

## Open risks / next step

- **SMS permission declaration.** The AAB declares `READ_SMS`; Play's sensitive
  permissions policy expects a declaration for restricted permissions. The
  console did not surface a form during staging (App content shows no pending
  item), so it will likely appear **when the release is sent for review**. The
  justification to use: bank SMS are parsed entirely on-device to turn the
  user's own bank messages into expense records; nothing is transmitted; the app
  is not a default SMS handler. Verify the exact policy wording at submission.
- **Production access needs the closed test first:** 12+ testers opted-in for 14
  continuous days, then "Apply for production" on the dashboard. The Google
  Group's membership count is unknown — confirm ≥12 real opt-ins before relying
  on it.
- **Nothing is live yet.** The listing, declarations and release are all
  pending-submission changes; the app is not discoverable or installable from
  Play until the user chooses to send for review and roll out.
- To finish when the user says go: dashboard → Closed testing → "Preview and
  confirm the release" → "Send the release to Google for review" (or the
  Publishing overview's "Send app for review"), then roll out the closed test.
- The emulator used for screenshots is `aura_spike`; demo data and the INR
  preference live on it and are disposable.

## Repo additions

- `docs/store/` — canonical copies of all uploaded assets (icon, feature
  graphic, 7 screenshots), the feature-graphic HTML source, and a README with
  the listing strings and regeneration steps.
- `CLAUDE.md` / `AGENTS.md` — pointer to this handoff.

## Release workflow (same session)

`.github/workflows/release.yml` — tag-driven, **build-only**:

- Triggers: `push` of `v*` tags + `workflow_dispatch`. Never pull requests, so
  repository secrets cannot reach a fork (GitHub withholds them regardless).
- Materialises `secrets.properties` from four now-configured repository secrets
  (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; set via
  `gh secret set` as the owner, verified with `gh secret list`). A missing
  secret fails the run *before* the build, and the verify step fails on an
  unsigned artifact (`app-free-release-unsigned.apk`, `jarsigner`/`apksigner`
  checks) so a green run can never attach an unsigned binary again.
- Builds `:app:bundlePlayRelease :app:assembleFreeRelease`, then renames the
  outputs to the house convention **`AuraSpend-V<version>.Alpha.{aab,apk}`**
  (version from the pushed tag, falling back to `versionName` on manual
  dispatch; a tag/versionName mismatch warns). Writes `SHA256SUMS` over the
  renamed files, uploads workflow artifacts, publishes the Release Drafter draft
  under the tag, and attaches the renamed AAB + APK + checksums to the GitHub
  Release.
- The Play draft release was renamed to **`AuraSpend-V0.1.1.Alpha`** to match
  (release names are internal-only; 50-char limit — this is the releaseName
  the other apps on the account set via GPP).
- **No Play upload by design.** Sending for review stays a human action:
  download the AAB from the release assets and upload it on the closed-testing
  track. (The service-account API route was considered and deliberately left
  out — see the session discussion; a public repo should carry no publishing
  credential that isn't strictly needed.)
- Verified locally with the exact commands (file paths, `jarsigner -verify`
  exit 0 on both artifacts, no `-unsigned` output).
- Version codes: **2 is used by the staged 0.1.1 draft; the next uploaded
  build must be versionCode 3** (e.g. 0.1.2). The workflow does not bump it.
