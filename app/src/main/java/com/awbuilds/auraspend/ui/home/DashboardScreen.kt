package com.awbuilds.auraspend.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.R
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.ui.core.HideAmountIconButton
import com.awbuilds.auraspend.ui.core.SectionHeaderRow
import com.awbuilds.auraspend.ui.core.TransactionEntryRow
import com.awbuilds.auraspend.ui.core.categoryColor
import com.awbuilds.auraspend.ui.core.categoryIconEmoji
import com.awbuilds.auraspend.ui.core.formatMoney
import com.awbuilds.auraspend.ui.designsystem.*
import com.awbuilds.auraspend.ui.theme.extendedColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Home — the app's hero surface.
 *
 * Hierarchy: greeting → aurora balance hero → quick actions → cash-flow chart
 * → budgets → subscriptions → recent activity.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTransactions: () -> Unit,
    onQuickAdd: (TransactionType) -> Unit,
    onOpenSmartAdd: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToPlan: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenTransaction: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var hideAmounts by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(DashboardViewIntent.LoadDashboard)
    }

    val extended = MaterialTheme.extendedColors
    val recent = state.recentTransactions.take(6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // ── Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AuraSpacing.gutter, end = AuraSpacing.sm, top = AuraSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    greetingText(),
                    fontSize = 14.sp,
                    color = extended.textLight
                )
                Text(
                    stringResource(R.string.app_name),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            HideAmountIconButton(hidden = hideAmounts, onToggle = { hideAmounts = !hideAmounts })
            SettingsAvatarButton(onClick = onOpenSettings)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AuraSpacing.xxl)
        ) {
            if (state.isLoading && state.recentTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = AuraSpacing.gutter, vertical = AuraSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)
                    ) {
                        AuraSkeleton(modifier = Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(24.dp))
                        AuraSkeleton(modifier = Modifier.fillMaxWidth().height(64.dp))
                        AuraSkeleton(modifier = Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(20.dp))
                    }
                }
                return@LazyColumn
            }

            // ── Balance hero (the one aurora-gradient moment on this screen)
            item {
                Box(modifier = Modifier.padding(horizontal = AuraSpacing.gutter, vertical = AuraSpacing.md)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(AuraGradients.aurora)
                            .padding(AuraSpacing.xl)
                    ) {
                        Text(
                            stringResource(R.string.home_total_balance),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AuraGradients.onAurora.copy(alpha = 0.82f)
                        )
                        Spacer(modifier = Modifier.height(AuraSpacing.xs))
                        if (hideAmounts) {
                            Text(stringResource(R.string.state_hidden_amount), style = AuraType.moneyHero, color = AuraGradients.onAurora)
                        } else {
                            AnimatedMoney(
                                amount = state.totalBalance,
                                style = AuraType.moneyHero,
                                color = AuraGradients.onAurora
                            )
                        }
                        Spacer(modifier = Modifier.height(AuraSpacing.lg))
                        Row(horizontalArrangement = Arrangement.spacedBy(AuraSpacing.xxl)) {
                            HeroStat(
                                label = stringResource(R.string.home_in_this_month),
                                value = if (hideAmounts) stringResource(R.string.state_hidden_amount)
                                else stringResource(R.string.common_amount_plus, formatMoney(state.monthlyIncome)),
                                onClick = onNavigateToAnalytics
                            )
                            HeroStat(
                                label = stringResource(R.string.home_out_this_month),
                                value = if (hideAmounts) stringResource(R.string.state_hidden_amount)
                                else stringResource(R.string.common_amount_minus, formatMoney(state.monthlyExpense)),
                                onClick = onNavigateToAnalytics
                            )
                        }
                    }
                }
            }

            // ── Quick actions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AuraSpacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
                ) {
                    QuickAction(
                        icon = Icons.Default.Add,
                        label = stringResource(R.string.home_quick_expense),
                        modifier = Modifier.weight(1f),
                        onClick = { onQuickAdd(TransactionType.EXPENSE) }
                    )
                    QuickAction(
                        icon = Icons.Default.ArrowDownward,
                        label = stringResource(R.string.home_quick_income),
                        modifier = Modifier.weight(1f),
                        onClick = { onQuickAdd(TransactionType.INCOME) }
                    )
                    QuickAction(
                        icon = Icons.Default.AutoAwesome,
                        label = stringResource(R.string.home_quick_smart_add),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSmartAdd
                    )
                    QuickAction(
                        icon = Icons.Default.BarChart,
                        label = stringResource(R.string.home_quick_stats),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAnalytics
                    )
                }
            }

            // ── Cash flow
            if (state.dailySpending.isNotEmpty()) {
                item {
                    SectionHeaderRow(
                        title = stringResource(R.string.home_cash_flow),
                        modifier = Modifier.padding(top = AuraSpacing.lg)
                    )
                }
                item {
                    Box(modifier = Modifier.padding(horizontal = AuraSpacing.gutter)) {
                        AuraCard(style = AuraCardStyle.Outlined, modifier = Modifier.fillMaxWidth()) {
                            val points = if (hideAmounts) state.dailySpending.map { it.first to 0.0 } else state.dailySpending
                            AuraAreaChart(
                                points = points,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentDescription = stringResource(R.string.home_spending_this_week)
                            )
                            Spacer(modifier = Modifier.height(AuraSpacing.sm))
                            WeekdayLabels(epochDays = state.dailySpending.map { it.first })
                        }
                    }
                }
            }

            // ── Budgets carousel
            if (state.budgets.isNotEmpty()) {
                item {
                    SectionHeaderRow(
                        title = stringResource(R.string.home_budgets),
                        modifier = Modifier.padding(top = AuraSpacing.lg),
                        trailing = {
                            Text(
                                stringResource(R.string.home_manage),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(onClick = onNavigateToPlan)
                                    .padding(horizontal = AuraSpacing.sm, vertical = AuraSpacing.xs)
                            )
                        }
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = AuraSpacing.gutter),
                        horizontalArrangement = Arrangement.spacedBy(AuraSpacing.md)
                    ) {
                        items(state.budgets, key = { it.id }) { budget ->
                            val category = state.categories.find { it.id == budget.categoryId }
                            BudgetCarouselCard(
                                name = category?.name ?: stringResource(R.string.home_unknown_category),
                                emoji = categoryIconEmoji(category?.icon),
                                color = categoryColor(category?.color),
                                spent = budget.spentAmount,
                                limit = budget.limitAmount,
                                hideAmounts = hideAmounts,
                                onClick = onNavigateToPlan
                            )
                        }
                    }
                }
            }

            // ── Subscriptions
            if (state.activeSubscriptions.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = AuraSpacing.gutter, vertical = AuraSpacing.lg)) {
                        AuraCard(
                            style = AuraCardStyle.Tonal,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(AuraSpacing.lg),
                            onClick = onNavigateToPlan
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Subscriptions,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(AuraSpacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.home_subscriptions),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        pluralStringResource(
                                            R.plurals.home_subscriptions_active,
                                            state.activeSubscriptions.size,
                                            state.activeSubscriptions.size
                                        ),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    if (hideAmounts) stringResource(R.string.state_hidden_amount)
                                    else stringResource(R.string.home_subscription_monthly, formatMoney(state.totalSubscriptionCost)),
                                    style = AuraType.moneyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // ── Recent activity
            item {
                SectionHeaderRow(
                    title = stringResource(R.string.home_recent),
                    modifier = Modifier.padding(top = AuraSpacing.sm),
                    trailing = {
                        Text(
                            stringResource(R.string.home_view_all),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onNavigateToTransactions)
                                .padding(horizontal = AuraSpacing.sm, vertical = AuraSpacing.xs)
                        )
                    }
                )
            }

            if (recent.isEmpty()) {
                item {
                    AuraEmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = stringResource(R.string.home_empty_title),
                        message = stringResource(R.string.home_empty_message),
                        actionLabel = stringResource(R.string.home_add_transaction),
                        onAction = { onQuickAdd(TransactionType.EXPENSE) }
                    )
                }
            } else {
                items(recent, key = { it.id }) { transaction ->
                    val category = state.categories.find { it.id == transaction.categoryId }
                    TransactionEntryRow(
                        transaction = transaction,
                        categoryName = category?.name ?: stringResource(R.string.home_other_category),
                        categoryColor = categoryColor(category?.color),
                        categoryEmoji = categoryIconEmoji(category?.icon),
                        modifier = Modifier.animateItem(),
                        onClick = { onOpenTransaction(transaction.id) }
                    )
                }
            }
        }
    }
}

