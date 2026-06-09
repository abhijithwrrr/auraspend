# AuraSpend Development Guide

## Prerequisites

- **Android Studio** Koala or newer (2024.1+)
- **JDK** 17+
- **Android SDK** 36 (Android 16)
- **Gradle** 8.11.1 (bundled wrapper)

## Setup

```bash
git clone https://github.com/abhijithwrrr/auraspend.git
cd auraspend
cp secrets.properties.example secrets.properties
```

Edit `secrets.properties` with your Google API credentials:
- `WEB_CLIENT_ID` — Google OAuth 2.0 Web Client ID (for Drive sign-in)
- `DRIVE_API_KEY` — Google Drive API key (optional)

## Build & Run

### Build Flavors

| Flavor | Command | Behavior |
|--------|---------|----------|
| Free (development) | `./gradlew installFreeDebug` | BillingManager stub — all premium features unlocked |
| Play Store | `./gradlew installPlayDebug` | Real billing verification (TODO) |

### Build Types

| Type | Command |
|------|---------|
| Debug | `./gradlew assembleFreeDebug` |
| Release | `./gradlew assembleFreeRelease` (minify disabled) |

## Project Structure Conventions

### Adding a New Screen

1. Create a new package under `ui/` (e.g., `ui/reports/`)
2. If MVI needed, create `ReportsMvi.kt` with `ReportsViewState` and `ReportsViewIntent`
3. Create `ReportsViewModel.kt` extending ViewModel
4. Create `ReportsScreen.kt` as @Composable functions
5. Add route in `NavGraph.kt` and navigation call
6. Wire dependencies in `AuraSpendApp.kt`

### MVI Pattern

```kotlin
// Mvi file
data class ReportsViewState(
    val isLoading: Boolean = true,
    val data: List<Report> = emptyList()
)

sealed interface ReportsViewIntent {
    data object Load : ReportsViewIntent
    data class Filter(val period: String) : ReportsViewIntent
}

// ViewModel
class ReportsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ReportsViewState())
    val state: StateFlow<ReportsViewState> = _state.asStateFlow()

    fun onIntent(intent: ReportsViewIntent) {
        when (intent) {
            ReportsViewIntent.Load -> loadReports()
            is ReportsViewIntent.Filter -> applyFilter(intent.period)
        }
    }
}

// Screen
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Render UI from state
}
```

### Adding a Room Entity

1. Define entity data class in `data/local/entities/Entities.kt` (or new file)
2. Add DAO methods in `data/local/dao/Daos.kt` (or new file)
3. Register entity + DAO in `AppDatabase.kt`
4. Add domain model in `domain/model/Models.kt`
5. Add mapper in `data/local/Mappers.kt`
6. Update repository interface + impl
7. Wire in `AuraSpendApp.kt`

## Code Style

- **Package by feature**: Each feature gets its own package under `ui/`
- **No `lateinit var`**: Prefer constructor injection with `val`
- **StateFlow over LiveData**: All reactive state uses `StateFlow` + `collectAsStateWithLifecycle()`
- **No third-party DI**: Manual service locator in Application class
- **Compose previews**: Use `@Preview` with `uiMode` for light/dark
- **Canvas over libraries**: Charts are custom Canvas draw calls, not third-party libraries

## Testing

**Current status**: Test infrastructure is in place but no tests have been written.

```bash
# Unit tests (JUnit 4)
./gradlew test

# Instrumented tests (Espresso)
./gradlew connectedAndroidTest
```

Test files go in:
- `app/src/test/java/com/awbuilds/auraspend/` — Unit tests
- `app/src/androidTest/java/com/awbuilds/auraspend/` — Instrumented tests

## CI/CD

### GitHub Actions

- **CI** (`.github/workflows/ci.yml`): Runs on push to `main`, builds both flavors
- **PR Check** (`.github/workflows/pr_check.yml`): On PRs to `main` touching Kotlin/gradle files
- **Release Drafter** (`.github/workflows/release-drafter.yml`): Maintains a draft release with auto-changelog

Dependabot runs weekly for Gradle dependencies and monthly for GitHub Actions.

## Privacy & Data

AuraSpend processes bank SMS locally on-device. No SMS data is sent to remote servers. Google Drive backup stores encrypted transaction data in the user's own Drive account (OAuth 2.0).
