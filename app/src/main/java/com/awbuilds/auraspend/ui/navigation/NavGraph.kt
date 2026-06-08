package com.awbuilds.auraspend.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.Context
import kotlinx.coroutines.launch
import com.awbuilds.auraspend.ui.analytics.AnalyticsScreen
import com.awbuilds.auraspend.ui.classification.ClassificationScreen
import com.awbuilds.auraspend.ui.classification.ClassificationViewModel
import com.awbuilds.auraspend.ui.core.AuraSpendScaffold
import com.awbuilds.auraspend.ui.home.DashboardScreen
import com.awbuilds.auraspend.ui.home.DashboardViewModel
import com.awbuilds.auraspend.ui.onboarding.OnboardingScreen
import com.awbuilds.auraspend.ui.category.CategoryManagementScreen
import com.awbuilds.auraspend.ui.settings.SettingsScreen
import com.awbuilds.auraspend.ui.splash.SplashScreen
import com.awbuilds.auraspend.ui.theme.AppThemeMode
import com.awbuilds.auraspend.ui.transaction.AddTransactionScreen
import com.awbuilds.auraspend.ui.transaction.TransactionListScreen
import com.awbuilds.auraspend.domain.repository.TransactionRepository

object Screen {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val ADD_TRANSACTION = "add_transaction"
    const val CLASSIFICATION = "classification"
    const val BUDGETS = "budgets"
    const val SUBSCRIPTIONS = "subscriptions"
    const val CATEGORIES = "categories"

