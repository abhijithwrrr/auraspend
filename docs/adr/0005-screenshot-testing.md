# ADR 0005 — Robolectric + Roborazzi screenshot tests

**Status:** Accepted · 2026-09

## Context

UI regressions were invisible until someone ran the app. Instrumented Compose
tests need a device and are slow; the project wanted a visual net that runs in
the normal JVM test task.

## Decision

Use **Robolectric with native graphics + Roborazzi**:

- `AuraScreenshotTest` renders the Aurora component gallery and captures PNGs
  for light, dark and AMOLED.
- A plain `TestApplication` keeps WorkManager/Room out of JVM tests.
- Tests run in `./gradlew testFreeDebugUnitTest`; CI uploads
  `app/build/screenshots/` as an artifact.
- Recording is enabled via `roborazzi.test.record` for now; moving to
  Roborazzi's record/verify tasks (with committed baselines) is the next step.

## Consequences

- Visual changes are reviewable as PNG diffs without a device.
- Screenshots use real resources (bundled font, colours), so token changes are
  caught.
- Robolectric adds ~1 min to the test task and downloads SDK jars on first run.
