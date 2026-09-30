# Changelog

All notable changes to AuraSpend are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and
the project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
from 0.1.0 onward — though note that below 1.0.0 the leading digit carries no
compatibility promise, so minor versions may contain breaking changes to
internal APIs.

## [Unreleased]

Nothing yet.

## [0.1.1] — 2026-09-30

Substantial pre-1.0 release. The headline is that the on-device categoriser
shrank from 468 MB to 22 MB, and the project changed licence.

### Changed

- **Licence: Apache-2.0 → AGPL-3.0-or-later.** So that a modified AuraSpend
  offered to users over a network must offer its source too, which Apache-2.0
  does not require. Irreversible for this code. See
  [ADR 0009](docs/adr/0009-agpl-3.0-relicensing.md), which also records what the
  change does *not* buy.
- **On-device categoriser: 468 MB generative model → 22 MB int8 encoder.**
  `EmbeddingClassifier` does nearest-centroid classification over ONNX Runtime
  and a quantised `all-MiniLM-L6-v2` sentence encoder, replacing a bundled
  Qwen/llama.cpp. Five candidate runtimes were measured; see
  [docs/evals](docs/evals/README.md).
- **Two ABIs instead of four** (`arm64-v8a`, `armeabi-v7a`). AAB 35.4 → 20.5 MB,
  APK 78.8 → 37.2 MB. ChromeOS is deliberately unsupported.
- The model is now downloaded at runtime after an explicit opt-in, and its
  SHA-256 is verified against a pinned digest before use.

### Fixed

- **A fresh install silently downloaded the 22 MB model** and enabled auto-read
  with no prompt. Consent was a `LaunchedEffect` that auto-accepted; it is now a
  tap on an inline card.
- **Swipe-to-save and swipe-to-delete were silent no-ops.** Both used
  `confirmValueChange`, deprecated without replacement in this Compose version,
  so the callback was never invoked — the row animated to its anchor, painted a
  coloured background, and changed nothing. On Activity that meant a transaction
  stayed in the database while the UI implied otherwise. Both now drive off the
  settled state, and both are undoable.
- **Triage inbox never drained.** Saved and dismissed messages stayed listed
  forever. They now leave the list, and undo brings them back. A dismissed
  message was also indistinguishable from a ready-to-save one, so "Save All"
  could resurrect one; dismissed now has its own status.
- **Plan reported ₹0 while Dashboard reported real spend** — a stale
  `spentAmount` column.
- **Dashboard and Plan disagreed on recurring totals** (₹3,745 vs ₹2,370.90).
- Unguarded `Enum.valueOf` calls in `Mappers.kt` — one corrupt value could brick
  the dashboard.
- Transport category showing `0%` alongside ₹2,795.
- A `↑46843%` delta in the Insights header.
- Settings and Quick Add ignored bottom system-bar insets.
- Emoji rendered as a monochrome Android robot instead of the intended glyph.
- Remaining ASCII minus signs in money strings, replaced with U+2212.

### Added

- **Open-source licenses screen** (Settings → About). Ships the full AGPL-3.0
  text and third-party attributions in-app, which AGPL-3.0 §4 requires. Before
  this the app displayed only the string `Apache-2.0` with no way to read
  anything.
- **A CLA with copyright assignment**, so contributions can be relicensed as a
  whole later. Under AGPL this is an obligation, not a formality: the 0.1.1
  relicensing was only possible because there was a single copyright holder.
- `tools/check_license.sh`, run in CI. Fails if the licence string is
  inconsistent across `LICENSE`, `README`, both string locales, the splash
  footer, the website, and the legal pages, or if a dependency has no
  third-party attribution. The licence had already drifted across eleven files.
- `tools/verify_r8_release.sh` and `tools/r8_scores.py` — release verification
  against the Google Play technical requirements.
- Demo data seeding (`tools/seed_demo_data.sh`) for manual testing.

### Verified

- Regex baseline: 13.8% exact match, type 95.4%, category 36.9%. Pinned by
  `RegexBaselineEvalTest` so the regex layer cannot regress.
- The shipped encoder destroys **0/46** transactions, where SmolLM2 destroyed
  43/46, Qwen2.5-0.5B 8/46, and FunctionGemma-270M 5/46. `mayDiscard` now
  requires both `isTransactionProbability >= 0.9` and an unparsed amount.
- Release bundle: 3.68 MB uncompressed DEX, so Play's February 2027 ≥25%
  shrinking floor does not bind. APK is 16 KB page-size aligned, and the JNI
  classes ONNX resolves at `JNI_OnLoad` survive R8 un-renamed.
- 274 unit tests, 0 failures. Screenshot baselines cover `DashboardScreen` in all
  three themes.

## [0.1.0] — 2026-05-13

First alpha. Feature-complete for its scope: Material 3 UI, bank SMS
classification, budgets, subscriptions, savings goals, and Google Drive backup.
Licensed under Apache-2.0.

[Unreleased]: https://github.com/abhijithwrrr/auraspend/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/abhijithwrrr/auraspend/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/abhijithwrrr/auraspend/releases/tag/v0.1.0
