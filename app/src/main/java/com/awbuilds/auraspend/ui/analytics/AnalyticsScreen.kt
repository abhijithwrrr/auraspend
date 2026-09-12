package com.awbuilds.auraspend.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.ui.designsystem.AuraAreaChart
import com.awbuilds.auraspend.ui.designsystem.AuraCard
import com.awbuilds.auraspend.ui.designsystem.AuraCardStyle
import com.awbuilds.auraspend.ui.designsystem.AuraEmptyState
import com.awbuilds.auraspend.ui.designsystem.AuraSegmentedControl
import com.awbuilds.auraspend.ui.designsystem.AuraSlice
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing
import com.awbuilds.auraspend.ui.designsystem.AuraType
import com.awbuilds.auraspend.ui.designsystem.AnimatedMoney
import com.awbuilds.auraspend.ui.designsystem.AuraDonutChart
import com.awbuilds.auraspend.ui.designsystem.SettingsAvatarButton
import com.awbuilds.auraspend.ui.designsystem.formatMoney
import com.awbuilds.auraspend.ui.theme.extendedColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import java.time.LocalDate
import java.time.ZoneId

private enum class StatsPeriod(val label: String) {
    THIS_MONTH("This Month"), LAST_30("30 Days"), ALL_TIME("All Time")
}

/**
 * Insights — spending analytics with animated data visualisations and
 * month-over-month context.
 */
