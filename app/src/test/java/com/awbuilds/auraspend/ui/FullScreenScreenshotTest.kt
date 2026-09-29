package com.awbuilds.auraspend.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.awbuilds.auraspend.TestTransactionRepositoryDefaults
import com.awbuilds.auraspend.domain.model.Budget
import com.awbuilds.auraspend.domain.model.BudgetPeriod
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.RecurrenceFrequency
import com.awbuilds.auraspend.domain.model.Subscription
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.ui.analytics.AnalyticsScreen
import com.awbuilds.auraspend.ui.budget.BudgetScreen
import com.awbuilds.auraspend.ui.budget.BudgetViewModel
import com.awbuilds.auraspend.ui.home.DashboardScreen
import com.awbuilds.auraspend.ui.plan.PlanHubScreen
import com.awbuilds.auraspend.ui.savings.SavingsGoalsScreen
import com.awbuilds.auraspend.ui.settings.SettingsScreen
import com.awbuilds.auraspend.ui.home.DashboardViewIntent
import com.awbuilds.auraspend.ui.home.DashboardViewModel
import com.awbuilds.auraspend.ui.theme.AppThemeMode
import com.awbuilds.auraspend.ui.theme.AuraSpendTheme
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Whole-screen visual regression.
 *
 * The previous suite rendered isolated components, so nothing caught a screen that
 * laid out badly, clipped its own chrome or broke in a non-default theme. These
 * drive the real `DashboardScreen` end-to-end against a deterministic repository,
 * which is the screen a user opens most and the one with the most surface area.
 *
 * Amounts are fixed (not `now()`-derived) wherever the render would otherwise
 * drift between runs.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-420dpi",
    application = com.awbuilds.auraspend.TestApplication::class
)
class FullScreenScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun dashboardLight() = captureDashboard(AppThemeMode.LIGHT, "dashboard_light")

    @Test
    fun dashboardDark() = captureDashboard(AppThemeMode.DARK, "dashboard_dark")

    @Test
    fun dashboardAmoled() = captureDashboard(AppThemeMode.AMOLED, "dashboard_amoled")

    // ── Additional screens, rendered in the default (light) theme for review ──

    @Test
    fun analyticsScreen() = captureSimple("analytics", AppThemeMode.LIGHT) {
        AnalyticsScreen(
            transactions = SampleData.transactions,
            categories = SampleData.categories,
            onBack = {}
        )
    }

    @Test
    fun planHubScreen() = captureSimple("plan", AppThemeMode.LIGHT) {
        PlanHubScreen(
            repository = FixtureRepository(),
            onOpenBudgets = {}, onOpenSubscriptions = {}, onOpenGoals = {},
            onOpenCategories = {}
        )
    }

    @Test
    fun budgetScreen() {
        // Built outside the composable: constructing a ViewModel inside composition
        // is exactly what lint's ViewModelConstructorInComposable forbids.
        val viewModel = BudgetViewModel(FixtureRepository())
        captureSimple("budget", AppThemeMode.LIGHT) {
            BudgetScreen(viewModel = viewModel, onBack = {})
        }
    }

    @Test
    fun goalsScreen() = captureSimple("goals", AppThemeMode.LIGHT) {
        SavingsGoalsScreen(repository = FixtureRepository(), onBack = {})
    }

    @Test
    fun settingsScreen() = captureSimple("settings", AppThemeMode.LIGHT) {
        SettingsScreen(
            currentTheme = AppThemeMode.SYSTEM,
            onThemeChanged = {},
            onBack = {}
        )
    }

    private fun captureSimple(
        name: String,
        mode: AppThemeMode,
        content: @androidx.compose.runtime.Composable () -> Unit
    ) {
        compose.setContent {
            AuraSpendTheme(themeMode = mode, dynamicColor = false) { content() }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/inspect/$name.png")
    }

    private fun captureDashboard(mode: AppThemeMode, name: String) {
        val repository = FixtureRepository()
        val viewModel = DashboardViewModel(repository)

        compose.setContent {
            AuraSpendTheme(themeMode = mode, dynamicColor = false) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = {},
                    onQuickAdd = {},
                    onOpenSmartAdd = {},
                    onNavigateToAnalytics = {},
                    onNavigateToPlan = {},
                    onOpenSettings = {},
                    onOpenTransaction = {}
                )
            }
        }

        // The dashboard reacts to a Load intent in a LaunchedEffect; give the fake
        // repository's flows a chance to settle so the capture is the loaded state,
        // not a permanent skeleton.
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()

        compose.onRoot().captureRoboImage("src/test/screenshots/screen_$name.png")
    }
}

