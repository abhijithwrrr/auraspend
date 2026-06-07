# Contributing to AuraSpend

Thanks for your interest! Here's how to get started.

## Build Flavors

AuraSpend uses two build flavors:

| Flavor | Command | Play Store IAP | Use Case |
|--------|---------|----------------|----------|
| `free` | `./gradlew assembleFreeDebug` | Stub (premium unlocked) | Development, F-Droid, self-build |
| `play` | `./gradlew assemblePlayDebug` | Real Play Billing | Play Store release |

The `free` flavor has all premium features unlocked. Only the Play Store build enforces the paywall.

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

By contributing, you agree that your contributions will be licensed under Apache 2.0.