@Composable
fun AnalyticsScreen(
    transactions: List<Transaction>,
    categories: List<Category>,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val extended = MaterialTheme.extendedColors
    var period by remember { mutableStateOf(StatsPeriod.THIS_MONTH) }
    val zone = ZoneId.systemDefault()

    val scoped = remember(transactions, period) {
        val today = LocalDate.now()
        val start = when (period) {
            StatsPeriod.THIS_MONTH -> today.withDayOfMonth(1)
            StatsPeriod.LAST_30 -> today.minusDays(29)
            StatsPeriod.ALL_TIME -> null
        }
        val startMillis = start?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: 0L
        transactions.filter {
            it.date.atZone(zone).toInstant().toEpochMilli() >= startMillis
        }
    }

    val expenseTxns = remember(scoped) { scoped.filter { it.type == TransactionType.EXPENSE } }
    val totalExpense = remember(expenseTxns) { expenseTxns.sumOf { it.amount } }
    val totalIncome = remember(scoped) { scoped.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } }
    val net = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((net / totalIncome) * 100).coerceIn(-999.0, 100.0) else null

    val categorySpending = remember(expenseTxns, categories) {
        categories.map { cat ->
            cat to expenseTxns.filter { it.categoryId == cat.id }.sumOf { it.amount }
        }.filter { it.second > 0 }.sortedByDescending { it.second }
    }
    val totalSpent = remember(categorySpending) { categorySpending.sumOf { it.second } }

    val merchantSpending = remember(expenseTxns) {
        expenseTxns
            .groupBy { it.merchant?.takeIf { m -> m.isNotBlank() } ?: "Unlabelled" }
            .map { (name, list) -> Triple(name.trim(), list.sumOf { it.amount }, list.size) }
            .sortedByDescending { it.second }
            .take(6)
    }

    // Month-over-month expense delta (only meaningful for "This Month").
    val monthDelta = remember(transactions, period) {
        if (period != StatsPeriod.THIS_MONTH) return@remember null
        val today = LocalDate.now()
        val thisStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val prevStart = today.minusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        var thisMonth = 0.0
        var prevMonth = 0.0
        transactions.forEach { txn ->
            if (txn.type != TransactionType.EXPENSE) return@forEach
            val ts = txn.date.atZone(zone).toInstant().toEpochMilli()
            when {
                ts >= thisStart -> thisMonth += txn.amount
                ts in prevStart until thisStart -> prevMonth += txn.amount
            }
        }
        if (prevMonth <= 0.0) null else ((thisMonth - prevMonth) / prevMonth * 100).toInt()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AuraSpacing.gutter, top = AuraSpacing.sm, bottom = AuraSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Insights",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            SettingsAvatarButton(onClick = onOpenSettings)
        }
        AuraSegmentedControl(
            options = StatsPeriod.entries.map { it.label },
            selectedIndex = period.ordinal,
            onSelect = { period = StatsPeriod.entries[it] },
            modifier = Modifier.padding(horizontal = AuraSpacing.gutter)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AuraSpacing.xxl)
        ) {
            // ── Summary hero
            item {
                Box(modifier = Modifier.padding(horizontal = AuraSpacing.gutter, vertical = AuraSpacing.lg)) {
                    AuraCard(style = AuraCardStyle.Tonal, modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(AuraSpacing.xxl)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Expense",
                                    style = AuraType.metricLabel,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(AuraSpacing.xs))
                                AnimatedMoney(
                                    amount = totalExpense,
                                    style = AuraType.moneyLarge,
                                    color = extended.expenseAmount
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Income",
                                    style = AuraType.metricLabel,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(AuraSpacing.xs))
                                AnimatedMoney(
                                    amount = totalIncome,
                                    style = AuraType.moneyLarge,
                                    color = extended.incomeAmount
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(AuraSpacing.lg))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(AuraSpacing.md))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Net ${period.label.lowercase()}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${if (net >= 0) "+" else "-"}${formatMoney(kotlin.math.abs(net))}",
                                    style = AuraType.moneyMedium,
                                    color = if (net >= 0) extended.incomeAmount else extended.expenseAmount
                                )
                            }
                            savingsRate?.let { rate ->
                                Badge(
                                    text = "Saving ${rate.toInt()}%",
                                    color = if (rate >= 0) extended.incomeAmount else extended.expenseAmount
                                )
                            }
                        }
                        monthDelta?.let { delta ->
                            Spacer(modifier = Modifier.height(AuraSpacing.sm))
                            Badge(
                                text = if (delta >= 0) "↑ $delta% vs last month" else "↓ ${-delta}% vs last month",
                                color = if (delta >= 0) extended.expenseAmount else extended.incomeAmount
                            )
                        }
                    }
                }
            }

            // ── Category donut + legend
            if (categorySpending.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = AuraSpacing.gutter)) {
                        AuraCard(style = AuraCardStyle.Outlined, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Spending by category",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(AuraSpacing.lg))
                            AuraDonutChart(
                                data = categorySpending.map { (cat, value) ->
                                    AuraSlice(cat.name, value, androidx.compose.ui.graphics.Color(cat.color.toLong()))
                                },
                                modifier = Modifier
                                    .size(190.dp)
                                    .align(Alignment.CenterHorizontally),
                                strokeWidth = 32.dp,
                                contentDescription = "Spending by category, total ${formatMoney(totalSpent)}"
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total", fontSize = 12.sp, color = extended.textLight)
                                    Text(
                                        formatMoney(totalSpent),
                                        style = AuraType.moneyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(AuraSpacing.lg))
                            categorySpending.forEachIndexed { index, (cat, amount) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = AuraSpacing.xs),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(androidx.compose.ui.graphics.Color(cat.color.toLong()))
                                    )
                                    Spacer(modifier = Modifier.width(AuraSpacing.sm))
                                    Text(
                                        cat.name,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${if (totalSpent > 0) (amount / totalSpent * 100).toInt() else 0}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = extended.textLight
                                    )
                                    Spacer(modifier = Modifier.width(AuraSpacing.md))
                                    Text(
                                        formatMoney(amount),
                                        style = AuraType.moneySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (index != categorySpending.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Top merchants
            if (merchantSpending.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = AuraSpacing.gutter, vertical = AuraSpacing.lg)) {
                        AuraCard(style = AuraCardStyle.Outlined, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Top merchants",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(AuraSpacing.sm))
                            merchantSpending.forEachIndexed { index, (name, amount, count) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = AuraSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            name,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "$count txn${if (count > 1) "s" else ""}",
                                            fontSize = 12.sp,
                                            color = extended.textLight
                                        )
                                    }
                                    Text(
                                        formatMoney(amount),
                                        style = AuraType.moneySmall,
                                        color = extended.expenseAmount
                                    )
                                }
                                if (index != merchantSpending.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (categorySpending.isEmpty() && merchantSpending.isEmpty()) {
                item {
                    AuraEmptyState(
                        icon = Icons.Default.Insights,
                        title = if (period == StatsPeriod.ALL_TIME) "No insights yet" else "Nothing in this period",
                        message = "Add transactions and insights will appear here."
                    )
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = AuraSpacing.md, vertical = 6.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
