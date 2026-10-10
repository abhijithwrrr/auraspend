# 0024 — screenshot tests removed; signed release pipeline verified

**Status:** done 2026-10-10

## Decision — no more screenshot tests (owner call)

The Robolectric + Roborazzi screenshot suite and its CI drift gate are
removed. Visual changes are verified by hand in all three themes again; the
automated design nets that remain are `DesignSystemGuardTest`, the
a11y/semantics tests and `ErrorStateWiringTest`.

Removed:

- `ScreenScreenshotTest`, `FullScreenScreenshotTest`, `AuraScreenshotTest`
  (20 of 274 tests), the 15 committed baselines in
  `app/src/test/screenshots/`, and `tools/compare_screenshots.py` (which
  existed only for the tolerance gate).
- `roborazzi` + `roborazzi-compose` from the version catalog and test deps.
  `robolectric` stays — `SwipeToActTest` runs on it.
- The `ci.yml` snapshot/compare/upload steps and the
  `roborazzi.test.record` hook in `app/build.gradle.kts`.
- Two stale lint-baseline entries and the references in README,
  CONTRIBUTING, CLAUDE.md, `memory/context/tech-stack.md`, the pr_check
  comment and AGENTS.md. ADR 0005 is marked **superseded** — kept as the
  record of why the suite existed.

Verification: **254/254 tests green** (was 274) and `lintProdDebug` green
with the trimmed baseline.

## Signed release pipeline — verified end to end

All four repository secrets are present (`KEYSTORE_BASE64`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; set 2026-10-10 alongside
the neutral upload key). A `workflow_dispatch` run of `release.yml`
(run **38042348426**, 3m11s) proves the signed path:

- The workflow's own gate passed: *"Both artifacts verified signed."*
- The downloaded artifact set (`auraspend-release-1.1`):
  - `AuraSpend-V0.1.1.Alpha.aab` (21,534,590 B) — certificate
    `CN=AuraSpend, OU=Mobile, O=AuraSpend`
  - `AuraSpend-V0.1.1.Alpha.apk` (39,055,262 B) — same certificate
  - `SHA256SUMS` — `shasum -a 256 -c` passes locally for both files.

The *"AuraSpend: no release keystore in secrets.properties … will be
UNSIGNED"* warning is **expected inside `ci.yml`**: CI deliberately writes
no keystore; its prod-release step is only the R8 compile check. Signing
happens in `release.yml` (tag push or manual dispatch), as verified above.
A dispatch attaches nothing to GitHub Releases — tags do that.

## Next step

Unchanged from 0022/0023: Play upload-key reset → re-upload the rebuilt AAB
→ preview/send for review.