    // Bottom nav tab routes (relative to MainScreen)
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AuraSpendNavHost(
    repository: TransactionRepository,
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    onThemeChanged: (AppThemeMode) -> Unit = {}
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("auraspend_prefs", Context.MODE_PRIVATE)
    val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
    val startDestination = if (onboardingCompleted) Screen.MAIN else Screen.SPLASH

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Screen.SPLASH) {
                SplashScreen(
                    onAnimationFinished = {
                        navController.navigate(Screen.ONBOARDING) {
                            popUpTo(Screen.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.ONBOARDING) {
                val app = context.applicationContext as com.awbuilds.auraspend.AuraSpendApp
                val driveSyncManager = app.driveSyncManager
                val scope = rememberCoroutineScope()

                var isRestoring by remember { mutableStateOf(false) }
                var restoreError by remember { mutableStateOf<String?>(null) }

                val signInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    scope.launch {
                        val signedIn = driveSyncManager.handleSignInResult(result.data)
                        if (signedIn) {
                            isRestoring = true
                            val json = driveSyncManager.restoreLocalData()
                            if (json != null) {
                                val backupData = com.awbuilds.auraspend.data.local.BackupSerializer.deserialize(json)
                                repository.saveTransactions(backupData.transactions)
                                repository.saveCategories(backupData.categories)
                                backupData.budgets.forEach { repository.saveBudget(it) }
                                backupData.subscriptions.forEach { repository.saveSubscription(it) }
                                isRestoring = false
                                prefs.edit().putBoolean("onboarding_completed", true).apply()
                                navController.navigate(Screen.MAIN) {
                                    popUpTo(Screen.ONBOARDING) { inclusive = true }
                                }
                            } else {
                                isRestoring = false
                                restoreError = "No backup found or restore failed"
                            }
                        } else {
                            restoreError = "Sign-in failed"
                        }
                    }
                }

                OnboardingScreen(
                    onFinished = {
                        prefs.edit().putBoolean("onboarding_completed", true).apply()
                        navController.navigate(Screen.MAIN) {
                            popUpTo(Screen.ONBOARDING) { inclusive = true }
                        }
                    },
                    onRestoreFromDrive = {
                        signInLauncher.launch(driveSyncManager.getSignInIntent())
                    },
                    isRestoring = isRestoring,
                    restoreError = restoreError,
                    onRestoreErrorDismissed = { restoreError = null }
                )
            }

            composable(Screen.MAIN) {
                MainScreen(
                    repository = repository,
                    navController = navController,
                    themeMode = themeMode,
                    onThemeChanged = onThemeChanged
                )
            }

            composable(Screen.CLASSIFICATION) {
                val context = LocalContext.current
                val viewModel = remember {
                    ClassificationViewModel(
                        classifyMessageUseCase = com.awbuilds.auraspend.domain.usecase.ClassifyMessageUseCase(),
                        saveTransactionUseCase = com.awbuilds.auraspend.domain.usecase.SaveTransactionUseCase(repository),
                        context = context
                    )
                }
                ClassificationScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.BUDGETS) {
                val viewModel = remember { com.awbuilds.auraspend.ui.budget.BudgetViewModel(repository) }
                com.awbuilds.auraspend.ui.budget.BudgetScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SUBSCRIPTIONS) {
                com.awbuilds.auraspend.ui.recurring.RecurringScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ADD_TRANSACTION) {
                val categories by repository.getAllCategories()
                    .collectAsState(initial = emptyList())
                val scope = rememberCoroutineScope()
                AddTransactionScreen(
                    categories = categories,
                    onSave = { amount, categoryId, note, merchant, type, date ->
                        scope.launch {
                            repository.saveTransaction(
                                com.awbuilds.auraspend.domain.model.Transaction(
                                    amount = amount,
                                    categoryId = categoryId,
                                    note = note,
                                    merchant = merchant,
                                    date = date,
                                    type = type
                                )
                            )
                            navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.CATEGORIES) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    repository: TransactionRepository,
    navController: NavHostController,
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    onThemeChanged: (AppThemeMode) -> Unit = {}
) {
    val dashboardViewModel = remember {
        DashboardViewModel(repository)
    }

    val transactions by repository.getAllTransactions()
        .collectAsState(initial = emptyList())
    val categories by repository.getAllCategories()
        .collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(Screen.HOME) }

    val context = LocalContext.current
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val file = com.awbuilds.auraspend.data.local.CsvManager.exportToCsv(context, transactions)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val imported = com.awbuilds.auraspend.data.local.CsvManager.importFromCsv(context, uri)
                repository.saveTransactions(imported)
            }
        }
    }

    var showAddSheet by remember { mutableStateOf(false) }

    AuraSpendScaffold(
        currentRoute = currentTab,
        onNavigate = { route ->
            currentTab = route
        },
        onAddClick = {
            showAddSheet = true
        }
    ) {
        when (currentTab) {
            Screen.HOME -> {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToTransactions = { currentTab = Screen.TRANSACTIONS },
                    onNavigateToAdd = { showAddSheet = true },
                    onNavigateToAnalytics = { currentTab = Screen.ANALYTICS }
                )
            }
            Screen.TRANSACTIONS -> {
                TransactionListScreen(
                    transactions = transactions,
                    categories = categories,
                    onSearch = { },
                    onDelete = { id ->
                        scope.launch { repository.deleteTransaction(id) }
                    },
                    onBack = { currentTab = Screen.HOME }
                )
            }
            Screen.ANALYTICS -> {
                AnalyticsScreen(
                    transactions = transactions,
                    categories = categories,
                    onBack = { currentTab = Screen.HOME }
                )
            }
            Screen.SETTINGS -> {
                SettingsScreen(
                    currentTheme = themeMode,
                    onThemeChanged = onThemeChanged,
                    onBack = { currentTab = Screen.HOME },
                    onExportCsv = { csvLauncher.launch("AuraSpend_export.csv") },
                    onImportCsv = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values")) },
                    onManageCategories = { navController.navigate(Screen.CATEGORIES) },
                    onManageSubscriptions = { navController.navigate(Screen.SUBSCRIPTIONS) },
                    onManageBudgets = { navController.navigate(Screen.BUDGETS) }
                )
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Add Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedCard(
                        onClick = {
                            showAddSheet = false
                            navController.navigate(Screen.CLASSIFICATION)
                        },
                        modifier = Modifier.size(140.dp, 120.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Smart Add", style = MaterialTheme.typography.labelLarge)
                            Text("SMS / Paste", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedCard(
                        onClick = {
                            showAddSheet = false
                            navController.navigate(Screen.ADD_TRANSACTION)
                        },
                        modifier = Modifier.size(140.dp, 120.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Manual", style = MaterialTheme.typography.labelLarge)
                            Text("Enter details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
