package com.awbuilds.auraspend.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.ui.core.*
import com.awbuilds.auraspend.ui.theme.extendedColors
import java.time.Instant
import java.time.ZoneId

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var selectedListIndex by remember { mutableStateOf(0) }
    var hideAmounts by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(DashboardViewIntent.LoadDashboard)
    }

    val extended = MaterialTheme.extendedColors
    val filteredTransactions = when (selectedListIndex) {
        1 -> state.recentTransactions.filter { it.type == TransactionType.EXPENSE }
        2 -> state.recentTransactions.filter { it.type == TransactionType.INCOME }
        else -> state.recentTransactions
    }
    fun money(value: Double): String =
        if (hideAmounts) "•••" else formatMoney(value)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // ── Greeting header (Cashew username header style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 13.dp, end = 4.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    greetingText(),
                    fontSize = 15.sp,
                    color = extended.textLight
                )
                Text(
                    "AuraSpend",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            HideAmountIconButton(hidden = hideAmounts, onToggle = { hideAmounts = !hideAmounts })
            Spacer(modifier = Modifier.width(2.dp))
            SettingsAvatarButton(onClick = onOpenSettings)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (state.isLoading && state.recentTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 120.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
                return@LazyColumn
            }

            // ── Expense | Income summary boxes (Cashew all-spending summary)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    AmountSummaryBox(
                        label = "Expense",
                        amount = if (hideAmounts) 0.0 else state.monthlyExpense,
                        transactionCount = if (hideAmounts) null else state.monthlyExpenseCount,
                        amountColor = extended.expenseAmount,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAnalytics
                    )
                    AmountSummaryBox(
                        label = "Income",
                        amount = if (hideAmounts) 0.0 else state.monthlyIncome,
                        transactionCount = if (hideAmounts) null else state.monthlyIncomeCount,
                        amountColor = extended.incomeAmount,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAnalytics
                    )
                }
            }

            // ── Net worth box
            item {
                Box(modifier = Modifier.padding(horizontal = 13.dp)) {
                    CashewCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Net Worth",
                            fontSize = 15.sp,
                            color = extended.textLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            money(state.totalBalance),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }

            // ── Budgets (Cashew budget rows with progress rings + bars)
            if (state.budgets.isNotEmpty()) {
                item { SectionHeaderRow("Budget", modifier = Modifier.padding(top = 14.dp)) }
                item {
                    Box(modifier = Modifier.padding(horizontal = 13.dp)) {
                        CashewCard(modifier = Modifier.fillMaxWidth()) {
                            state.budgets.forEachIndexed { index, budget ->
                                val category = state.categories.find { it.id == budget.categoryId }
                                BudgetRowItem(
                                    name = category?.name ?: "Unknown",
                                    emoji = categoryIconEmoji(category?.icon),
                                    color = categoryColor(category?.color),
                                    spent = budget.spentAmount,
                                    limit = budget.limitAmount,
                                    transactionCount = state.recentTransactions.count {
                                        it.categoryId == budget.categoryId &&
                                                it.type == TransactionType.EXPENSE
                                    },
                                    hideAmounts = hideAmounts
                                )
                                if (index != state.budgets.lastIndex) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── Subscriptions summary
            if (state.activeSubscriptions.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 13.dp, vertical = 13.dp)) {
                        CashewCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🔁", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Subscriptions",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "${state.activeSubscriptions.size} active",
                                        fontSize = 13.sp,
                                        color = extended.textLight
                                    )
                                }
                                Text(
                                    "${money(state.totalSubscriptionCost)}/mo",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // ── Weekly spending graph
            if (state.dailySpending.isNotEmpty()) {
                item {
                    SectionHeaderRow("Spending Graph", modifier = Modifier.padding(top = 6.dp))
                }
                item {
                    Box(modifier = Modifier.padding(horizontal = 13.dp)) {
                        CashewCard(modifier = Modifier.fillMaxWidth()) {
                            WeeklyLineChart(dailySpending = state.dailySpending)
                        }
                    }
                }
            }

            // ── Category pie chart
            if (state.categoryMonthTotals.isNotEmpty()) {
                item {
                    SectionHeaderRow("Spending by Category", modifier = Modifier.padding(top = 6.dp))
                }
                item {
                    Box(modifier = Modifier.padding(horizontal = 13.dp)) {
                        CashewCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToAnalytics) {
                            CategoryPieSection(
                                categoryTotals = state.categoryMonthTotals,
                                categories = state.categories,
                                hideAmounts = hideAmounts
                            )
                        }
                    }
                }
            }

            // ── Transactions list
            item { SectionHeaderRow("Transactions", modifier = Modifier.padding(top = 14.dp)) }
            item {
                SlidingSelector(
                    options = listOf("All", "Outgoing", "Incoming"),
                    selectedIndex = selectedListIndex,
                    onSelect = { selectedListIndex = it },
                    modifier = Modifier.padding(horizontal = 13.dp)
                )
            }

            if (filteredTransactions.isEmpty()) {
                item { EmptyHomeState(onAddClick = onNavigateToAdd) }
            } else {
                items(filteredTransactions.take(8)) { transaction ->
                    val category = state.categories.find { it.id == transaction.categoryId }
                    TransactionEntryRow(
                        transaction = transaction,
                        categoryName = category?.name ?: "Other",
                        categoryColor = categoryColor(category?.color),
                        categoryEmoji = categoryIconEmoji(category?.icon)
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ViewAllButton(onClick = onNavigateToTransactions)
                }
            }
        }
    }
}

// ─── Budget row ───────────────────────────────────────────────────────────────

@Composable
private fun BudgetRowItem(
    name: String,
    emoji: String,
    color: Color,
    spent: Double,
    limit: Double,
    transactionCount: Int,
    hideAmounts: Boolean
) {
    val extended = MaterialTheme.extendedColors
    val progress = if (limit > 0) (spent / limit).toFloat() else 0f
    val overBudget = limit > 0 && spent > limit
    val spentText = if (hideAmounts) "•••" else formatMoney(spent)
    val limitText = if (hideAmounts) "•••" else formatMoney(limit)

    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressRing(
            progress = progress.coerceIn(0f, 1f),
            color = if (overBudget) extended.expenseAmount else color,
            modifier = Modifier.size(50.dp),
            stroke = 3.dp,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "$name · $transactionCount ${if (transactionCount == 1) "transaction" else "transactions"}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (overBudget) extended.expenseAmount else color,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row {
                Text(
                    spentText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (overBudget) extended.expenseAmount else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    " / $limitText",
                    fontSize = 15.sp,
                    color = extended.textLight
                )
            }
        }
    }
}

