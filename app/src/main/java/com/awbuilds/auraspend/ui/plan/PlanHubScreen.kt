package com.awbuilds.auraspend.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.domain.model.RecurrenceFrequency
import com.awbuilds.auraspend.domain.repository.TransactionRepository
import com.awbuilds.auraspend.ui.designsystem.AuraCard
import com.awbuilds.auraspend.ui.designsystem.AuraCardStyle
import com.awbuilds.auraspend.ui.designsystem.AuraProgressRing
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing
import com.awbuilds.auraspend.ui.designsystem.AuraType
import com.awbuilds.auraspend.ui.designsystem.formatMoney
import com.awbuilds.auraspend.ui.theme.extendedColors

/**
 * Plan hub — the home for everything forward-looking: budgets, subscriptions
 * and (from P3) savings goals.
 */
@Composable
fun PlanHubScreen(
    repository: TransactionRepository,
    onOpenBudgets: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenCategories: () -> Unit
) {
    val budgets by repository.getAllBudgets().collectAsState(initial = emptyList())
    val subscriptions by repository.getActiveSubscriptions().collectAsState(initial = emptyList())

    val totalLimit = remember(budgets) { budgets.sumOf { it.limitAmount } }
    val totalSpent = remember(budgets) { budgets.sumOf { it.spentAmount } }
    val budgetProgress = if (totalLimit > 0) (totalSpent / totalLimit).toFloat().coerceIn(0f, 1f) else 0f
    val monthlySubscriptions = remember(subscriptions) {
        subscriptions.sumOf { sub ->
            sub.amount * when (sub.billingCycle) {
                RecurrenceFrequency.DAILY -> 30.0
                RecurrenceFrequency.WEEKLY -> 52.0 / 12.0
                RecurrenceFrequency.MONTHLY -> 1.0
                RecurrenceFrequency.YEARLY -> 1.0 / 12.0
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = AuraSpacing.gutter)
    ) {
        Text(
            "Plan",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = AuraSpacing.sm, bottom = AuraSpacing.xs)
        )

        Spacer(modifier = Modifier.height(AuraSpacing.md))

        // ── Budget health hero
        AuraCard(style = AuraCardStyle.Tonal, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "This month's budgets",
                        style = AuraType.metricLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(AuraSpacing.xs))
                    Text(
                        "${formatMoney(totalSpent)} of ${formatMoney(totalLimit)}",
                        style = AuraType.moneyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(AuraSpacing.xs))
                    Text(
                        if (totalLimit > 0) "${(budgetProgress * 100).toInt()}% used"
                        else "No budgets yet",
                        fontSize = 13.sp,
                        color = MaterialTheme.extendedColors.textLight
                    )
                }
                AuraProgressRing(
                    progress = budgetProgress,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                    stroke = 6.dp
                ) {
                    Text(
                        "${(budgetProgress * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AuraSpacing.lg))

        // ── Forward-looking totals
        AuraCard(style = AuraCardStyle.Outlined, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Recurring monthly",
                style = AuraType.metricLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(AuraSpacing.xs))
            Text(
                "${formatMoney(monthlySubscriptions)}/mo",
                style = AuraType.moneyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AuraSpacing.xs))
            Text(
                "${subscriptions.size} active ${if (subscriptions.size == 1) "subscription" else "subscriptions"}",
                fontSize = 13.sp,
                color = MaterialTheme.extendedColors.textLight
            )
        }

        Spacer(modifier = Modifier.height(AuraSpacing.lg))

        // ── Management entries
        PlanRow(
            icon = Icons.Default.AccountBalance,
            title = "Budgets",
            subtitle = "Spending limits per category",
            onClick = onOpenBudgets
        )
        Spacer(modifier = Modifier.height(AuraSpacing.sm))
        PlanRow(
            icon = Icons.Default.Subscriptions,
            title = "Subscriptions",
            subtitle = "Recurring charges and renewals",
            onClick = onOpenSubscriptions
        )
        Spacer(modifier = Modifier.height(AuraSpacing.sm))
        PlanRow(
            icon = Icons.Default.Category,
            title = "Categories",
            subtitle = "Organize how money is grouped",
            onClick = onOpenCategories
        )

        Spacer(modifier = Modifier.height(AuraSpacing.xxl))
    }
}

@Composable
private fun PlanRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    AuraCard(
        style = AuraCardStyle.Outlined,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = AuraSpacing.lg, vertical = AuraSpacing.lg),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(AuraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.extendedColors.textLight
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
