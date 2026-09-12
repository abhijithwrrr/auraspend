# AuraSpend

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/minSdk-30-green)](app/build.gradle.kts)
[![Target SDK](https://img.shields.io/badge/targetSdk-37-green)](app/build.gradle.kts)
[![AGP](https://img.shields.io/badge/AGP-9.3.1-blue)](build.gradle.kts)
[![CI](https://github.com/auraspend/auraspend/actions/workflows/ci.yml/badge.svg)](https://github.com/auraspend/auraspend/actions/workflows/ci.yml)

A world-class expense manager for Android 17 (API 37) with bank message classification, edge-to-edge Material 3 UI, budget tracking, and Google Drive backup.

Made with ❤️ by AW Builds

---

## Features

| Category | Details | Premium |
|----------|---------|---------|
| **Smart Classification** | Paste bank SMS or read from inbox — auto-categorizes via regex + optional on-device AI (Qwen2.5) into subscriptions / categories / income / expense / other | Free |
| **Dashboard** | Balance card, weekly bar chart, budget progress, subscription summary, category breakdown | Free |
| **Transaction List** | Search, date groups, swipe-to-delete, expense/income filters | Free |
| **Budgets** | Per-category monthly/weekly/yearly spending limits with progress bars | Free |
| **Recurring Subscriptions** | Track monthly costs, next billing dates | Free |
| **CSV Export/Import** | Backup and restore your transactions | Free |
| **Dark & AMOLED Theme** | Light, Dark, and true-black AMOLED modes | 🔒 Premium |
| **Advanced Analytics** | Canvas pie charts, category breakdowns, merchant insights | 🔒 Premium |
| **Google Drive Backup** | Cloud sync and restore from onboarding | 🔒 Premium |

## On-Device AI (Local Categorization)

AuraSpend can run a small, fully-on-device LLM (**Qwen2.5-0.5B-Instruct**, GGUF Q4_K_M, ~400 MB) to
improve message auto-categorization into **subscriptions, categories, income, expense and other**.

- **Consent first**: the first time you open **Smart Add** a dialog asks whether you want to download the model.
  Accepting starts a **background download** (resumable, cancellable) to internal storage — nothing leaves the device,
  and it immediately runs a classification pass (reading + saving device SMS as income / expense) and opts you into
  **Auto-categorize messages** (toggle in Settings).
- **Progress everywhere**: the download progress is shown **in-app** (Smart Add banner + Settings card) **and in a
  system notification** that updates live and clears when the download finishes.
- **Hybrid classifier**: amount / merchant / date still come from the battle-tested regex parser
  (`BankMessageParser`), while the LLM handles the parts regex is bad at — *subscription detection,
  category selection, income-vs-expense*. If the model isn't downloaded (or the native runtime isn't
  linked), the app transparently falls back to the pure regex classifier, so nothing breaks.
- **Learned classification memory**: every save (manual or auto) records a normalized
  merchant/note → category mapping in a local Room table (`classification_memory`). Repeat
  merchants are categorized **instantly** — the LLM is skipped entirely — and your manual
  corrections always win over automated suggestions. Stored in `classification_memory`, migrated
  safely from previous schema versions (v5 → v6).
- **Multi-signal fusion** (`AiSignalFusion`): an explicit LLM "not a transaction" verdict vetoes
  false positives; explicit *credited/debited* keywords beat an LLM type guess on conflict;
  recurring-payment keywords (auto-debit, NACH, renewal…) force subscription classification even
  without AI; confidence rises when signals agree and drops when they conflict.
- **Hardened inference**: every LLM generation runs under a 90 s wall-clock timeout, an
  errored engine short-circuits instead of blocking, unparseable output triggers one retry, and
  JSON extraction survives markdown fences / surrounding prose / nested braces. Category names
  returned by the model are matched fuzzily ("food" → *Food & Dining*).
- **Manage it**: an **Intelligent Features** card in Settings shows status, lets you download, cancel and delete the model.

### Build prerequisites (native runtime)

To ship the APK with the llama.cpp runtime that *actually runs* the model, your build machine needs:

1. **Android NDK 29** (e.g. `29.0.14206865`) — the app's `:llama` module sets `ndkVersion`.
2. **CMake >= 3.31.6** (AGP will auto-download it during build if the SDK license is accepted).

The runtime lives in the vendored `llama-lib/` module (a cleaned copy of llama.cpp's official
`examples/llama.android`), and the upstream source is pinned as a git submodule at
`third_party/llama.cpp`. Without the native toolchain installed, the app still **compiles and runs**
with regex-only classification (the UI shows the download option but the model won't load).

### Toolchain

| Component | Version |
|-----------|---------|
| Gradle    | 9.5.0   |
| AGP       | 9.3.1 (built-in Kotlin, KGP 2.2.10) |
| Kotlin / Compose compiler | 2.2.10 |
| compileSdk / targetSdk    | 37 |
| minSdk    | 30 |
| R8        | Full mode (minify + optimize + obfuscate + resource shrinking) |

```bash
# first time, fetch the llama.cpp submodule
git submodule update --init --recursive
# build (requires NDK + CMake)
./gradlew assembleFreeDebug
```

The model is Apache-2.0 licensed (Qwen2.5-0.5B-Instruct). It is downloaded at runtime from
HuggingFace after user consent.

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
git clone https://github.com/auraspend/auraspend.git
cd auraspend
cp secrets.properties.example secrets.properties
```

Edit `secrets.properties` with:
- **WEB_CLIENT_ID**: Google OAuth 2.0 client ID for Drive sync
- **DRIVE_API_KEY**: Google Drive API key (optional)

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
Copyright 2026 AuraSpend

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
