# AuraSpend

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/minSdk-30-green)](app/build.gradle.kts)
[![Target SDK](https://img.shields.io/badge/targetSdk-36-green)](app/build.gradle.kts)

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
- **Architecture**: Clean Architecture + MVI
- **Local Storage**: Room Database
- **Cloud**: Google Drive API (premium)
- **Target SDK**: Android 16 (API 36)

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
# Edit secrets.properties with your API keys
./gradlew assembleFreeDebug
```

## Contributing

See [CONTRIBUTING.md](.github/CONTRIBUTING.md) for build flavors, code style, and PR process.

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
