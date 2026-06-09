package com.awbuilds.auraspend.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.ui.core.ShimmerCard
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.handleIntent(DashboardViewIntent.LoadDashboard)
    }

    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("AuraSpend", fontWeight = FontWeight.Bold)
                        Text(
                            "Your Finance Manager",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAnalytics) {
                        Icon(Icons.Default.Analytics, contentDescription = "Analytics")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            if (state.isLoading && state.recentTransactions.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(4) {
                        ShimmerCard()
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                                animationSpec = tween(400),
                                initialOffsetY = { it / 2 }
                            )
                        ) {
                            BalanceCard(state.totalBalance, state.monthlyIncome, state.monthlyExpense)
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(400, delayMillis = 100)) + slideInVertically(
                                animationSpec = tween(400, delayMillis = 100),
                                initialOffsetY = { it / 2 }
                            )
                        ) {
                            QuickStatsRow(
                                transactions = state.recentTransactions.size,
                                largestExpense = state.recentTransactions
                                    .filter { it.type == TransactionType.EXPENSE }
                                    .maxOfOrNull { it.amount } ?: 0.0,
                                subscriptions = state.totalSubscriptionCost
                            )
                        }
                    }

                    if (state.dailySpending.isNotEmpty()) {
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(400, delayMillis = 200)) + slideInVertically(
                                    animationSpec = tween(400, delayMillis = 200),
                                    initialOffsetY = { it / 2 }
                                )
                            ) {
                                WeeklyChartCard(dailySpending = state.dailySpending)
                            }
                        }
                    }

                    if (state.budgets.isNotEmpty()) {
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(400, delayMillis = 300))
                            ) {
                                Text(
                                    "Budget Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        state.budgets.forEachIndexed { index, budget ->
                            val category = state.categories.find { it.id == budget.categoryId }
                            item(key = "budget_${budget.id}") {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(300, delayMillis = 300 + index * 80)) + slideInVertically(
                                        animationSpec = tween(300, delayMillis = 300 + index * 80),
                                        initialOffsetY = { it / 3 }
                                    )
                                ) {
                                    BudgetProgressCard(
                                        categoryName = category?.name ?: "Unknown",
                                        categoryColor = Color(category?.color?.toLong() ?: 0xFF757575),
                                        spent = budget.spentAmount,
                                        limit = budget.limitAmount
                                    )
                                }
                            }
                        }
                    }

                    if (state.activeSubscriptions.isNotEmpty()) {
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(400, delayMillis = 350))
                            ) {
                                SubscriptionSummaryCard(
                                    totalMonthly = state.totalSubscriptionCost,
                                    count = state.activeSubscriptions.size
                                )
                            }
                        }
                    }

                    if (state.categories.isNotEmpty()) {
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(400, delayMillis = 400))
                            ) {
                                Text(
                                    "Spending by Category",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(400, delayMillis = 450))
                            ) {
                                CategoryBreakdownRow(
                                    transactions = state.recentTransactions,
                                    categories = state.categories
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = onNavigateToTransactions) {
                                Text("See All")
                            }
                        }
                    }

                    if (state.recentTransactions.isEmpty()) {
                        item { EmptyStateCard(onNavigateToAdd = onNavigateToAdd) }
                    } else {
                        items(state.recentTransactions.take(5), key = { it.id }) { transaction ->
                            val category = state.categories.find { it.id == transaction.categoryId }
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(300, delayMillis = 500)) + slideInVertically(
                                    animationSpec = tween(300, delayMillis = 500),
                                    initialOffsetY = { it / 3 }
                                )
                            ) {
                                TransactionCard(
                                    transaction = transaction,
                                    categoryName = category?.name ?: "Other",
                                    categoryColor = Color(category?.color?.toLong() ?: 0xFF757575)
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            if (pullRefreshState.isRefreshing) {
                LaunchedEffect(true) {
                    viewModel.handleIntent(DashboardViewIntent.RefreshTransactions(false))
                    pullRefreshState.endRefresh()
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun BalanceCard(balance: Double, income: Double, expense: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Balance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "₹${formatLargeNumber(balance)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IncomeExpenseChip("Income", income, MaterialTheme.colorScheme.primary)
                IncomeExpenseChip("Expense", expense, MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun IncomeExpenseChip(label: String, amount: Double, color: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.15f)) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(
                "₹${formatLargeNumber(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun QuickStatsRow(transactions: Int, largestExpense: Double, subscriptions: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(Modifier.weight(1f), Icons.Default.Receipt, "Transactions", "$transactions")
        StatCard(Modifier.weight(1f), Icons.Default.TrendingUp, "Largest", "₹${formatLargeNumber(largestExpense)}")
        StatCard(Modifier.weight(1f), Icons.Default.Subscriptions, "Subscriptions", "₹${formatLargeNumber(subscriptions)}/mo")
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyChartCard(dailySpending: List<Pair<Long, Double>>) {
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val maxAmount = dailySpending.maxOfOrNull { it.second } ?: 1.0
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("This Week", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = size.width / (dailySpending.size * 2f + 1f)
                    val gap = barWidth

                    dailySpending.forEachIndexed { index, (_, amount) ->
                        val barHeight = if (maxAmount > 0) (amount / maxAmount * size.height).toFloat() else 0f
                        val x = gap + index * (barWidth + gap)
                        val y = size.height - barHeight
                        drawRoundRect(
                            color = if (amount > 0) primaryColor else surfaceVariantColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight.coerceAtLeast(4f)),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                dailySpending.forEachIndexed { index, _ ->
                    Text(
                        dayNames.getOrElse(index) { "" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressCard(categoryName: String, categoryColor: Color, spent: Double, limit: Double) {
    val progress = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
    val progressColor = when {
        progress >= 1f -> MaterialTheme.colorScheme.error
        progress >= 0.8f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(categoryColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Text(
                    "₹${formatLargeNumber(spent)} / ₹${formatLargeNumber(limit)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun SubscriptionSummaryCard(totalMonthly: Double, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Subscriptions, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Active Subscriptions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("$count active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                }
            }
            Text(
                "₹${formatLargeNumber(totalMonthly)}/mo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    transactions: List<Transaction>,
    categories: List<com.awbuilds.auraspend.domain.model.Category>
) {
    val spending = categories.map { cat ->
        val total = transactions
            .filter { it.categoryId == cat.id && it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        cat to total
    }.filter { it.second > 0 }.sortedByDescending { it.second }

    if (spending.isEmpty()) {
        Text("No spending this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(spending) { (category, amount) ->
            Surface(shape = RoundedCornerShape(16.dp), color = Color(category.color).copy(alpha = 0.15f)) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(category.color).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(category.icon.take(2), style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${formatLargeNumber(amount)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(category.color))
                    Text(category.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: Transaction,
    categoryName: String,
    categoryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Circle, contentDescription = null, tint = categoryColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.merchant ?: transaction.note,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    "$categoryName · ${formatRelativeDate(transaction.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}₹${formatLargeNumber(transaction.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyStateCard(onNavigateToAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No transactions yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Add your first transaction to start tracking your finances.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Transaction")
            }
        }
    }
}

private fun formatLargeNumber(value: Double): String {
    return when {
        value >= 1_000_000 -> String.format("%.1fL", value / 100_000)
        value >= 100_000 -> String.format("%.1fL", value / 100_000)
        value >= 1_000 -> String.format("%.1fK", value / 1_000)
        else -> String.format("%.0f", value)
    }
}

private fun formatRelativeDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 24 * 60 * 60 * 1000 -> "Today"
        diff < 48 * 60 * 60 * 1000 -> "Yesterday"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
        else -> { val sdf = SimpleDateFormat("dd MMM", Locale.getDefault()); sdf.format(Date(timestamp)) }
    }
}
