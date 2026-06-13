package com.awbuilds.auraspend.ui.navigation

import android.content.Context
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.awbuilds.auraspend.AuraSpendApp
import com.awbuilds.auraspend.data.local.BackupSerializer
import com.awbuilds.auraspend.data.local.CsvManager
import com.awbuilds.auraspend.domain.model.Transaction
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
import com.awbuilds.auraspend.ui.core.AuraSpendScaffold
import com.awbuilds.auraspend.ui.home.DashboardScreen
import com.awbuilds.auraspend.ui.home.DashboardViewModel
import com.awbuilds.auraspend.ui.onboarding.OnboardingScreen
import com.awbuilds.auraspend.ui.recurring.RecurringScreen
import com.awbuilds.auraspend.ui.settings.SettingsScreen
import com.awbuilds.auraspend.ui.splash.SplashScreen
import com.awbuilds.auraspend.ui.theme.AppThemeMode
import com.awbuilds.auraspend.ui.transaction.AddTransactionScreen
import com.awbuilds.auraspend.ui.transaction.TransactionListScreen
import kotlinx.coroutines.launch

object Screen {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val ADD_TRANSACTION = "add_transaction"
    const val CLASSIFICATION = "classification"
    const val BUDGETS = "budgets"
    const val SUBSCRIPTIONS = "subscriptions"
    const val CATEGORIES = "categories"

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
    onThemeChanged: (AppThemeMode) -> Unit = {},
    dynamicColor: Boolean = true,
    onDynamicColorChanged: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()
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
                val app = context.applicationContext as AuraSpendApp
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
                                val backupData = BackupSerializer.deserialize(json)
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
                    onThemeChanged = onThemeChanged,
                    dynamicColor = dynamicColor,
                    onDynamicColorChanged = onDynamicColorChanged
                )
            }

            composable(Screen.CLASSIFICATION) {
                val context = LocalContext.current
                val categories by repository.getAllCategories()
                    .collectAsState(initial = emptyList())
                val viewModel = remember {
                    ClassificationViewModel(
                        classifyMessageUseCase = ClassifyMessageUseCase(),
                        saveTransactionUseCase = SaveTransactionUseCase(repository),
                        context = context
                    )
                }
                LaunchedEffect(categories) {
                    if (categories.isNotEmpty()) {
                        viewModel.handleIntent(
                            ClassificationViewIntent.SetCategories(categories)
                        )
                    }
                }
                ClassificationScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.BUDGETS) {
                val viewModel = remember { BudgetViewModel(repository) }
                BudgetScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SUBSCRIPTIONS) {
                RecurringScreen(
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
                                Transaction(
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
    onThemeChanged: (AppThemeMode) -> Unit = {},
    dynamicColor: Boolean = true,
    onDynamicColorChanged: (Boolean) -> Unit = {}
) {
    val dashboardViewModel = remember { DashboardViewModel(repository) }

    val transactions by repository.getAllTransactions().collectAsState(initial = emptyList())
    val categories by repository.getAllCategories().collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(Screen.HOME) }

    val context = LocalContext.current
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val file = CsvManager.exportToCsv(context, transactions)
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
                val imported = CsvManager.importFromCsv(context, uri)
                repository.saveTransactions(imported)
            }
        }
    }

    var showAddSheet by remember { mutableStateOf(false) }

    AuraSpendScaffold(
        currentRoute = currentTab,
        onNavigate = { currentTab = it },
        onAddClick = { showAddSheet = true }
    ) {
        when (currentTab) {
            Screen.HOME -> DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToTransactions = { currentTab = Screen.TRANSACTIONS },
                onNavigateToAdd = { showAddSheet = true },
                onNavigateToAnalytics = { currentTab = Screen.ANALYTICS }
            )
            Screen.TRANSACTIONS -> TransactionListScreen(
                transactions = transactions,
                categories = categories,
                onSearch = { },
                onDelete = { id -> scope.launch { repository.deleteTransaction(id) } },
                onBack = { currentTab = Screen.HOME }
            )
            Screen.ANALYTICS -> AnalyticsScreen(
                transactions = transactions,
                categories = categories,
                onBack = { currentTab = Screen.HOME }
            )
            Screen.SETTINGS -> SettingsScreen(
                currentTheme = themeMode,
                onThemeChanged = onThemeChanged,
                dynamicColor = dynamicColor,
                onDynamicColorChanged = onDynamicColorChanged,
                onBack = { currentTab = Screen.HOME },
                onExportCsv = { csvLauncher.launch("AuraSpend_export.csv") },
                onImportCsv = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values")) },
                onManageCategories = { navController.navigate(Screen.CATEGORIES) },
                onManageSubscriptions = { navController.navigate(Screen.SUBSCRIPTIONS) },
                onManageBudgets = { navController.navigate(Screen.BUDGETS) }
            )
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            onSmartAdd = {
                showAddSheet = false
                navController.navigate(Screen.CLASSIFICATION)
            },
            onManualAdd = {
                showAddSheet = false
                navController.navigate(Screen.ADD_TRANSACTION)
            },
            onDismiss = { showAddSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheet(
    onSmartAdd: () -> Unit,
    onManualAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Add Transaction",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    onClick = onSmartAdd,
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    SheetOptionContent(
                        icon = Icons.Default.AutoAwesome,
                        title = "Smart Add",
                        subtitle = "SMS / Paste",
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Card(
                    onClick = onManualAdd,
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    SheetOptionContent(
                        icon = Icons.Default.EditNote,
                        title = "Manual",
                        subtitle = "Enter details",
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetOptionContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp), tint = contentColor)
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = contentColor)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
    }
}
