package com.awbuilds.auraspend.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.awbuilds.auraspend.domain.model.BudgetPeriod
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.handleIntent(BudgetViewIntent.LoadBudgets)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.handleIntent(BudgetViewIntent.StartAdd) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Budget")
                    }
                }
            )
        }
    ) { padding ->
        if (state.editingBudget != null || state.selectedCategoryId.isNotEmpty()) {
            // Show add/edit form
            BudgetForm(state = state, viewModel = viewModel, modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.budgets.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No budgets set", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Set spending limits for each category.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.handleIntent(BudgetViewIntent.StartAdd) }) {
                                    Text("Add Budget")
                                }
                            }
                        }
                    }
                } else {
                    items(state.budgets) { budget ->
                        val category = state.categories.find { it.id == budget.categoryId }
                        val progress = if (budget.limitAmount > 0) (budget.spentAmount / budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
                        val progressColor = when { progress >= 1f -> MaterialTheme.colorScheme.error; progress >= 0.8f -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.primary }

                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(category?.color?.toLong() ?: 0xFF757575)))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(category?.name ?: "Unknown", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                    IconButton(onClick = { viewModel.handleIntent(BudgetViewIntent.DeleteBudget(budget.id)) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = progressColor, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("₹${String.format("%.0f", budget.spentAmount)} spent", style = MaterialTheme.typography.labelSmall)
                                    Text("₹${String.format("%.0f", budget.limitAmount)} limit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                }
                                Text("${budget.period.name.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetForm(
    state: BudgetViewState,
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (state.editingBudget != null) "Edit Budget" else "New Budget", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Text("Category", style = MaterialTheme.typography.labelLarge)
        if (state.categories.isEmpty()) {
            Text("No categories", style = MaterialTheme.typography.bodySmall)
        } else {
            state.categories.chunked(3).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { category ->
                        FilterChip(
                            selected = state.selectedCategoryId == category.id,
                            onClick = { viewModel.handleIntent(BudgetViewIntent.SelectCategory(category.id)) },
                            label = { Text(category.name, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.limitAmount,
            onValueChange = { viewModel.handleIntent(BudgetViewIntent.AmountChanged(it)) },
            label = { Text("Monthly Limit") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            prefix = { Text("₹") }
        )

        Text("Period", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BudgetPeriod.entries.forEach { period ->
                FilterChip(
                    selected = state.selectedPeriod == period,
                    onClick = { viewModel.handleIntent(BudgetViewIntent.PeriodChanged(period)) },
                    label = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.handleIntent(BudgetViewIntent.SaveBudget) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.selectedCategoryId.isNotBlank() && state.limitAmount.toDoubleOrNull() != null
        ) {
            Text("Save Budget")
        }
    }
}
