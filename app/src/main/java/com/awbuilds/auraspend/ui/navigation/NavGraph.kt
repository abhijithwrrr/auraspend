package com.awbuilds.auraspend.ui.navigation

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.awbuilds.auraspend.AuraSpendApp
import com.awbuilds.auraspend.R
import com.awbuilds.auraspend.data.ai.ModelDownloadManager
import com.awbuilds.auraspend.data.classification.AutoClassificationWorker
import com.awbuilds.auraspend.data.classification.AutoDetect
import com.awbuilds.auraspend.data.classification.DuplicateDetector
import com.awbuilds.auraspend.core.boundaryOrNull
import com.awbuilds.auraspend.data.local.BackupSerializer
import com.awbuilds.auraspend.data.local.CsvManager
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.domain.repository.TransactionRepository
import com.awbuilds.auraspend.domain.usecase.ClassifyMessageUseCase
import com.awbuilds.auraspend.domain.usecase.SaveTransactionUseCase
import com.awbuilds.auraspend.ui.analytics.AnalyticsScreen
import com.awbuilds.auraspend.ui.budget.BudgetScreen
import com.awbuilds.auraspend.ui.budget.BudgetViewModel
import com.awbuilds.auraspend.ui.category.CategoryManagementScreen
import com.awbuilds.auraspend.ui.classification.ClassificationScreen
import com.awbuilds.auraspend.ui.classification.ClassificationViewIntent
import com.awbuilds.auraspend.ui.classification.ClassificationViewModel
import com.awbuilds.auraspend.ui.core.AuraAppChrome
import com.awbuilds.auraspend.ui.core.isNotificationPermissionNeeded
import com.awbuilds.auraspend.ui.core.rememberNotificationPermissionLauncher
import com.awbuilds.auraspend.ui.home.DashboardScreen
import com.awbuilds.auraspend.ui.home.DashboardViewModel
import com.awbuilds.auraspend.ui.onboarding.OnboardingScreen
import com.awbuilds.auraspend.ui.plan.PlanHubScreen
import com.awbuilds.auraspend.ui.recurring.RecurringScreen
import com.awbuilds.auraspend.ui.settings.SettingsScreen
import com.awbuilds.auraspend.ui.splash.SplashScreen
import com.awbuilds.auraspend.ui.theme.AppThemeMode
import com.awbuilds.auraspend.ui.transaction.NewTransactionScreen
import com.awbuilds.auraspend.ui.transaction.QuickAddSheet
import com.awbuilds.auraspend.ui.transaction.TransactionDetailScreen
import com.awbuilds.auraspend.ui.transaction.TransactionListScreen
import com.awbuilds.auraspend.ui.transaction.TransactionListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** All navigation routes in one place. */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"

    // Top-level destinations (chrome visible).
    const val HOME = "home"
    const val ACTIVITY = "activity"
    const val PLAN = "plan"
    const val INSIGHTS = "insights"

    // Pushed destinations.
    const val SETTINGS = "settings"
    const val ADD_TRANSACTION = "add_transaction"
    const val CLASSIFICATION = "classification"
    const val BUDGETS = "budgets"
    const val SUBSCRIPTIONS = "subscriptions"
    const val CATEGORIES = "categories"
    const val GOALS = "goals"
    const val TRANSACTION_DETAIL = "transaction/{transactionId}"

    fun transactionDetail(id: String) = "transaction/$id"

    val topLevel = setOf(HOME, ACTIVITY, PLAN, INSIGHTS)
}

/**
 * Builds a [ViewModelProvider.Factory] for a ViewModel that takes constructor arguments.
 *
 * Screens used to call `remember { DashboardViewModel(repository) }` directly, which meant the
 * ViewModel was *not* retained across configuration change: every rotation reset filters,
 * paging position and in-flight work, `onCleared()` never ran, and the internal coroutine
 * scope leaked for the life of the process. `viewModel(factory = ...)` scopes the instance to
 * the NavBackStackEntry, so state survives rotation and is cleared when the entry leaves
 * the back stack.
 */
private inline fun <reified VM : ViewModel> factoryOf(
    crossinline create: () -> VM
): ViewModelProvider.Factory = viewModelFactory {
    initializer { create() }
}