/**
 * Deterministic in-memory repository for screenshot tests.
 *
 * Everything is derived from a fixed base date so the rendered PNG is stable
 * between machines and between runs — a drifting baseline is worse than none.
 */
/** Sample data shared with the simple screens, which take plain lists. */
private object SampleData {
    val categories = listOf(
        Category(id = "cat_food", name = "Food & dining", icon = "restaurant", color = 0xFFFF7043.toInt()),
        Category(id = "cat_transport", name = "Transport", icon = "directions_car", color = 0xFF42A5F5.toInt()),
        Category(id = "cat_subs", name = "Subscriptions", icon = "subscriptions", color = 0xFFAB47BC.toInt()),
        Category(id = "cat_salary", name = "Salary", icon = "account_balance", color = 0xFF26A69A.toInt())
    )

    private val base = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0)

    val transactions = listOf(
        Transaction(
            id = "t1", amount = 1_250.00, categoryId = "cat_food", note = "Weekly groceries",
            merchant = "BigBasket", date = base, type = TransactionType.EXPENSE
        ),
        Transaction(
            id = "t2", amount = 320.00, categoryId = "cat_transport", note = "Cab",
            merchant = "Uber", date = base.minusDays(1), type = TransactionType.EXPENSE
        ),
        Transaction(
            id = "t3", amount = 649.00, categoryId = "cat_subs", note = "Netflix",
            merchant = "Netflix", date = base.minusDays(2), type = TransactionType.EXPENSE,
            isRecurring = true, recurrenceFrequency = RecurrenceFrequency.MONTHLY
        ),
        Transaction(
            id = "t4", amount = 85_000.00, categoryId = "cat_salary", note = "Salary credit",
            merchant = "HDFC Bank", date = base.minusDays(3), type = TransactionType.INCOME
        ),
        Transaction(
            id = "t5", amount = 1_899.00, categoryId = "cat_food", note = "Dinner",
            merchant = "Truffles", date = base.minusDays(4), type = TransactionType.EXPENSE
        ),
        Transaction(
            id = "t6", amount = 2_400.00, categoryId = "cat_food", note = "Groceries",
            merchant = "Dmart", date = base.minusDays(5), type = TransactionType.EXPENSE
        )
    )
}

