# 0022 — CI was red: stale SDK package id, then the screenshot gate

**Status:** fixed 2026-10-10 · CI green again (run 38035323657)

## Failure 1 — every job died in ~25 s

"Accept SDK licenses & install platform 37" exited 1 with
`Failed to find package 'platforms;android-37'` — before the tests ever ran.
Google ships platform packages with the minor version now: the SDK installs
as **`platforms;android-37.0`**; the bare id no longer resolves, and
`sdkmanager` exits 1 when any requested package is missing. The step had
never run on `main` (main had been stale since August, and `pr_check.yml`
installs no SDK at all — it passed on the same runner image, which proves the
platform is preinstalled), so the bug was latent from the day it was written
(ebcb8ea).

Fix: `ci.yml` + `release.yml` — install `platforms;android-37.0`.

## Failure 2 — the screenshot gate failed on all 15 PNGs

The gate compared **byte-exactly** (`git status` after the test task, which
always records with `roborazzi.test.record=true`). The baselines were recorded
on an arm64 macOS machine; CI renders on x86_64 Linux; antialiasing alone
changes every file (max channel Δ of 1–3; dashboard text ~0.09 % of pixels
above Δ8). Byte-exactness across CPU architectures is impossible, so the gate
could never pass on CI.

Fix: **`tools/compare_screenshots.py`**, a tolerance gate. CI snapshots the
committed baselines before the tests, then fails only on missing/new files, a
size change, or >0.5 % of pixels differing by >8 per channel. Measured
cross-platform noise sits ~6× below the threshold; a structural change is far
above it (a 500×500 corrupted region trips it at ~10 %). Local behaviour is
unchanged: same-platform recordings stay byte-identical and the tree stays
clean.

## Verification

- The gate was validated three ways: self-compare passes; the actual
  CI-rendered artifact passes (max 0.087 %); a corrupted image fails
  (9.96 %, exit 1).
- `tools/check_license.sh` passes locally (exit 0).
- CI run **38035323657** on `dev`: success, 6 m 49 s, full pipeline.
- `main` fast-forwarded to `dev`, so the public branch is green too.

## Next step

Resume the 0.1.1 launch checklist (Play upload-key reset → AAB re-upload →
preview/send for review).
