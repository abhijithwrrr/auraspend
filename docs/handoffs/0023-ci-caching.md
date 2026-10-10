# 0023 — CI caching: the download tax is gone

**Status:** fixed 2026-10-10

## The symptom

Every CI run re-downloaded the Gradle distribution and all dependencies; the
first green runs took 6–9 minutes. Nothing was cached between runs in
practice, even though the workflow had cache steps.

## What was actually happening — three separate mechanisms

1. **Caches are branch-scoped, and `setup-gradle` only *writes* on the
   default branch.** The action's documented default is `cache-read-only:
   true` outside `main`, so `dev` runs restore but never save. Worse, caches
   saved on `dev` are **not** readable from `main` — `main` can only see its
   own lineage (plus `dev` reading `main`). That asymmetry produced most of
   the confusion.
2. **`main`'s cache lineage was poisoned by the incident.** The four runs
   that failed *before Gradle ever ran* (the `platforms;android-37` bug,
   handoff 0022) still saved an **empty 5–9 KB** `gradle-home` entry each.
   Restore falls back to the most recent prefix match, so the first healthy
   `main` run restored "~0 MB" and re-downloaded everything, and each early
   failure re-poisoned the next run. It self-healed only when a run went
   fully green and saved the real caches.
3. **The Robolectric runtime jars were never cached at all.** Robolectric
   downloads `android-all-instrumented` jars (100 MB+) into
   `~/.m2/repository/org/robolectric` on the first test run per machine, and
   no workflow step cached that directory — so *every* run re-downloaded
   them, silently (Gradle hides the test stdout).

An extra find: bumping `setup-gradle@v4 → v6` (part of this fix) renamed the
cache namespaces (`gradle-home-v1 → v2`, and every component cache
`v1 → v2`), so the first `main` run after the bump was cold again by design.
It re-seeded everything; that cost is one-time.

## What changed

- **New cache:** `~/.m2/repository/org/robolectric`, keyed on
  `gradle/libs.versions.toml` (Robolectric/Roborazzi versions).
- Wrapper-distribution `restore-keys` tightened — the bare `Linux-` prefix
  could match unrelated caches.
- `ci.yml`: `setup-gradle` v4 → **v6** (pr_check and release already used v6).
- `release.yml` untouched (tags-only, rare; it uses v6 but caches nothing
  extra — acceptable).

## Cache inventory after the seeding run (plain `actions/cache` entries; the
## `gradle-*-v2` component caches are written by `main` and readable by all)

- dependencies 633.55 MiB, transforms 233.94 MiB, build-cache 105.41 MiB,
  wrapper zips 133.14 MiB, generated jars 40.12 MiB, Gradle home 4.62 MiB
- Robolectric jars 133.38 MiB (one entry per branch scope)
- Gradle wrapper dist 133.13 MiB — plain `actions/cache`, writable from any
  branch (unlike setup-gradle's component caches)

## Verification — measured on the same commit

| Step | Cold run (before) | Warm rerun (after) |
|---|---|---|
| Unit tests | 195–203 s | **12 s** |
| Build dev flavor | 118–125 s | **3 s** |
| Build prod flavor | 6–7 s | **1 s** |
| Build prod release (R8) | 143–149 s | **4 s** |
| **Job total** | **6–9 min** | **52 s** |

The warm run restores every cache and executes zero downloads. A *new commit*
re-runs only the tasks whose inputs changed, so typical runs land between
these numbers — without the download tax.

## Why the screenshots still run every time

Twenty of the 274 unit tests are Robolectric screenshot tests
(`ScreenScreenshotTest`, `FullScreenScreenshotTest`, `AuraScreenshotTest`):
the dashboard plus empty/error/stats states in light/dark/AMOLED, and the
design-system components. They run inside `testProdDebugUnitTest`, and the
CI drift gate compares the freshly rendered images against the committed
baselines (handoff 0022) — so an unintended visual change fails CI instead
of shipping. With the caches warm, unchanged screenshots cost almost nothing
(the test task comes back from the Gradle build cache), and the gate itself
takes ~3 s.
