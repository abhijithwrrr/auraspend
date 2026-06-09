# AuraSpend Architecture

## Overview

AuraSpend is a single-module Android 16 expense tracker following **Clean Architecture** with **MVI (Model-View-Intent)** for the UI layer. The app uses **manual dependency injection** via a service locator pattern in the `Application` class, **Room** for local persistence, and **Google Drive API v3** for cloud backup.

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                     UI Layer (Compose)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────────┐ │
│  │ Screens  │  │ MVI      │  │ViewModels│  │  Navigation │ │
│  │(Composables)│ │State/Intent│ │          │  │  NavGraph   │ │
│  └──────────┘  └──────────┘  └──────────┘  └─────────────┘ │
├──────────────────────────────────────────────────────────────┤
│                    Domain Layer                               │
│  ┌─────────────────┐  ┌──────────────────┐  ┌────────────┐ │
│  │    Models        │  │   Repository     │  │  Use Cases │ │
│  │  (data classes)  │  │   (interface)    │  │            │ │
│  └─────────────────┘  └──────────────────┘  └────────────┘ │
├──────────────────────────────────────────────────────────────┤
│                     Data Layer                                │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  ┌────────────┐ │
│  │  Room DB  │  │  DAOs    │  │Repository │  │  Drive     │ │
│  │ (entities)│  │          │  │   Impl    │  │  Sync      │ │
│  └──────────┘  └──────────┘  └───────────┘  └────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

## Layers

### 1. UI Layer (`com.awbuilds.auraspend.ui`)

Each feature screen follows the MVI pattern with three components:

- **`*Mvi.kt`**: Defines `ViewState` (immutable data class) and `ViewIntent` (sealed interface)
- **`*ViewModel.kt`**: Processes intents, updates state via `StateFlow<ViewState>`
- **`*Screen.kt`**: Composable functions that observe state and dispatch intents

Example flow:
```
User taps button → Screen dispatches Intent → ViewModel processes →
Updates StateFlow → Screen recomposes with new state
```

#### Screen Index

| Screen | File | State/Intent |
|--------|------|-------------|
| Dashboard | `DashboardScreen.kt` | `DashboardMvi.kt` |
| Transaction List | `TransactionListScreen.kt` | — |
| Add Transaction | `AddTransactionScreen.kt` | — |
| Classification | `ClassificationScreen.kt` | `ClassificationMvi.kt` |
| Budget | `BudgetScreen.kt` | `BudgetMvi.kt` |
| Analytics | `AnalyticsScreen.kt` | — |
| Settings | `SettingsScreen.kt` | — |
| Onboarding | `OnboardingScreen.kt` | — |
| Splash | `SplashScreen.kt` | — |
| Recurring | `RecurringScreen.kt` | — |
| Category Management | `CategoryManagementScreen.kt` | — |

### 2. Domain Layer (`com.awbuilds.auraspend.domain`)

Contains pure Kotlin business logic with no Android framework dependencies:

- **`model/Models.kt`**: Domain entities — `Transaction`, `Category`, `Budget`, `Subscription`, `ParsedBankMessage`
- **`repository/TransactionRepository.kt`**: Interface defining data operations
- **`usecase/ClassifyMessageUseCase.kt`**: Orchestrates SMS parsing + classification
- **`usecase/SaveTransactionUseCase.kt`**: Validates and persists a transaction

### 3. Data Layer (`com.awbuilds.auraspend.data`)

Implements the repository interface and handles all data sources:

- **`local/entities/Entities.kt`**: Room entity definitions
- **`local/dao/Daos.kt`**: 4 DAO interfaces (Transaction, Category, Budget, Subscription)
- **`local/AppDatabase.kt`**: Room database singleton (version 2, destructive migration)
- **`local/Mappers.kt`**: Entity-to-domain mapping
- **`local/CsvManager.kt`**: CSV export/import
- **`local/BackupSerializer.kt`**: JSON serialization for Drive backup
- **`classification/BankMessageParser.kt`**: Regex-based SMS parser
- **`classification/DefaultCategories.kt`**: 12 default categories + keyword map
- **`classification/TransactionClassifier.kt`**: High-level classification logic
- **`remote/DriveSyncManager.kt`**: Google Drive API v3 backup/restore
- **`repository/TransactionRepositoryImpl.kt`**: Repository implementation

## Dependency Injection

Manual DI via `AuraSpendApp` (Application class). All dependencies are instantiated once in `onCreate()` and exposed as lazy properties or accessed through the companion object:

```kotlin
class AuraSpendApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { TransactionRepositoryImpl(database, ...) }
    val classifier by lazy { TransactionClassifier(BankMessageParser(), DefaultCategories()) }
    // ...
}
```

This avoids Hilt/Dagger/Koin overhead. For a single-module app this is manageable, but a multi-module migration would benefit from Hilt.

## Navigation

Jetpack Navigation Compose (`NavGraph.kt`) with these routes:

```
splash → onboarding → main (bottom nav container)
                         ├── home (Dashboard)
                         ├── transactions (TransactionList)
                         ├── analytics (Analytics)
                         └── settings (Settings)
                         └── [FAB opens bottom sheet]
                               ├── "Smart Add" → classification
                               └── "Manual"    → add_transaction
```

Additional push routes: `budgets`, `subscriptions`, `categories`, `premium_upgrade`

## Theme System (`com.awbuilds.auraspend.ui.theme`)

- **Color.kt**: Three complete palettes — Light, Dark, AMOLED (true black)
- **Theme.kt**: Material 3 theming with dynamic color support (Android 12+)
- Theme mode persisted in `SharedPreferences`
- Palette: Teal-based primary (`0xFF00897B`), warm accent secondary
