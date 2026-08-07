package com.awbuilds.auraspend.ui.classification

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardType.Companion.Decimal
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.rotate
import com.awbuilds.auraspend.data.ai.AiModelState
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.data.classification.ClassifiedSms
import com.awbuilds.auraspend.ui.core.isNotificationPermissionNeeded
import com.awbuilds.auraspend.ui.core.rememberNotificationPermissionLauncher
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

    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(state.showDuplicateDialog) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.handleIntent(ClassificationViewIntent.SmsPermissionResult(granted))
    }

    val notificationPermissionLauncher = rememberNotificationPermissionLauncher()

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

    LaunchedEffect(state.showDuplicateDialog) {
        showDuplicateDialog = state.showDuplicateDialog
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
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        if (!state.smsPermissionGranted) {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        } else {
                            viewModel.handleIntent(ClassificationViewIntent.LoadAndClassifyAll)
                        }
                    },
                    text = { Text("Auto Detect") }
                )
            }

            when (selectedTab) {
                0 -> PasteMessageTab(state, viewModel)
                1 -> SmsListTab(state, viewModel, smsPermissionLauncher)
                2 -> AutoDetectTab(state, viewModel, smsPermissionLauncher)
            }

            (state.aiModelState as? AiModelState.Downloading)?.let { d ->
                AiDownloadStatusBanner(progress = d.progress)
            }
        }
    }

    if (state.consentRequired) {
        ModelConsentDialog(
            onAccept = {
                if (isNotificationPermissionNeeded(context)) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.handleIntent(ClassificationViewIntent.ConsentResult(true))
            },
            onDecline = { viewModel.handleIntent(ClassificationViewIntent.ConsentResult(false)) }
        )
    }

    // Hierarchical category selector dialog
    if (showCategoryDialog) {
        HierarchicalCategoryDialog(
            categories = state.availableCategories,
            selectedCategoryId = state.selectedCategoryId,
            onCategorySelected = { id ->
                viewModel.handleIntent(ClassificationViewIntent.SelectCategory(id))
            },
            onDismiss = { showCategoryDialog = false },
            merchantSuggestion = if (state.merchantConfidence > 0) {
                Pair(state.parsedMessage?.merchant ?: "Unknown", state.merchantConfidence)
            } else null
        )
    }

    // Duplicate detection warning dialog
    val potentialDuplicate = state.potentialDuplicate
    if (showDuplicateDialog && potentialDuplicate != null) {
        DuplicateDetectionDialog(
            duplicateTransaction = potentialDuplicate,
            onIgnore = {
                viewModel.handleIntent(ClassificationViewIntent.IgnoreDuplicate)
            },
            onMarkAsDuplicate = {
                viewModel.handleIntent(ClassificationViewIntent.MarkAsDuplicate)
            },
            onDismiss = {
                viewModel.handleIntent(ClassificationViewIntent.IgnoreDuplicate)
            }
        )
    }
}

