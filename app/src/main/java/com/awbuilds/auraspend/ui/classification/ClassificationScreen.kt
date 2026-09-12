package com.awbuilds.auraspend.ui.classification

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.R
import com.awbuilds.auraspend.data.ai.AiModelState
import com.awbuilds.auraspend.data.classification.ClassifiedSms
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.ui.core.isNotificationPermissionNeeded
import com.awbuilds.auraspend.ui.core.rememberNotificationPermissionLauncher
import com.awbuilds.auraspend.ui.designsystem.AnimatedMoney
import com.awbuilds.auraspend.ui.designsystem.AuraCard
import com.awbuilds.auraspend.ui.designsystem.AuraCardStyle
import com.awbuilds.auraspend.ui.designsystem.AuraEmptyState
import com.awbuilds.auraspend.ui.designsystem.AuraSegmentedControl
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing
import com.awbuilds.auraspend.ui.designsystem.AuraType
import com.awbuilds.auraspend.ui.designsystem.CategoryAvatar
import com.awbuilds.auraspend.ui.theme.extendedColors
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
                title = { Text(stringResource(R.string.classification_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_cancel)) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AuraSegmentedControl(
                options = listOf(
                    stringResource(R.string.classification_tab_paste),
                    stringResource(R.string.classification_tab_sms),
                    stringResource(R.string.classification_tab_auto)
                ),
                selectedIndex = selectedTab,
                onSelect = { index ->
                    selectedTab = index
                    when (index) {
                        1 -> if (!state.smsPermissionGranted) {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        } else {
                            viewModel.handleIntent(ClassificationViewIntent.LoadSmsMessages)
                        }
                        2 -> if (!state.smsPermissionGranted) {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        } else {
                            viewModel.handleIntent(ClassificationViewIntent.LoadAndClassifyAll)
                        }
                    }
                },
                modifier = Modifier.padding(
                    horizontal = AuraSpacing.gutter,
                    vertical = AuraSpacing.md
                )
            )

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
                Pair(state.parsedMessage?.merchant ?: stringResource(R.string.classification_unknown_merchant), state.merchantConfidence)
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
        contentPadding = PaddingValues(
            horizontal = AuraSpacing.gutter,
            vertical = AuraSpacing.sm
        ),
        verticalArrangement = Arrangement.spacedBy(AuraSpacing.lg)
    ) {
        item {
            OutlinedTextField(
                value = state.rawMessage,
                onValueChange = {
                    viewModel.handleIntent(ClassificationViewIntent.MessageChanged(it))
                },
                label = { Text(stringResource(R.string.classification_paste_label)) },
                placeholder = { Text(stringResource(R.string.classification_paste_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                minLines = 5,
                maxLines = 8,
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            Button(
                onClick = { viewModel.handleIntent(ClassificationViewIntent.ClassifyMessage) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = state.rawMessage.isNotBlank(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.classification_classify), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.classification_save_transaction), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                Pair(state.parsedMessage?.merchant ?: stringResource(R.string.classification_unknown_merchant), state.merchantConfidence)
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

    AuraCard(
        modifier = Modifier.fillMaxWidth(),
        style = AuraCardStyle.Tonal,
        contentPadding = PaddingValues(AuraSpacing.lg)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)
        ) {
            Text(
                stringResource(R.string.classification_detected_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            ConfidenceBadge(confidence = parsed.confidence)

            // Live AI status: refining in flight, or refined result applied.
            if (state.isAiEnriching && !state.aiRefined) {
                AiStatusBadge(text = stringResource(R.string.classification_ai_refining), tint = MaterialTheme.colorScheme.tertiary)
            } else if (state.aiRefined) {
                AiStatusBadge(text = stringResource(R.string.classification_ai_refined), tint = MaterialTheme.colorScheme.primary)
            }

            // Show merchant confidence if available
            if (state.merchantConfidence > 0) {
                MerchantConfidenceBadge(
                    merchant = parsed.merchant ?: stringResource(R.string.classification_unknown_merchant),
                    confidence = state.merchantConfidence
                )
            }

            AmountDetailRow(amount = parsed.amount)

            DetailRow(
                label = stringResource(R.string.classification_type),
                value = parsed.type?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: stringResource(R.string.classification_not_detected)
            )

            DetailRow(
                label = stringResource(R.string.classification_merchant),
                value = parsed.merchant ?: stringResource(R.string.classification_not_detected)
            )

            DetailRow(
                label = stringResource(R.string.classification_bank),
                value = parsed.bankName ?: stringResource(R.string.classification_not_detected)
            )

            Text(
                stringResource(R.string.classification_category),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
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
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("…", fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider()

            TextButton(
                onClick = { viewModel.handleIntent(ClassificationViewIntent.ToggleManualEntry) }
            ) {
                Text(
                    if (state.useManualEntry) stringResource(R.string.classification_use_detected)
                    else stringResource(R.string.classification_edit_manually)
                )
            }

            if (state.useManualEntry) {
                ManualEntryFields(state, viewModel)
            }
        }
    }
}

/**
 * Rounded tinted pill shared by the AI/confidence badges — one line, quiet,
 * and consistent with the badge style used across the app.
 */
@Composable
private fun TintBadge(text: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = AuraSpacing.md, vertical = AuraSpacing.xs)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@Composable
private fun AiStatusBadge(text: String, tint: Color) = TintBadge(text = text, tint = tint)

@Composable
private fun MerchantConfidenceBadge(merchant: String, confidence: Float) {
    val color = when {
        confidence >= 0.9f -> MaterialTheme.colorScheme.primary
        confidence >= 0.7f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    TintBadge(
        text = stringResource(R.string.classification_merchant_confidence, merchant, (confidence * 100).toInt()),
        tint = color
    )
}

@Composable
private fun ConfidenceBadge(confidence: Float) {
    val color = when {
        confidence >= 0.8f -> MaterialTheme.colorScheme.primary
        confidence >= 0.5f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val label = when {
        confidence >= 0.8f -> stringResource(R.string.classification_confidence_high)
        confidence >= 0.5f -> stringResource(R.string.classification_confidence_medium)
        else -> stringResource(R.string.classification_confidence_low)
    }

    TintBadge(text = label, tint = color)
}

@Composable
private fun AmountDetailRow(amount: Double?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.classification_amount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (amount != null) {
            AnimatedMoney(
                amount = amount,
                style = AuraType.moneySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = stringResource(R.string.classification_not_detected),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelector(
    categories: List<com.awbuilds.auraspend.domain.model.Category>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        Text(stringResource(R.string.classification_no_categories), style = MaterialTheme.typography.bodySmall)
        return
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
                label = {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    CategoryAvatar(
                        icon = category.icon,
                        color = Color(category.color.toLong()),
                        size = 22.dp
                    )
                },
                shape = RoundedCornerShape(50)
            )
        }
    }
}

@Composable
private fun ManualEntryFields(
    state: ClassificationViewState,
    viewModel: ClassificationViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)) {
        OutlinedTextField(
            value = state.manualAmount,
            onValueChange = { viewModel.handleIntent(ClassificationViewIntent.AmountChanged(it)) },
            label = { Text(stringResource(R.string.classification_amount)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            prefix = { Text("₹") },
            shape = RoundedCornerShape(14.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AuraSpacing.md)
        ) {
            FilterChip(
                selected = state.manualType == TransactionType.EXPENSE,
                onClick = { viewModel.handleIntent(ClassificationViewIntent.TypeChanged(TransactionType.EXPENSE)) },
                label = { Text(stringResource(R.string.classification_manual_expense)) },
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = state.manualType == TransactionType.INCOME,
                onClick = { viewModel.handleIntent(ClassificationViewIntent.TypeChanged(TransactionType.INCOME)) },
                label = { Text(stringResource(R.string.classification_manual_income)) },
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = state.manualMerchant,
            onValueChange = { viewModel.handleIntent(ClassificationViewIntent.MerchantChanged(it)) },
            label = { Text(stringResource(R.string.classification_manual_merchant)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
            value = state.manualNote,
            onValueChange = { viewModel.handleIntent(ClassificationViewIntent.NoteChanged(it)) },
            label = { Text(stringResource(R.string.classification_manual_note)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun SmsPermissionRequired(onGrant: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AuraEmptyState(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.classification_permission_title),
            message = stringResource(R.string.classification_permission_message),
            actionLabel = stringResource(R.string.classification_grant_permission),
            onAction = onGrant
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
        SmsPermissionRequired { permissionLauncher.launch(Manifest.permission.READ_SMS) }
    } else if (state.smsMessages.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AuraEmptyState(
                icon = Icons.Default.Sms,
                title = stringResource(R.string.classification_no_sms_title),
                message = stringResource(R.string.classification_no_sms_message)
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
                    stringResource(
                        R.string.classification_ai_progress,
                        state.aiProgressCurrent,
                        state.aiProgressTotal
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = AuraSpacing.gutter, vertical = AuraSpacing.xs)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    horizontal = AuraSpacing.gutter,
                    vertical = AuraSpacing.md
                ),
                verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
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
    AuraCard(
        modifier = Modifier.fillMaxWidth(),
        style = AuraCardStyle.Outlined,
        contentPadding = PaddingValues(AuraSpacing.md),
        onClick = onClick
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AuraSpacing.xs)
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
        SmsPermissionRequired { permissionLauncher.launch(Manifest.permission.READ_SMS) }
    } else if (!isModelDownloaded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AuraEmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource(R.string.classification_local_ai_title),
                    message = stringResource(R.string.classification_local_ai_message)
                )
                Button(
                    onClick = { viewModel.handleIntent(ClassificationViewIntent.StartModelDownload) },
                    enabled = state.aiModelState !is AiModelState.Downloading,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(horizontal = AuraSpacing.xxl)
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (state.aiModelState is AiModelState.Downloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(AuraSpacing.sm))
                        Text(stringResource(R.string.classification_downloading))
                    } else {
                        Text(stringResource(R.string.classification_download_model), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
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
                modifier = Modifier.padding(AuraSpacing.xxxl)
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(AuraSpacing.lg))
                Text(
                    if (state.isAiEnriching) stringResource(R.string.classification_ai_categorizing)
                    else stringResource(R.string.classification_reading_sms),
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
            AuraEmptyState(
                icon = Icons.Default.Sms,
                title = stringResource(R.string.classification_no_sms_title),
                message = stringResource(R.string.classification_scan_message),
                actionLabel = stringResource(R.string.classification_scan_sms),
                onAction = {
                    viewModel.handleIntent(ClassificationViewIntent.LoadAndClassifyAll)
                }
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
                    stringResource(
                        R.string.classification_ai_progress,
                        state.aiProgressCurrent,
                        state.aiProgressTotal
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = AuraSpacing.gutter, vertical = AuraSpacing.xs)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    horizontal = AuraSpacing.gutter,
                    vertical = AuraSpacing.md
                ),
                verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = AuraSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            pluralStringResource(
                                R.plurals.classification_messages_found,
                                state.classifiedSmsList.size,
                                state.classifiedSmsList.size
                            ),
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (state.classifiedSmsList.any { !it.isSaved }) {
                            Column(horizontalAlignment = Alignment.End) {
                                Button(
                                    onClick = {
                                        viewModel.handleIntent(ClassificationViewIntent.SaveAllClassified)
                                    },
                                    enabled = !state.isSavingAll,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    if (state.isSavingAll) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Text(stringResource(R.string.classification_save_all), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(AuraSpacing.xs))
                                Text(
                                    stringResource(R.string.classification_swipe_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                items(state.classifiedSmsList, key = { it.sms.id }) { classified ->
                    if (classified.isSaved) {
                        ClassifiedSmsItem(
                            classified = classified,
                            onSave = {},
                            onDismiss = {}
                        )
                    } else {
                        // Triage inbox: swipe right to save, left to dismiss.
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        viewModel.handleIntent(
                                            ClassificationViewIntent.SaveClassifiedSms(classified.sms.id)
                                        )
                                        true
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        viewModel.handleIntent(
                                            ClassificationViewIntent.DismissClassifiedSms(classified.sms.id)
                                        )
                                        true
                                    }
                                    else -> false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier.animateItem(),
                            backgroundContent = { TriageSwipeBackground(dismissState.dismissDirection) }
                        ) {
                            ClassifiedSmsItem(
                                classified = classified,
                                onSave = {
                                    viewModel.handleIntent(
                                        ClassificationViewIntent.SaveClassifiedSms(classified.sms.id)
                                    )
                                },
                                onDismiss = {
                                    viewModel.handleIntent(
                                        ClassificationViewIntent.DismissClassifiedSms(classified.sms.id)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TriageSwipeBackground(direction: SwipeToDismissBoxValue) {
    val isSave = direction == SwipeToDismissBoxValue.StartToEnd
    val container = when (direction) {
        SwipeToDismissBoxValue.StartToEnd ->
            MaterialTheme.extendedColors.incomeAmount.copy(alpha = 0.18f)
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }
    val contentColor = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.extendedColors.incomeAmount
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onErrorContainer
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(container),
        contentAlignment = if (isSave) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        if (direction != SwipeToDismissBoxValue.Settled) {
            Icon(
                imageVector = if (isSave) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (isSave) stringResource(R.string.classification_save_desc)
                else stringResource(R.string.classification_dismiss_desc),
                tint = contentColor,
                modifier = Modifier.padding(horizontal = AuraSpacing.xxl)
            )
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
    AuraCard(
        modifier = Modifier.fillMaxWidth(),
        style = if (classified.isSaved) AuraCardStyle.Tonal else AuraCardStyle.Outlined,
        contentPadding = PaddingValues(AuraSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AuraSpacing.xs)
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    parsed.amount?.let { amount ->
                        AnimatedMoney(
                            amount = amount,
                            style = AuraType.moneySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            animate = false
                        )
                    } ?: Text(
                        text = "₹0.00",
                        style = AuraType.moneySmall,
                        color = MaterialTheme.colorScheme.onSurface
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
                    verticalArrangement = Arrangement.spacedBy(AuraSpacing.xs),
                    modifier = Modifier.padding(start = AuraSpacing.sm)
                ) {
                    FilledTonalButton(
                        onClick = onSave,
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = stringResource(R.string.action_save),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_dismiss),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                TintBadge(
                    text = stringResource(R.string.classification_saved),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@Composable
private fun AiDownloadStatusBanner(progress: Float) {
    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AuraSpacing.gutter, vertical = AuraSpacing.sm),
        style = AuraCardStyle.Tonal,
        contentPadding = PaddingValues(AuraSpacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
            Text(
                stringResource(R.string.classification_download_progress, (progress * 100).toInt()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.classification_download_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
