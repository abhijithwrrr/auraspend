package com.awbuilds.auraspend.ui.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.domain.model.SavingsGoal
import com.awbuilds.auraspend.domain.repository.TransactionRepository
import com.awbuilds.auraspend.ui.designsystem.AuraCard
import com.awbuilds.auraspend.ui.designsystem.AuraCardStyle
import com.awbuilds.auraspend.ui.designsystem.AuraEmptyState
import com.awbuilds.auraspend.ui.designsystem.AuraProgressRing
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing
import com.awbuilds.auraspend.ui.designsystem.AuraType
import com.awbuilds.auraspend.ui.designsystem.AnimatedMoney
import com.awbuilds.auraspend.ui.designsystem.formatMoney
import com.awbuilds.auraspend.ui.theme.extendedColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Savings goals — the forward-looking half of the Plan hub. Progress rings
 * animate, milestones get a check badge, and adding funds is two taps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    repository: TransactionRepository,
    onBack: () -> Unit
) {
    val goals by repository.getAllSavingsGoals().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var editorGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var addFundsGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var deleteGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    val totalSaved = remember(goals) { goals.sumOf { it.currentAmount } }
    val totalTarget = remember(goals) { goals.sumOf { it.targetAmount } }
    val overallProgress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings goals") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editorGoal = null
                        showEditor = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add goal")
                    }
                }
            )
        }
    ) { padding ->
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                AuraEmptyState(
                    icon = Icons.Default.Savings,
                    title = "No savings goals yet",
                    message = "Set a target — a trip, a gadget, an emergency fund — and watch it fill up.",
                    actionLabel = "Create a goal",
                    onAction = {
                        editorGoal = null
                        showEditor = true
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(
                    start = AuraSpacing.gutter,
                    end = AuraSpacing.gutter,
                    bottom = AuraSpacing.xxl
                ),
                verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)
            ) {
                item {
                    AuraCard(style = AuraCardStyle.Tonal, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Total saved",
                                    style = AuraType.metricLabel,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(AuraSpacing.xs))
                                AnimatedMoney(
                                    amount = totalSaved,
                                    style = AuraType.moneyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "of ${formatMoney(totalTarget)}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.extendedColors.textLight
                                )
                            }
                            AuraProgressRing(
                                progress = overallProgress,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(72.dp),
                                stroke = 7.dp,
                                contentDescription = "Total savings ${(overallProgress * 100).toInt()} percent"
                            ) {
                                Text(
                                    "${(overallProgress * 100).toInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                items(goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        onAddFunds = { addFundsGoal = goal },
                        onEdit = {
                            editorGoal = goal
                            showEditor = true
                        },
                        onDelete = { deleteGoal = goal },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (showEditor) {
        GoalEditorSheet(
            goal = editorGoal,
            onSave = { goal ->
                scope.launch { repository.saveSavingsGoal(goal) }
                showEditor = false
            },
            onDismiss = { showEditor = false }
        )
    }

    addFundsGoal?.let { goal ->
        AddFundsDialog(
            goal = goal,
            onAdd = { amount ->
                scope.launch {
                    repository.saveSavingsGoal(goal.copy(currentAmount = goal.currentAmount + amount))
                }
                addFundsGoal = null
            },
            onDismiss = { addFundsGoal = null }
        )
    }

    deleteGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { deleteGoal = null },
            title = { Text("Delete “${goal.name}”?") },
            text = { Text("The goal and its progress will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { repository.deleteSavingsGoal(goal.id) }
                        deleteGoal = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteGoal = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun GoalCard(
    goal: SavingsGoal,
    onAddFunds: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    val complete = progress >= 1f
    val deadlineLabel = remember(goal.deadline) {
        goal.deadline?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        }
    }

    AuraCard(style = AuraCardStyle.Outlined, modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AuraProgressRing(
                progress = progress,
                color = if (complete) MaterialTheme.extendedColors.incomeAmount else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
                stroke = 5.dp,
                contentDescription = "${goal.name} ${(progress.coerceIn(0f, 1f) * 100).toInt()} percent"
            ) {
                if (complete) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.extendedColors.incomeAmount,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.width(AuraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        goal.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (complete) {
                        Spacer(modifier = Modifier.width(AuraSpacing.sm))
                        Text(
                            "Reached",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.extendedColors.incomeAmount,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.extendedColors.incomeAmount.copy(alpha = 0.14f),
                                    CircleShape
                                )
                                .padding(horizontal = AuraSpacing.sm, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AuraSpacing.xxs))
                Text(
                    "${formatMoney(goal.currentAmount)} of ${formatMoney(goal.targetAmount)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                deadlineLabel?.let {
                    Text(
                        "Target $it",
                        fontSize = 12.sp,
                        color = MaterialTheme.extendedColors.textLight
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(AuraSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
        ) {
            Button(
                onClick = onAddFunds,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(AuraSpacing.xs))
                Text("Add funds")
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit goal")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete goal",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalEditorSheet(
    goal: SavingsGoal?,
    onSave: (SavingsGoal) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(goal) { mutableStateOf(goal?.name ?: "") }
    var target by remember(goal) { mutableStateOf(goal?.targetAmount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var deadlineMillis by remember(goal) { mutableStateOf(goal?.deadline) }
    var showDatePicker by remember { mutableStateOf(false) }

    val targetValue = target.toDoubleOrNull()
    val valid = name.isNotBlank() && targetValue != null && targetValue > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AuraSpacing.gutter)
                .padding(bottom = AuraSpacing.xxl)
        ) {
            Text(
                if (goal == null) "New goal" else "Edit goal",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AuraSpacing.lg))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Goal name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(AuraSpacing.md))
            OutlinedTextField(
                value = target,
                onValueChange = { target = it },
                label = { Text("Target amount") },
                prefix = { Text("₹") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(AuraSpacing.md))
            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    deadlineMillis?.let {
                        "Target ${Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
                    } ?: "Add a target date (optional)"
                )
            }
            if (deadlineMillis != null) {
                TextButton(
                    onClick = { deadlineMillis = null },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text("Remove date") }
            }
            Spacer(modifier = Modifier.height(AuraSpacing.lg))
            Button(
                onClick = {
                    onSave(
                        (goal ?: SavingsGoal(name = "", targetAmount = 0.0)).copy(
                            name = name.trim(),
                            targetAmount = targetValue ?: 0.0,
                            deadline = deadlineMillis
                        )
                    )
                },
                enabled = valid,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (goal == null) "Create goal" else "Save changes", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = deadlineMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { deadlineMillis = it }
                        showDatePicker = false
                    }
                ) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun AddFundsDialog(
    goal: SavingsGoal,
    onAdd: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val parsed = amount.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to “${goal.name}”") },
        text = {
            Column {
                Text(
                    "${formatMoney(goal.currentAmount)} of ${formatMoney(goal.targetAmount)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AuraSpacing.md))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text("₹") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onAdd) },
                enabled = parsed != null && parsed > 0
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
