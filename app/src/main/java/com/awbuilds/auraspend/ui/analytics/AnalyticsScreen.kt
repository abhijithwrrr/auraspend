package com.awbuilds.auraspend.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.ui.core.AmountSummaryBox
import com.awbuilds.auraspend.ui.core.CashewCard
import com.awbuilds.auraspend.ui.core.DonutChart
import com.awbuilds.auraspend.ui.core.PieSliceData
import com.awbuilds.auraspend.ui.core.SectionHeaderRow
import com.awbuilds.auraspend.ui.core.SlidingSelector
import com.awbuilds.auraspend.ui.core.categoryColor
import com.awbuilds.auraspend.ui.core.formatMoney
import com.awbuilds.auraspend.ui.theme.extendedColors
import java.time.LocalDate
import java.time.ZoneId

private enum class StatsPeriod(val label: String) {
    THIS_MONTH("This Month"), LAST_30("30 Days"), ALL_TIME("All Time")
}

@Composable
fun AnalyticsScreen(
    transactions: List<Transaction>,
    categories: List<Category>,
    onBack: () -> Unit
) {
    val extended = MaterialTheme.extendedColors
    var period by remember { mutableStateOf(StatsPeriod.THIS_MONTH) }
    val zone = ZoneId.systemDefault()

    val windowStartMillis = remember(period) {
        val today = LocalDate.now()
        val start = when (period) {
            StatsPeriod.THIS_MONTH -> today.withDayOfMonth(1)
            StatsPeriod.LAST_30 -> today.minusDays(29)
            StatsPeriod.ALL_TIME -> null
        }
        start?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: 0L
    }

    val scoped = remember(transactions, windowStartMillis) {
        transactions.filter {
            it.date.atZone(zone).toInstant().toEpochMilli() >= windowStartMillis
        }
    }

    val expenseTransactions = scoped.filter { it.type == TransactionType.EXPENSE }
    val totalExpense = expenseTransactions.sumOf { it.amount }
    val totalIncome = scoped.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val incomeCount = scoped.count { it.type == TransactionType.INCOME }
    val expenseCount = expenseTransactions.size
    val net = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((net / totalIncome) * 100).coerceIn(-999.0, 100.0) else null

    val categorySpending = categories.map { cat ->
        val spent = expenseTransactions
            .filter { it.categoryId == cat.id }
            .sumOf { it.amount }
        cat to spent
    }.filter { it.second > 0 }
        .sortedByDescending { it.second }

    val totalSpent = categorySpending.sumOf { it.second }

    data class MerchantStat(val label: String, val amount: Double, val count: Int)

    val merchantSpending = expenseTransactions
        .groupBy { it.merchant?.takeIf { m -> m.isNotBlank() } ?: "Unlabelled" }
        .map { (name, list) -> MerchantStat(name.trim(), list.sumOf { it.amount }, list.size) }
        .sortedByDescending { it.amount }
        .take(10)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Text(
            "Stats",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 13.dp, top = 10.dp, bottom = 4.dp)
        )

        // ── Period selector ────────────────────────────────────────────────────
        SlidingSelector(
            options = StatsPeriod.entries.map { it.label },
            selectedIndex = period.ordinal,
            onSelect = { period = StatsPeriod.entries[it] },
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Income | Expense summary boxes
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    AmountSummaryBox(
                        label = "Expense",
                        amount = totalExpense,
                        transactionCount = expenseCount,
                        amountColor = extended.expenseAmount,
                        modifier = Modifier.weight(1f)
                    )
                    AmountSummaryBox(
                        label = "Income",
                        amount = totalIncome,
                        transactionCount = incomeCount,
                        amountColor = extended.incomeAmount,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Net & savings rate ─────────────────────────────────────────────
            if (scoped.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 13.dp)) {
                        CashewCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Net ${period.label.lowercase()}",
                                        fontSize = 13.sp,
                                        color = extended.textLight
                                    )
                                    Text(
                                        "${if (net >= 0) "+" else "-"}${formatMoney(kotlin.math.abs(net))}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (net >= 0) extended.incomeAmount else extended.expenseAmount
                                    )
                                }
                                savingsRate?.let { rate ->
                                    Box(
                                        modifier = Modifier
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                            .background(
                                                if (rate >= 0) extended.incomeAmount.copy(alpha = 0.14f)
                                                else extended.expenseAmount.copy(alpha = 0.14f)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Saving ${rate.toInt()}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (rate >= 0) extended.incomeAmount else extended.expenseAmount
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Spending by category (donut + legend)
            if (categorySpending.isNotEmpty()) {
                item { SectionHeaderRow("Spending by Category") }
                item {
                    Box(modifier = Modifier.padding(horizontal = 13.dp)) {
                        CashewCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                            DonutChart(
                                data = categorySpending.map { (cat, value) ->
                                    PieSliceData(cat.name, value, categoryColor(cat.color))
                                },
                                modifier = Modifier
                                    .size(190.dp)
                                    .align(Alignment.CenterHorizontally),
                                strokeWidth = 34.dp
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total", fontSize = 13.sp, color = extended.textLight)
                                    Text(
                                        formatMoney(totalExpense),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            categorySpending.forEachIndexed { index, (cat, amount) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor(cat.color))
                                    )
                                    Spacer(modifier = Modifier.width(9.dp))
                                    Text(
                                        cat.name,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${if (totalSpent > 0) (amount / totalSpent * 100).toInt() else 0}%",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = extended.textLight
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        formatMoney(amount),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (index != categorySpending.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }

            // ── Top merchants
            if (merchantSpending.isNotEmpty()) {
                item { SectionHeaderRow("Top Merchants", modifier = Modifier.padding(top = 8.dp)) }
                item {
                    Box(modifier = Modifier.padding(horizontal = 13.dp)) {
                        CashewCard(modifier = Modifier.fillMaxWidth()) {
                            merchantSpending.forEachIndexed { index, stat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            stat.label.let {
                                                if (it.length > 26) it.take(25) + "…" else it
                                            },
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "${stat.count} txn${if (stat.count > 1) "s" else ""}",
                                            fontSize = 12.sp,
                                            color = extended.textLight
                                        )
                                    }
                                    Text(
                                        formatMoney(stat.amount),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = extended.expenseAmount
                                    )
                                }
                                if (index != merchantSpending.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }

            if (categorySpending.isEmpty() && merchantSpending.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📊", fontSize = 44.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (period == StatsPeriod.ALL_TIME) "No analytics data yet"
                                else "Nothing in this period",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Add transactions to see insights.",
                                fontSize = 14.sp,
                                color = extended.textLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
