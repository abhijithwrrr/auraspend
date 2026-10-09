# Play Store assets — AuraSpend

Everything here is what is uploaded to Google Play for the default (en-US) store
listing. The live listing lives in the Play Console; this directory is the
versioned source so assets can be changed and re-uploaded later.

| File | Used for | Play requirements |
|---|---|---|
| `icon-512.png` | Store listing icon | 512×512, 32-bit PNG |
| `feature-1024x500.png` | Feature graphic | 1024×500, PNG/JPEG |
| `screenshots/01..07` | Phone screenshots, in listing order | 2–8 images, 16:9 or 9:16, 320–3840 px per side |
| `feature-graphic.html` | Source for the feature graphic | render with headless Chrome (see below) |

## Listing text (en-US)

These are the exact strings saved on Play (also kept here so they can be
diffed/updated):

**Title** (30 max):

```
AuraSpend: Expense Tracker
```

**Short description** (80 max):

```
Offline expense tracker: budgets, subscriptions, insights. No ads, no tracking.
```

**Full description** (4000 max): see `../../docs/handoffs/0017-play-console-setup.md`
— the text was composed for the 0.1.1 listing and lives in the Play Console's
store listing editor. If it is edited there, copy the new text back here in the
next handoff.

## Regenerating the feature graphic

```bash
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  --headless=new --disable-gpu --hide-scrollbars --allow-file-access-from-files \
  --force-device-scale-factor=1 --window-size=1024,500 \
  --screenshot=docs/store/feature-1024x500.png \
  "file://$PWD/docs/store/feature-graphic.html"
```

## Screenshot provenance

Captured from a debug build on the `aura_spike` AVD (Play Store image,
1080×2400), with the display overridden to `1080x1920` (16:9 — the repo's older
`docs/screenshots/` are 720×1600 i.e. 9:20 and are *not* valid Play sizes), dark
theme, `en-IN` rupee formatting, and demo data from
`app/src/androidTest/java/com/awbuilds/auraspend/DemoDataSeeder.kt`. One extra
₹88,000 "Consulting invoice" income row was inserted into the emulator's debug
database to make the balance positive for the screenshots; it exists only on
that emulator, not in the seeder or the app.

Note: the `dev` and `prod` flavors are feature-identical (ADR 0010); the
screenshots would be pixel-identical on the prod build.
