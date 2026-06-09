package com.awbuilds.auraspend.ui.classification

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.awbuilds.auraspend.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassificationScreen(
    viewModel: ClassificationViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.handleIntent(ClassificationViewIntent.SmsPermissionResult(granted))
    }

    LaunchedEffect(Unit) {
        if (state.smsPermissionGranted) {
            viewModel.handleIntent(ClassificationViewIntent.LoadSmsMessages)
        }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            onBack()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Smart Add") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Cancel") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Paste Message") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        if (!state.smsPermissionGranted) {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        } else {
                            viewModel.handleIntent(ClassificationViewIntent.LoadSmsMessages)
                        }
                    },
                    text = { Text("From SMS") }
                )
            }

            when (selectedTab) {
                0 -> PasteMessageTab(state, viewModel)
                1 -> SmsListTab(state, viewModel, smsPermissionLauncher)
            }
        }
    }
}

@Composable
private fun PasteMessageTab(
    state: ClassificationViewState,
    viewModel: ClassificationViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300))
            ) {
                OutlinedTextField(
                    value = state.rawMessage,
                    onValueChange = {
                        viewModel.handleIntent(ClassificationViewIntent.MessageChanged(it))
                    },
                    label = { Text("Paste bank SMS message") },
                    placeholder = { Text("Paste your bank SMS here...\ne.g. INR 500.00 debited from HDFC Bank for Swiggy order") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    minLines = 5,
                    maxLines = 8
                )
            }
        }

        item {
            Button(
                onClick = { viewModel.handleIntent(ClassificationViewIntent.ClassifyMessage) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.rawMessage.isNotBlank()
            ) {
                Text("Classify Message")
            }
        }

        if (state.parsedMessage != null) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                        animationSpec = tween(400),
                        initialOffsetY = { it / 3 }
                    )
                ) {
                    ClassificationResultCard(state, viewModel)
                }
            }

            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(400))
                ) {
                    Button(
                        onClick = { viewModel.handleIntent(ClassificationViewIntent.SaveTransaction) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save Transaction")
                        }
                    }
                }
            }

            if (state.error != null) {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(300))
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassificationResultCard(
    state: ClassificationViewState,
    viewModel: ClassificationViewModel
) {
    val parsed = state.parsedMessage ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Detected Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            ConfidenceBadge(confidence = parsed.confidence)

            DetailRow(
                label = "Amount",
                value = parsed.amount?.let { "₹${String.format("%.2f", it)}" } ?: "Not detected",
                onEdit = { }
            )

            DetailRow(
                label = "Type",
                value = parsed.type?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Not detected"
            )

            DetailRow(
                label = "Merchant",
                value = parsed.merchant ?: "Not detected"
            )

            DetailRow(
                label = "Bank",
                value = parsed.bankName ?: "Not detected"
            )

            Text(
                "Category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CategorySelector(
                categories = state.availableCategories,
                selectedCategoryId = state.selectedCategoryId,
                onCategorySelected = { id ->
                    viewModel.handleIntent(ClassificationViewIntent.SelectCategory(id))
                }
            )

            HorizontalDivider()

            TextButton(
                onClick = { viewModel.handleIntent(ClassificationViewIntent.ToggleManualEntry) }
            ) {
                Text(if (state.useManualEntry) "Use detected values" else "Edit manually")
            }

            AnimatedVisibility(
                visible = state.useManualEntry,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            ) {
                ManualEntryFields(state, viewModel)
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: Float) {
    val color = when {
        confidence >= 0.8f -> MaterialTheme.colorScheme.primary
        confidence >= 0.5f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val label = when {
        confidence >= 0.8f -> "High confidence"
        confidence >= 0.5f -> "Medium confidence"
        else -> "Low confidence - please verify"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = "  $label  ",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    onEdit: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CategorySelector(
    categories: List<com.awbuilds.auraspend.domain.model.Category>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit
) {
    if (categories.isEmpty()) {
        Text("No categories available", style = MaterialTheme.typography.bodySmall)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.name, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualEntryFields(
    state: ClassificationViewState,
    viewModel: ClassificationViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.manualAmount,
            onValueChange = { viewModel.handleIntent(ClassificationViewIntent.AmountChanged(it)) },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            prefix = { Text("₹") }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = state.manualType == TransactionType.EXPENSE,
                onClick = { viewModel.handleIntent(ClassificationViewIntent.TypeChanged(TransactionType.EXPENSE)) },
                label = { Text("Expense") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = state.manualType == TransactionType.INCOME,
                onClick = { viewModel.handleIntent(ClassificationViewIntent.TypeChanged(TransactionType.INCOME)) },
                label = { Text("Income") },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = state.manualMerchant,
            onValueChange = { viewModel.handleIntent(ClassificationViewIntent.MerchantChanged(it)) },
            label = { Text("Merchant / Payee") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.manualNote,
            onValueChange = { viewModel.handleIntent(ClassificationViewIntent.NoteChanged(it)) },
            label = { Text("Note") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
    }
}

@Composable
private fun SmsListTab(
    state: ClassificationViewState,
    viewModel: ClassificationViewModel,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    if (!state.smsPermissionGranted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "SMS Permission Required",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Allow access to read bank SMS messages for auto-classification.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) }) {
                Text("Grant Permission")
            }
        }
    } else if (state.smsMessages.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No bank SMS messages found.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.smsMessages) { sms ->
                SmsItem(
                    sms = sms,
                    onClick = {
                        viewModel.handleIntent(ClassificationViewIntent.SmsSelected(sms))
                    }
                )
            }
        }
    }
}

@Composable
private fun SmsItem(
    sms: SmsInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = sms.address,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatTimestamp(sms.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = sms.body,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