@Composable
private fun PasteMessageTab(
    state: ClassificationViewState,
    viewModel: ClassificationViewModel
) {
    var showCategoryDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
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
                ClassificationResultCard(
                    state = state,
                    viewModel = viewModel,
                    onOpenCategoryDialog = { showCategoryDialog = true }
                )
            }

            item {
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

            if (state.error != null) {
                item {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    if (showCategoryDialog) {
        HierarchicalCategoryDialog(
            categories = state.availableCategories,
            selectedCategoryId = state.selectedCategoryId,
            onCategorySelected = { id ->
                viewModel.handleIntent(ClassificationViewIntent.SelectCategory(id))
            },
            onDismiss = { showCategoryDialog = false },
            merchantSuggestion = if (state.merchantConfidence > 0) {
                Pair(state.parsedMessage?.merchant ?: "Unknown", state.merchantConfidence)
            } else null
        )
    }
}

@Composable
private fun ClassificationResultCard(
    state: ClassificationViewState,
    viewModel: ClassificationViewModel,
    onOpenCategoryDialog: () -> Unit = {}
) {
    val parsed = state.parsedMessage ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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

            // Show merchant confidence if available
            if (state.merchantConfidence > 0) {
                MerchantConfidenceBadge(
                    merchant = parsed.merchant ?: "Unknown",
                    confidence = state.merchantConfidence
                )
            }

            DetailRow(
                label = "Amount",
                value = parsed.amount?.let { "₹${String.format("%.2f", it)}" } ?: "Not detected",
                onEdit = { /* handled by manual entry */ }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategorySelector(
                    categories = state.availableCategories,
                    selectedCategoryId = state.selectedCategoryId,
                    onCategorySelected = { id ->
                        viewModel.handleIntent(ClassificationViewIntent.SelectCategory(id))
                    },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onOpenCategoryDialog,
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("…", fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider()

            TextButton(
                onClick = { viewModel.handleIntent(ClassificationViewIntent.ToggleManualEntry) }
            ) {
                Text(if (state.useManualEntry) "Use detected values" else "Edit manually")
            }

            if (state.useManualEntry) {
                ManualEntryFields(state, viewModel)
            }
        }
    }
}

@Composable
private fun MerchantConfidenceBadge(merchant: String, confidence: Float) {
    val color = when {
        confidence >= 0.9f -> MaterialTheme.colorScheme.primary
        confidence >= 0.7f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
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
                text = "  Merchant: $merchant (${(confidence * 100).toInt()}%)  ",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
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
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        Text("No categories available", style = MaterialTheme.typography.bodySmall)
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.isAiEnriching && state.aiProgressTotal > 0) {
                LinearProgressIndicator(
                    progress = { state.aiProgressCurrent.toFloat() / state.aiProgressTotal.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    "AI is categorizing: ${state.aiProgressCurrent} of ${state.aiProgressTotal}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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

@Composable
private fun AutoDetectTab(
    state: ClassificationViewState,
    viewModel: ClassificationViewModel,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val isModelDownloaded = state.aiModelState is AiModelState.Ready

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
    } else if (!isModelDownloaded) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Local AI Required",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Download the on-device AI model to automatically detect and categorize your bank messages offline.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.handleIntent(ClassificationViewIntent.StartModelDownload) },
                enabled = state.aiModelState !is AiModelState.Downloading
            ) {
                if (state.aiModelState is AiModelState.Downloading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Downloading…")
                } else {
                    Text("Download AI Model (~380 MB)")
                }
            }
        }
    } else if (state.isBatchClassifying || (state.isAiEnriching && state.classifiedSmsList.isEmpty())) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (state.isAiEnriching) "AI is categorizing messages..." else "Reading bank messages...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (state.classifiedSmsList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "No bank SMS messages found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.handleIntent(ClassificationViewIntent.LoadAndClassifyAll)
                    }
                ) {
                    Text("Scan SMS")
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.isAiEnriching && state.aiProgressTotal > 0) {
                LinearProgressIndicator(
                    progress = { state.aiProgressCurrent.toFloat() / state.aiProgressTotal.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    "AI is categorizing: ${state.aiProgressCurrent} of ${state.aiProgressTotal}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${state.classifiedSmsList.size} message${if (state.classifiedSmsList.size != 1) "s" else ""} found",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (state.classifiedSmsList.any { !it.isSaved }) {
                        Button(
                            onClick = {
                                viewModel.handleIntent(ClassificationViewIntent.SaveAllClassified)
                            },
                            enabled = !state.isSavingAll
                        ) {
                            if (state.isSavingAll) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Save All")
                            }
                        }
                    }
                }
            }
            items(state.classifiedSmsList, key = { it.sms.id }) { classified ->
                ClassifiedSmsItem(
                    classified = classified,
                    onSave = {
                        viewModel.handleIntent(ClassificationViewIntent.SaveClassifiedSms(classified.sms.id))
                    },
                    onDismiss = {
                        viewModel.handleIntent(ClassificationViewIntent.DismissClassifiedSms(classified.sms.id))
                    }
                )
            }
        }
    }
}
}

@Composable
private fun ClassifiedSmsItem(
    classified: ClassifiedSms,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val parsed = classified.parsed
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (classified.isSaved)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = classified.sms.address,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatTimestamp(classified.sms.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "₹${String.format("%.2f", parsed.amount ?: 0.0)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (parsed.type != null) {
                        Text(
                            text = "• ${parsed.type.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (parsed.merchant != null) {
                        Text(
                            text = "• ${parsed.merchant}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = classified.sms.body,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (!classified.isSaved) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = onSave,
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Save",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("x", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Text(
                    "Saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


@Composable
private fun AiDownloadStatusBanner(progress: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Downloading on-device AI… ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Messages will be auto-categorised into subscriptions, categories, income, expense and other once ready.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

