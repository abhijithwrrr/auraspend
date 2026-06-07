# AuraSpend

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/minSdk-30-green)](app/build.gradle.kts)
[![Target SDK](https://img.shields.io/badge/targetSdk-36-green)](app/build.gradle.kts)
[![CI](https://github.com/abhijithwrrr/auraspend/actions/workflows/ci.yml/badge.svg)](https://github.com/abhijithwrrr/auraspend/actions/workflows/ci.yml)

A world-class expense manager for Android 16 with bank message classification, Material 3 UI, budget tracking, and Google Drive backup.

Made with ❤️ by AW Builds

---

## Features

| Category | Details | Premium |
|----------|---------|---------|
| **Smart Classification** | Paste bank SMS or read from inbox — auto-categorizes via regex (HDFC, ICICI, SBI, Axis, Kotak & more) | Free |
| **Dashboard** | Balance card, weekly bar chart, budget progress, subscription summary, category breakdown | Free |
| **Transaction List** | Search, date groups, swipe-to-delete, expense/income filters | Free |
| **Budgets** | Per-category monthly/weekly/yearly spending limits with progress bars | Free |
| **Recurring Subscriptions** | Track monthly costs, next billing dates | Free |
| **CSV Export/Import** | Backup and restore your transactions | Free |
| **Dark & AMOLED Theme** | Light, Dark, and true-black AMOLED modes | 🔒 Premium |
| **Advanced Analytics** | Canvas pie charts, category breakdowns, merchant insights | 🔒 Premium |
| **Google Drive Backup** | Cloud sync and restore from onboarding | 🔒 Premium |

## Build Flavors

| Flavor | Command | Play Billing | Use Case |
|--------|---------|--------------|----------|
| `free` | `./gradlew assembleFreeDebug` | Stub (all premium unlocked) | Development, self-build, F-Droid |
| `play` | `./gradlew assemblePlayDebug` | Real IAP verification | Play Store release |

The `free` flavor has all premium features unlocked at no cost. Only the Play Store build enforces the paywall.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: Clean Architecture + MVI (Unidirectional data flow)
- **DI**: Manual (Application class) — no Hilt/Koin
- **Local Storage**: Room Database
- **Charts**: Canvas-based (no external charting library)
- **Cloud**: Google Drive API v3 (premium)
- **Target SDK**: Android 16 (API 36)
- **Min SDK**: Android 13 (API 30)

## Project Structure

```
app/src/
├── main/java/com/awbuilds/auraspend/
│   ├── data/
│   │   ├── classification/   # Bank SMS parser + classifier
│   │   ├── local/            # Room DB, DAOs, entities, CSV manager
│   │   └── remote/           # Google Drive sync
│   ├── domain/
│   │   ├── model/            # Core domain models
│   │   ├── repository/       # Repository interface
│   │   └── usecase/          # Business logic use cases
│   ├── ui/
│   │   ├── analytics/        # Pie charts, spending insights
│   │   ├── budget/           # Per-category budget tracking
│   │   ├── category/         # Category management
│   │   ├── classification/   # SMS classification screen
│   │   ├── core/             # Shared scaffold, navigation bar
│   │   ├── home/             # Dashboard with charts
│   │   ├── navigation/       # NavGraph, route definitions
│   │   ├── onboarding/       # First-launch wizard
│   │   ├── premium/          # Premium feature gate
│   │   ├── recurring/        # Subscription management
│   │   ├── settings/         # Settings + premium upgrade
│   │   ├── splash/           # Animated splash screen
│   │   ├── theme/            # M3 colors, light/dark/AMOLED
│   │   └── transaction/      # List + add/edit screens
│   └── AuraSpendApp.kt       # Application class (DI)
├── free/                     # Free flavor sources (BillingManager stub)
└── play/                     # Play flavor sources (real IAP)
```

## Getting Started

### Prerequisites

- Android Studio Koala or newer
- JDK 17+
- Android SDK 36

### Setup

```bash
git clone https://github.com/abhijithwrrr/auraspend.git
cd auraspend
cp secrets.properties.example secrets.properties
```

Edit `secrets.properties` with:
- **WEB_CLIENT_ID**: Google OAuth 2.0 client ID for Drive sync
- **DRIVE_API_KEY**: Google Drive API key (optional)
- **ADMOB_APP_ID**: Your AdMob app ID (test ID is fine for dev)

Build and run:

```bash
./gradlew assembleFreeDebug
```

**First launch**: The app auto-seeds 12 default categories and shows the onboarding screen.

## Preview

| Screen | Description |
|--------|-------------|
| Dashboard | Balance card, weekly bar chart, budget progress, subscription summary, category breakdown |
| Classification | Paste bank SMS or read from inbox — auto-categorizes |
| Transactions | Search, date-groups, swipe-to-delete, income/expense filters |
| Analytics | Canvas pie chart, category breakdown with percentages, top merchants |
| Settings | Theme selector, CSV export/import, manage categories/budgets/subscriptions |
| Onboarding | 3-page carousel with Lottie animations, Google Drive restore option |

## Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](.github/CONTRIBUTING.md) for:

- Build flavor system explained
- Code style guide
- Pull request process
- Issue reporting guidelines

**First-time contributors**: Look for issues labeled `good first issue` or `help wanted`.

**Release process**: Every merge to `main` updates a draft release via [Release Drafter](.github/release-drafter.yml), grouping PRs by label. When ready to ship, publish the draft and tag it `vX.Y.Z` — the version is auto-bumped based on the highest priority label (`breaking` → major, `enhancement`/`feature` → minor, `bug`/`fix` → patch).

## Security

See [SECURITY.md](.github/SECURITY.md) for reporting vulnerabilities.

## License

```
Copyright 2026 Abhijith Warrier

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