// ─── Pieces ───────────────────────────────────────────────────────────────────

@Composable
private fun HeroStat(label: String, value: String, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Text(
            label,
            fontSize = 12.sp,
            color = AuraGradients.onAurora.copy(alpha = 0.78f)
        )
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AuraGradients.onAurora
        )
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = AuraSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(AuraSpacing.sm))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WeekdayLabels(epochDays: List<Long>) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEE") }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        epochDays.forEach { epoch ->
            Text(
                Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(formatter),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetCarouselCard(
    name: String,
    emoji: String,
    color: Color,
    spent: Double,
    limit: Double,
    hideAmounts: Boolean,
    onClick: () -> Unit
) {
    val progress = if (limit > 0) (spent / limit).toFloat() else 0f
    val overBudget = limit > 0 && spent > limit

    AuraCard(
        modifier = Modifier.width(168.dp),
        style = AuraCardStyle.Outlined,
        contentPadding = PaddingValues(AuraSpacing.lg),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AuraProgressRing(
                progress = progress,
                color = if (overBudget) MaterialTheme.extendedColors.expenseAmount else color,
                modifier = Modifier.size(40.dp),
                stroke = 3.5.dp,
                contentDescription = stringResource(
                    R.string.home_budget_percent_used,
                    name,
                    (progress.coerceIn(0f, 1f) * 100).toInt()
                )
            ) {
                CategoryAvatar(icon = emoji, color = color, size = 26.dp)
            }
            Spacer(modifier = Modifier.width(AuraSpacing.sm))
            Text(
                name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(AuraSpacing.md))
        Text(
            if (hideAmounts) stringResource(R.string.state_hidden_amount) else formatMoney(spent),
            style = AuraType.moneyMedium,
            color = if (overBudget) MaterialTheme.extendedColors.expenseAmount
            else MaterialTheme.colorScheme.onSurface
        )
        Text(
            if (hideAmounts) stringResource(R.string.home_budget_of, stringResource(R.string.state_hidden_amount))
            else stringResource(R.string.home_budget_of, formatMoney(limit)),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun greetingText(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> stringResource(R.string.home_greeting_morning)
        hour < 17 -> stringResource(R.string.home_greeting_afternoon)
        else -> stringResource(R.string.home_greeting_evening)
    }
}
