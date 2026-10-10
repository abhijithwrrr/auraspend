# 0019 — Play marketing assets (composed deck + feature graphic) and the Drive-backup note

**Branch:** `phase-agpl-oss-readiness` · **Date:** 2026-10-10

## What changed

### 1. Google Play assets: composed screenshots + refreshed feature graphic (uploaded)

The 0.1.1 listing's screenshots were raw app captures. They are now seven
composed ads at **1080×1920 (9:16, Play-valid)**, produced with the
`app-store-screenshots` skill editor (Aurora theme, isolated canvas):

| # | Layout | Headline | Screen shown |
|---|---|---|---|
| 1 | hero | "Your money. Nobody else's." | Home (dark) |
| 2 | device-bottom | "Spending tracked automatically." | Activity |
| 3 | device-top (inverted) | "See where it all goes." | Insights |
| 4 | two-devices | "Dark or light. Your call." | Home dark + light |
| 5 | device-bottom | "Budgets, bills, all in one place." | Plan |
| 6 | hero | "Log a spend in seconds." | Quick-add |
| 7 | no-device | "No ads. No accounts. No tracking." | — (open-source close) |

- **Feature graphic** re-rendered (`feature-graphic.html`) with the phone
  showing a fresh native capture.
- Both were **uploaded to Play**, replacing the previous set; the listing was
  saved as a **draft** — **not sent for review** (unchanged instruction), and
  verified by reload: screenshots **7/8 in order 01→07**, feature graphic 1/1,
  title/short/full counters untouched.
- Two fixes were needed in the editor for Android Phone output, both documented
  in `docs/store/README.md`: the Android frame aspect is **9:20** to match the
  native captures (a 9:16 frame cropped 11 % off each side through
  `object-fit: cover`), and the deck runs in **isolated** canvas mode because
  no element crosses screens.

### 2. Captures redone at native 1080×2400 on Android 17

The earlier captures forced `wm size 1080x1920` so the *files* were Play-valid,
which letterboxes a 1080×2400 panel — the black bars visible in the emulator.
Captures now come from the **native 1080×2400** display (real phone layout);
Play's 9:16 requirement applies to the exported asset, which is the editor's
1080×1920 canvas. Raw captures live in `docs/store/raw/`.

Exports are now deterministic: `node tools/export.js android dist/android.zip`
(Playwright-core driving system Chrome headless), because driving the app in a
long-lived Chrome tab via Apple Events proved unreliable — the tab silently
stopped executing page JavaScript. The script is archived at
`docs/store/export.js`; the editor project state at
`docs/store/app-store-screenshots.json`.

### 3. README: Google Drive backup explained per install source

New section: Play installs sign in and back up to the user's **own Google
Drive** out of the box; a self-built APK needs its own OAuth client (Google
matches package id + signing certificate), and its backup still lands in the
user's own Drive; CSV export/import works everywhere with no Google service.

### 4. `tools/seed_demo_data.sh`: dev-flavor launch fix

`am start -n $PKG/.MainActivity` resolved to
`com.awbuilds.auraspend.dev/.MainActivity`, but the activity class keeps the
source package (`com.awbuilds.auraspend.MainActivity`). The script now carries
an explicit `MAIN_ACTIVITY` constant. Found while seeding the emulator for the
new captures; verified on-device afterwards.

## Verification (measured)

| Check | Result |
|---|---|
| Play listing reload | screenshots 7/8 in order `01-home … 07-open-source`; feature 1/1; counters 26/30, 79/80, 2355/4000 unchanged; "Your changes have been saved" |
| Exported slide sizes | 7× 1080×1920; feature graphic 1024×500 (accepted by Play's validator on upload) |
| Feature graphic render | 1024×500, headless Chrome, embedded phone shows the new capture |
| Seed script | `seeded: 19 lines OK`; app launches on the dev package; ₹58,588 balance from demo data + the extra ₹88,000 row |
| Not submitted | no send-for-review action was taken anywhere |

## Open risks / next step

- Play still waits on: send-for-review → the SMS permission declaration that
  surfaces at review → 12 testers × 14 days → "Apply for production".
- **No Hindi variant** of the composed screenshots yet; the app ships `hi`, so
  add a `hi` locale in the editor if the Hindi listing is ever created. The
  deck's copy is English-only today.
- The editor lives **outside the repo** (`~/projects/auraspend-store-assets/`).
  Do not put it under `docs/` — that directory is the GitHub Pages root.
- This handoff changes **no app code**; the signed AAB staged on Play
  (versionCode 2) remains the current artifact.
