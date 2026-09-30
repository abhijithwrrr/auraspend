# Contributing to AuraSpend

Thanks for your interest! Here's how to get started.

## Build Flavors

AuraSpend uses two build flavors, and they are **identical in features**:

| Flavor | Command | Use Case |
|--------|---------|----------|
| `free` | `./gradlew assembleFreeDebug` | Development, F-Droid, self-build |
| `play` | `./gradlew assemblePlayDebug` | Play Store release |

**Every feature ships in both flavors, and neither build carries analytics or
ads.** The flavors exist for distribution configuration only — signing, listing
metadata, and a distribution-specific permission if one is ever needed. See
[ADR 0007](../docs/adr/0007-no-paywall.md) and
[ADR 0008](../docs/adr/0008-distribution-flavors.md).

### The invariant to preserve

**No feature may live in `app/src/play/` that is absent from `app/src/main/`.**
Only services and configuration may differ between flavors. This is what keeps
the F-Droid build from ever becoming a degraded build, and it is a hard
requirement for F-Droid, which builds from source and accepts only FOSS
dependencies.

If you are tempted to add a Play-only capability, it almost certainly belongs
in `main` behind a runtime check instead.

### Screenshot baselines are platform-dependent

`testFreeDebugUnitTest` **rewrites** `app/src/test/screenshots/*.png` — it does
not compare against them. CI then fails the build if that directory is dirty,
which is a real drift gate. But Robolectric rasterises on the host JDK and OS,
so a macOS run produces byte-different PNGs from the Linux ones committed here.

**Consequence: never commit baselines you rendered locally.** Re-render on CI
(or on Linux) and take the PNGs from the `aurora-screenshots` artifact. If you
see the dashboard baselines change after a plain test run and you changed no UI,
it is the platform, not your change — revert them.

## Development Setup

1. Clone the repo
2. Open in Android Studio
3. Create `secrets.properties` in the project root (see `secrets.properties.example`)
4. Sync Gradle and run

## Code Style

- Follow Material 3 / Jetpack Compose conventions
- Use MVI pattern for screens (ViewModel + StateFlow + sealed Intent)
- Name composable files with uppercase first letter, matching the composable function
- Resource strings go in `strings.xml`, not hardcoded
- Keep `.kt` files under 400 lines — split into smaller composables or files if needed

## Pull Request Process

1. Fork the repo and create a branch from `main`
2. Run `./gradlew assembleFreeDebug` — it must build clean
3. Update docs if needed
4. Open a PR with a clear title and description
5. A maintainer will review

## Reporting Issues

- Use the bug report or feature request templates
- Include device model, OS version, and steps to reproduce
- For crashes, include the full stack trace or logcat output

## License

AuraSpend is licensed under the **GNU Affero General Public License v3.0 or
later** (AGPL-3.0-or-later). See [`LICENSE`](../LICENSE).

By contributing you agree that your contribution is licensed under AGPL-3.0-or-later.

### Copyright assignment — please read

AuraSpend relicensed from Apache-2.0 to AGPL-3.0 in 0.1.1. The relicensing was
possible because the project had a single copyright holder. **That is no longer
true the moment you contribute**, and the difference matters:

- If you retain copyright and merely license your contribution under AGPL-3.0,
  you can no longer relicense the project later — a future relicensing, dual
  licensing, or commercial exception would need your consent, forever.
- To keep the project manageable, contributions must be made **under a copyright
  assignment to AW Builds**, so that the project can be relicensed as a whole if
  that ever becomes the right call.

By opening a pull request you confirm:

1. You are the original author of the contribution, or have the right to submit
   it under these terms.
2. You assign copyright in the contribution to AW Builds, and
3. You accept that the contribution is distributed under AGPL-3.0-or-later.

This is a plain-language summary. The authoritative text is the
[GNU AGPL-3.0 §5(c)](https://www.gnu.org/licenses/agpl-3.0.html) plus the
assignment you confirm in the pull request. If you would rather not assign
copyright, you are very welcome to open an issue describing your idea instead —
that costs you nothing and I will implement it.