private class FixtureRepository(
    private val withGoals: Boolean = true
) : TestTransactionRepositoryDefaults {

    // Fixed, NOT now(): the committed dashboard baselines must not drift between
    // runs, or CI's Roborazzi diff fails for reasons unrelated to any change.
    private val base = LocalDateTime.of(2026, 9, 15, 12, 0)

    private val categories = listOf(
        Category(id = "cat_food", name = "Food & dining", icon = "restaurant", color = 0xFFFF7043.toInt()),
        Category(id = "cat_transport", name = "Transport", icon = "directions_car", color = 0xFF42A5F5.toInt()),
        Category(id = "cat_subs", name = "Subscriptions", icon = "subscriptions", color = 0xFFAB47BC.toInt()),
        Category(id = "cat_salary", name = "Salary", icon = "account_balance", color = 0xFF26A69A.toInt())
    )

    private val transactions = listOf(
        Transaction(
            id = "t1", amount = 1_250.00, categoryId = "cat_food", note = "Weekly groceries",
            merchant = "BigBasket", date = base, type = TransactionType.EXPENSE
        ),
        Transaction(
            id = "t2", amount = 320.00, categoryId = "cat_transport", note = "Cab",
            merchant = "Uber", date = base.minusDays(1), type = TransactionType.EXPENSE
        ),
        Transaction(
            id = "t3", amount = 649.00, categoryId = "cat_subs", note = "Netflix",
            merchant = "Netflix", date = base.minusDays(2), type = TransactionType.EXPENSE,
            isRecurring = true, recurrenceFrequency = RecurrenceFrequency.MONTHLY
        ),
        Transaction(
            id = "t4", amount = 85_000.00, categoryId = "cat_salary", note = "Salary credit",
            merchant = "HDFC Bank", date = base.minusDays(3), type = TransactionType.INCOME
        ),
        Transaction(
            id = "t5", amount = 1_899.00, categoryId = "cat_food", note = "Dinner",
            merchant = "Truffles", date = base.minusDays(4), type = TransactionType.EXPENSE
        )
    )

    private val budgets = listOf(
        Budget(id = "b1", categoryId = "cat_food", limitAmount = 8_000.0, spentAmount = 4_280.5, period = BudgetPeriod.MONTHLY),
        Budget(id = "b2", categoryId = "cat_transport", limitAmount = 3_000.0, spentAmount = 1_150.0, period = BudgetPeriod.MONTHLY)
    )

    private val subscriptions = listOf(
        Subscription(
            id = "s1", name = "Netflix", amount = 649.0, categoryId = "cat_subs",
            billingCycle = RecurrenceFrequency.MONTHLY, nextBillingDate = base.plusDays(6)
        ),
        Subscription(
            id = "s2", name = "Spotify", amount = 119.0, categoryId = "cat_subs",
            billingCycle = RecurrenceFrequency.MONTHLY, nextBillingDate = base.plusDays(12)
        )
    )

    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(transactions)
    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(transactions.take(limit))
    override fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>> = flowOf(transactions)
    override fun searchTransactions(query: String): Flow<List<Transaction>> = flowOf(transactions)
    override fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> =
        flowOf(transactions.filter { it.categoryId == categoryId })
    override fun getRecurringTransactions(): Flow<List<Transaction>> =
        flowOf(transactions.filter { it.isRecurring })

    override fun getAllCategories(): Flow<List<Category>> = flowOf(categories)
    override fun getAllBudgets(): Flow<List<Budget>> = flowOf(budgets)
    override fun getActiveSubscriptions(): Flow<List<Subscription>> = flowOf(subscriptions)

    override suspend fun saveTransaction(transaction: Transaction) = Unit
    override suspend fun saveTransactions(transactions: List<Transaction>) = Unit
    override suspend fun deleteTransaction(id: String) = Unit
    override suspend fun getCategoryById(id: String): Category? = categories.find { it.id == id }
    override suspend fun saveCategory(category: Category) = Unit
    override suspend fun saveCategories(categories: List<Category>) = Unit
    override suspend fun deleteCategory(id: String) = Unit
    override suspend fun getBudgetByCategory(categoryId: String): Budget? = budgets.find { it.categoryId == categoryId }
    override suspend fun saveBudget(budget: Budget) = Unit
    override suspend fun updateBudgetSpent(categoryId: String, spent: Double) = Unit
    override suspend fun deleteBudget(budgetId: String) = Unit
    private val goals = listOf(
        com.awbuilds.auraspend.domain.model.SavingsGoal(
            id = "g1", name = "New laptop", targetAmount = 150_000.0, currentAmount = 62_500.0
        ),
        com.awbuilds.auraspend.domain.model.SavingsGoal(
            id = "g2", name = "Japan trip", targetAmount = 200_000.0, currentAmount = 200_000.0
        )
    )

    override fun getAllSavingsGoals(): Flow<List<com.awbuilds.auraspend.domain.model.SavingsGoal>> =
        flowOf(goals)
    override suspend fun saveSavingsGoal(goal: com.awbuilds.auraspend.domain.model.SavingsGoal) = Unit
    override suspend fun deleteSavingsGoal(goalId: String) = Unit
    override suspend fun updateSavingsGoal(goal: com.awbuilds.auraspend.domain.model.SavingsGoal) = Unit
    override suspend fun saveSubscription(subscription: Subscription) = Unit
    override suspend fun deleteSubscription(id: String, active: Boolean) = Unit
}