// ── Weekly line chart ────────────────────────────────────────────────────────

@Composable
private fun WeeklyLineChart(dailySpending: List<Pair<Long, Double>>) {
    val primary = MaterialTheme.colorScheme.primary
    val textLight = MaterialTheme.extendedColors.textLight
    val dayFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("EEE") }

    Column {
        val maxVal = dailySpending.maxOfOrNull { it.second } ?: 0.0
        Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
            val n = dailySpending.size
            if (n < 2 || maxVal <= 0.0) return@Canvas
            val stepX = size.width / (n - 1)
            val points = dailySpending.mapIndexed { i, (_, value) ->
                Offset(i * stepX, size.height - (value / maxVal).toFloat() * (size.height - 12f))
            }
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    val midX = (p0.x + p1.x) / 2
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
            }
            drawPath(path, primary, style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dailySpending.forEach { (epochMillis, _) ->
                Text(
                    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dayFormatter),
                    fontSize = 12.sp,
                    color = textLight
                )
            }
        }
    }
}

// ── Category pie section ─────────────────────────────────────────────────────

@Composable
private fun CategoryPieSection(
    categoryTotals: List<Pair<String, Double>>,
    categories: List<com.awbuilds.auraspend.domain.model.Category>,
    hideAmounts: Boolean
) {
    val total = categoryTotals.sumOf { it.second }
    Row(verticalAlignment = Alignment.CenterVertically) {
        DonutChart(
            data = categoryTotals.map { (categoryId, value) ->
                val category = categories.find { it.id == categoryId }
                PieSliceData(
                    label = category?.name ?: "Other",
                    value = value,
                    color = categoryColor(category?.color)
                )
            },
            modifier = Modifier.size(150.dp),
            strokeWidth = 28.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", fontSize = 13.sp, color = MaterialTheme.extendedColors.textLight)
                Text(
                    if (hideAmounts) "•••" else formatMoney(total),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categoryTotals.take(5).forEach { (categoryId, value) ->
                val category = categories.find { it.id == categoryId }
                val pct = if (total > 0) ((value / total) * 100).toInt() else 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(categoryColor(category?.color))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        category?.name ?: "Other",
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "$pct%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.extendedColors.textLight
                    )
                }
            }
            if (categoryTotals.size > 5) {
                Text(
                    "+${categoryTotals.size - 5} more",
                    fontSize = 13.sp,
                    color = MaterialTheme.extendedColors.textLight
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyHomeState(onAddClick: () -> Unit) {
    Box(modifier = Modifier.padding(13.dp)) {
        CashewCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🪙", fontSize = 42.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "No transactions yet",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Add your first transaction to start tracking your finances.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.extendedColors.textLight
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onAddClick)
                        .padding(horizontal = 22.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Add Transaction",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Header avatar — the single entry point to Settings (P1 IA). */
@Composable
private fun SettingsAvatarButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

fun greetingText(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