/** Premium-feeling screen transition: subtle horizontal push + fade. */
private const val TRANSITION_MS = 260
private val pushEnter: EnterTransition =
    fadeIn(tween(TRANSITION_MS)) + slideInHorizontally(tween(TRANSITION_MS)) { it / 8 }

private val pushExit: ExitTransition = fadeOut(tween(TRANSITION_MS / 2))

private val popEnter: EnterTransition = fadeIn(tween(TRANSITION_MS))

private val popExit: ExitTransition =
    fadeOut(tween(TRANSITION_MS)) + slideOutHorizontally(tween(TRANSITION_MS)) { it / 8 }

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AuraSpendNavHost(
    repository: TransactionRepository,
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    onThemeChanged: (AppThemeMode) -> Unit = {},
    dynamicColor: Boolean = false,
    onDynamicColorChanged: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("auraspend_prefs", Context.MODE_PRIVATE)
    val startOnboarding = remember { !prefs.getBoolean("onboarding_completed", false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute in Routes.topLevel

    var showQuickAdd by remember { mutableStateOf(false) }
    var quickAddType by remember { mutableStateOf(TransactionType.EXPENSE) }
    val repositoryScope = rememberCoroutineScope()

    AuraAppChrome(
        showChrome = showChrome,
        currentRoute = currentRoute,
        onNavigate = { navController.navigateToTab(it) },
        onAddClick = {
            quickAddType = TransactionType.EXPENSE
            showQuickAdd = true
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) }
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onAnimationFinished = {
                        val destination = if (startOnboarding) Routes.ONBOARDING else Routes.HOME
                        navController.navigate(destination) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.ONBOARDING) {
                OnboardingFlow(
                    repository = repository,
                    onFinished = {
                        prefs.edit().putBoolean("onboarding_completed", true).apply()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            // ── Top-level destinations ────────────────────────────────────────

            composable(Routes.HOME) {
                val dashboardViewModel = viewModel<DashboardViewModel>(
                    factory = factoryOf { DashboardViewModel(repository) }
                )
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToTransactions = { navController.navigateToTab(Routes.ACTIVITY) },
                    onQuickAdd = { type ->
                        quickAddType = type
                        showQuickAdd = true
                    },
                    onOpenSmartAdd = { navController.navigate(Routes.CLASSIFICATION) },
                    onNavigateToAnalytics = { navController.navigateToTab(Routes.INSIGHTS) },
                    onNavigateToPlan = { navController.navigateToTab(Routes.PLAN) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenTransaction = { id -> navController.navigate(Routes.transactionDetail(id)) }
                )
            }

            composable(Routes.ACTIVITY) {
                val viewModel = viewModel<TransactionListViewModel>(
                    factory = factoryOf { TransactionListViewModel(repository) }
                )
                val categories by repository.getAllCategories()
                    .collectAsState(initial = emptyList())

                TransactionListScreen(
                    viewModel = viewModel,
                    categories = categories,
                    onOpenTransaction = { id -> navController.navigate(Routes.transactionDetail(id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.PLAN) {
                PlanHubScreen(
                    repository = repository,
                    onOpenBudgets = { navController.navigate(Routes.BUDGETS) },
                    onOpenSubscriptions = { navController.navigate(Routes.SUBSCRIPTIONS) },
                    onOpenGoals = { navController.navigate(Routes.GOALS) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.INSIGHTS) {
                val transactions by repository.getAllTransactions()
                    .collectAsState(initial = emptyList())
                val categories by repository.getAllCategories()
                    .collectAsState(initial = emptyList())
                AnalyticsScreen(
                    transactions = transactions,
                    categories = categories,
                    onBack = { navController.navigateToTab(Routes.HOME) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            // ── Pushed destinations ───────────────────────────────────────────

            composable(
                route = Routes.SETTINGS,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit }
            ) {
                SettingsDestination(
                    repository = repository,
                    navController = navController,
                    themeMode = themeMode,
                    onThemeChanged = onThemeChanged,
                    dynamicColor = dynamicColor,
                    onDynamicColorChanged = onDynamicColorChanged
                )
            }

            composable(
                route = Routes.CLASSIFICATION,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit }
            ) {
                val categories by repository.getAllCategories()
                    .collectAsState(initial = emptyList())
                val viewModel = viewModel<ClassificationViewModel>(
                    factory = factoryOf {
                        ClassificationViewModel(
                            classifyMessageUseCase = ClassifyMessageUseCase(),
                            saveTransactionUseCase = SaveTransactionUseCase(repository),
                            // applicationContext, not the Activity: this ViewModel is now
                            // retained across configuration changes, so holding the Activity
                            // would leak it for as long as the screen is in the back stack.
                            context = context.applicationContext
                        )
                    }
                )
                LaunchedEffect(categories) {
                    if (categories.isNotEmpty()) {
                        viewModel.handleIntent(ClassificationViewIntent.SetCategories(categories))
                    }
                }
                ClassificationScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.BUDGETS,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit }
            ) {
                val viewModel = viewModel<BudgetViewModel>(
                    factory = factoryOf { BudgetViewModel(repository) }
                )
                BudgetScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.SUBSCRIPTIONS,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit }
            ) {
                RecurringScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.GOALS,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit }
            ) {
                com.awbuilds.auraspend.ui.savings.SavingsGoalsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.ADD_TRANSACTION,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit }
            ) {
                NewTransactionScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.TRANSACTION_DETAIL,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit }
            ) { entry ->
                val transactionId = entry.arguments?.getString("transactionId") ?: return@composable
                TransactionDetailScreen(
                    transactionId = transactionId,
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onOpenTransaction = { id -> navController.navigate(Routes.transactionDetail(id)) }
                )
            }

            composable(
                route = Routes.CATEGORIES,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit }
            ) {
                val scope = rememberCoroutineScope()
                val categories by repository.getAllCategories()
                    .collectAsState(initial = emptyList())
                CategoryManagementScreen(
                    categories = categories,
                    onSaveCategory = { category ->
                        scope.launch { repository.saveCategory(category) }
                    },
                    onDeleteCategory = { id ->
                        scope.launch { repository.deleteCategory(id) }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (showQuickAdd) {
        val categories by repository.getAllCategories().collectAsState(initial = emptyList())
        QuickAddSheet(
            categories = categories,
            initialType = quickAddType,
            onSave = { amount, categoryId, type, merchant, note ->
                showQuickAdd = false
                repositoryScope.launch {
                    repository.saveTransaction(
                        Transaction(
                            amount = amount,
                            categoryId = categoryId,
                            note = note,
                            merchant = merchant,
                            date = java.time.LocalDateTime.now(),
                            type = type
                        )
                    )
                }
            },
            onSmartAdd = {
                showQuickAdd = false
                navController.navigate(Routes.CLASSIFICATION)
            },
            onManualAdd = {
                showQuickAdd = false
                navController.navigate(Routes.ADD_TRANSACTION)
            },
            onDismiss = { showQuickAdd = false }
        )
    }
}

/** Onboarding + optional Google Drive restore (unchanged behavior, new route). */
@Composable
private fun OnboardingFlow(
    repository: TransactionRepository,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as AuraSpendApp
    val driveSyncManager = app.driveSyncManager
    val prefs = context.getSharedPreferences("auraspend_prefs", Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()

    // Resolved in composition so the text tracks configuration changes
    // (and the locale) rather than being read off LocalContext later.
    val signInFailedMessage = stringResource(R.string.onboarding_restore_signin_failed)
    val noBackupMessage = stringResource(R.string.onboarding_restore_no_backup)
    val restoreFailedMessage = stringResource(R.string.onboarding_restore_failed)

    var isRestoring by remember { mutableStateOf(false) }
    var restoreError by remember { mutableStateOf<String?>(null) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            val signedIn = driveSyncManager.handleSignInResult(result.data)
            if (!signedIn) {
                restoreError = signInFailedMessage
                return@launch
            }

            isRestoring = true
            // Every step below is a boundary: a malformed backup, a revoked Drive
            // grant or a full disk must leave the user on onboarding with a message,
            // never a crash. BackupRestoreManager applies the whole replace inside a
            // single Room transaction, so a failure leaves the database untouched.
            val errorMessage: String? = run {
                val json = boundaryOrNull("DriveRestore") { driveSyncManager.restoreLocalData() }
                when {
                    json == null -> noBackupMessage
                    else -> {
                        val backup = boundaryOrNull("DriveRestore") { BackupSerializer.deserialize(json) }
                        val summary = backup?.let { app.backupRestoreManager.restore(it) }
                        when {
                            summary == null -> restoreFailedMessage
                            else -> {
                                prefs.edit().putBoolean("onboarding_completed", true).apply()
                                null
                            }
                        }
                    }
                }
            }
            isRestoring = false
            if (errorMessage != null) {
                restoreError = errorMessage
            } else {
                onFinished()
            }
        }
    }

    OnboardingScreen(
        onFinished = onFinished,
        onRestoreFromDrive = { signInLauncher.launch(driveSyncManager.getSignInIntent()) },
        isRestoring = isRestoring,
        restoreError = restoreError,
        onRestoreErrorDismissed = { restoreError = null }
    )
}

/** Settings keeps its own side effects (CSV, AI model, auto-detect) here. */
@Composable
private fun SettingsDestination(
    repository: TransactionRepository,
    navController: NavHostController,
    themeMode: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transactions by repository.getAllTransactions().collectAsState(initial = emptyList())
    val aiModelState by ModelDownloadManager.state.collectAsState()
    val notificationPermissionLauncher = rememberNotificationPermissionLauncher()
    val snackbarHostState = remember { SnackbarHostState() }

    // Message templates are resolved in composition (so they follow the locale) and
    // formatted with the runtime counts inside the coroutine.
    val exportSuccessTemplate = stringResource(R.string.csv_export_success)
    val exportFailedMessage = stringResource(R.string.csv_export_failed)
    val importFailedMessage = stringResource(R.string.csv_import_failed)
    val importAllDuplicatesMessage = stringResource(R.string.csv_import_all_duplicates)
    val importPartialTemplate = stringResource(R.string.csv_import_partial)
    val importSuccessTemplate = stringResource(R.string.csv_import_success)

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                // Written straight to the SAF uri. The previous version staged the file
                // in the public Downloads directory first, which cannot succeed on
                // API 30+ and threw out of this unguarded coroutine.
                val rows = withContext(Dispatchers.IO) {
                    CsvManager.exportToCsv(context, transactions, uri)
                }
                val message = if (rows >= 0) {
                    String.format(exportSuccessTemplate, rows)
                } else {
                    exportFailedMessage
                }
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) { CsvManager.importFromCsv(context, uri) }
                // Re-importing the same file used to duplicate every row: the
                // duplicate detector was never consulted on this path.
                val fresh = result.imported.filter { incoming ->
                    DuplicateDetector.detectDuplicates(incoming, transactions).isEmpty()
                }
                if (fresh.isNotEmpty()) {
                    repository.saveTransactions(fresh)
                }
                val message = when {
                    result.imported.isEmpty() -> importFailedMessage
                    fresh.isEmpty() -> importAllDuplicatesMessage
                    result.skippedRows > 0 ->
                        String.format(importPartialTemplate, fresh.size, result.skippedRows)
                    else -> String.format(importSuccessTemplate, fresh.size)
                }
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    SettingsScreen(
        currentTheme = themeMode,
        onThemeChanged = onThemeChanged,
        dynamicColor = dynamicColor,
        onDynamicColorChanged = onDynamicColorChanged,
        snackbarHostState = snackbarHostState,
        onBack = { navController.popBackStack() },
        onExportCsv = { csvLauncher.launch("AuraSpend_export.csv") },
        onImportCsv = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values")) },
        onManageCategories = { navController.navigate(Routes.CATEGORIES) },
        onManageSubscriptions = { navController.navigate(Routes.SUBSCRIPTIONS) },
        onManageBudgets = { navController.navigate(Routes.BUDGETS) },
        aiModelState = aiModelState,
        onDownloadModel = {
            // Downloading from Settings is itself the consent.
            ModelDownloadManager.markConsentGiven(context)
            if (isNotificationPermissionNeeded(context)) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            ModelDownloadManager.start(context)
        },
        onCancelModelDownload = { ModelDownloadManager.cancel() },
        onDeleteModel = { ModelDownloadManager.deleteModel(context) },
        autoDetectEnabled = AutoDetect.isEnabled(context),
        onAutoDetectChanged = { enabled ->
            AutoDetect.setEnabled(context, enabled)
            if (enabled) {
                if (isNotificationPermissionNeeded(context)) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                scope.launch(Dispatchers.IO) { AutoClassificationWorker.runNow(context) }
            } else {
                scope.launch(Dispatchers.IO) { AutoClassificationWorker.cancelPendingWork(context) }
            }
        }
    )
}
