package com.awbuilds.auraspend.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.awbuilds.auraspend.R
import com.awbuilds.auraspend.domain.model.BudgetPeriod
import com.awbuilds.auraspend.ui.designsystem.AuraCard
import com.awbuilds.auraspend.ui.designsystem.AuraCardStyle
import com.awbuilds.auraspend.ui.designsystem.AuraEmptyState
import com.awbuilds.auraspend.ui.designsystem.AuraProgressRing
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing
import com.awbuilds.auraspend.ui.designsystem.AuraType
import com.awbuilds.auraspend.ui.designsystem.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.handleIntent(BudgetViewIntent.LoadBudgets)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.budget_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.handleIntent(BudgetViewIntent.StartAdd) }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.budget_add))
                    }
                }
            )
        }
    ) { padding ->
        if (state.budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                AuraEmptyState(
                    icon = Icons.Default.AccountBalance,
                    title = stringResource(R.string.budget_empty_title),
                    message = stringResource(R.string.budget_empty_message),
                    actionLabel = stringResource(R.string.budget_add),
                    onAction = { viewModel.handleIntent(BudgetViewIntent.StartAdd) }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    horizontal = AuraSpacing.gutter,
                    vertical = AuraSpacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)
            ) {
                // ── Overall budget health ──────────────────────────────────────
                item {
                    val totalLimit = state.budgets.sumOf { it.limitAmount }
                    val totalSpent = state.budgets.sumOf { it.spentAmount }
                    val overall = if (totalLimit > 0) (totalSpent / totalLimit).toFloat() else 0f
                    val today = java.time.LocalDate.now()
                    val daysLeft = today.lengthOfMonth() - today.dayOfMonth + 1
                    val overallColor = when {
                        overall >= 1f -> MaterialTheme.colorScheme.error
                        overall >= 0.8f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    AuraCard(
                        modifier = Modifier.fillMaxWidth(),
                        style = AuraCardStyle.Tonal
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.budget_all),
                                    style = AuraType.metricLabel,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(AuraSpacing.xs))
                                Text(
                                    stringResource(R.string.budget_spent_of, formatMoney(totalSpent), formatMoney(totalLimit)),
                                    style = AuraType.moneyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(AuraSpacing.xs))
                                Text(
                                    pluralStringResource(R.plurals.budget_days_left, daysLeft, daysLeft),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(AuraSpacing.lg))
                            AuraProgressRing(
                                progress = overall.coerceIn(0f, 1f),
                                color = overallColor,
                                modifier = Modifier.size(72.dp),
                                stroke = 7.dp
                            ) {
                                Text(
                                    stringResource(R.string.common_percent, (overall.coerceIn(0f, 1f) * 100).toInt()),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                items(state.budgets) { budget ->
                    val category = state.categories.find { it.id == budget.categoryId }
                    val progress = if (budget.limitAmount > 0) (budget.spentAmount / budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
                    val overBy = budget.spentAmount - budget.limitAmount
                    val progressColor = when {
                        progress >= 1f -> MaterialTheme.colorScheme.error
                        progress >= 0.8f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    val categoryColor = category?.let { Color(it.color.toLong()) }
                        ?: MaterialTheme.colorScheme.onSurfaceVariant

                    AuraCard(
                        modifier = Modifier.fillMaxWidth(),
                        style = AuraCardStyle.Outlined
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor)
                            )
                            Spacer(modifier = Modifier.width(AuraSpacing.sm))
                            Text(
                                category?.name ?: stringResource(R.string.budget_unknown_category),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { viewModel.handleIntent(BudgetViewIntent.DeleteBudget(budget.id)) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(AuraSpacing.md))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuraProgressRing(
                                progress = progress,
                                color = progressColor,
                                modifier = Modifier.size(64.dp),
                                stroke = 6.dp
                            ) {
                                Text(
                                    stringResource(R.string.common_percent, (progress * 100).toInt()),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(AuraSpacing.lg))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.budget_spent, formatMoney(budget.spentAmount)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(AuraSpacing.xs))
                                if (overBy > 0) {
                                    Text(
                                        stringResource(R.string.budget_over_by, formatMoney(overBy)),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text(
                                        stringResource(R.string.budget_limit, formatMoney(budget.limitAmount)),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(AuraSpacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                budget.period.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (progress >= 1f) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(AuraSpacing.xs))
                                    Text(
                                        stringResource(R.string.budget_limit_reached),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else if (progress >= 0.8f) {
                                Text(
                                    stringResource(R.string.budget_percent_used, (progress * 100).toInt()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.isAdding || state.editingBudget != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.handleIntent(BudgetViewIntent.CancelEdit) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AuraSpacing.xxl)
                    .padding(bottom = AuraSpacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(AuraSpacing.xl)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AuraSpacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            if (state.editingBudget != null) stringResource(R.string.budget_edit_title)
                            else stringResource(R.string.budget_new_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.budget_editor_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
                    Text(
                        stringResource(R.string.budget_category),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.categories.isEmpty()) {
                        Text(
                            stringResource(R.string.budget_no_categories),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.categories.take(6).forEach { cat ->
                            val catColor = Color(cat.color.toLong())
                            FilterChip(
                                selected = state.selectedCategoryId == cat.id,
                                onClick = { viewModel.handleIntent(BudgetViewIntent.SelectCategory(cat.id)) },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(catColor)
                                        )
                                        Text(cat.name, style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.limitAmount,
                    onValueChange = { viewModel.handleIntent(BudgetViewIntent.AmountChanged(it)) },
                    label = { Text(stringResource(R.string.budget_monthly_limit)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = {
                        Icon(
                            Icons.Default.CurrencyRupee,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
                    Text(
                        stringResource(R.string.budget_period),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
                    ) {
                        BudgetPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = state.selectedPeriod == period,
                                onClick = { viewModel.handleIntent(BudgetViewIntent.PeriodChanged(period)) },
                                label = {
                                    Text(
                                        period.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AuraSpacing.md)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.handleIntent(BudgetViewIntent.CancelEdit) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = { viewModel.handleIntent(BudgetViewIntent.SaveBudget) },
                        modifier = Modifier.weight(1f),
                        enabled = state.selectedCategoryId.isNotBlank() && state.limitAmount.toDoubleOrNull() != null
                    ) {
                        Text(stringResource(R.string.budget_save))
                    }
                }
            }
        }
    }
}
