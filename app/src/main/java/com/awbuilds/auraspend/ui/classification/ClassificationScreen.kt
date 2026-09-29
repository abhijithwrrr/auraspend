package com.awbuilds.auraspend.ui.classification

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.awbuilds.auraspend.ui.designsystem.AuraErrorBanner
import com.awbuilds.auraspend.ui.designsystem.AuraCardStyle
import com.awbuilds.auraspend.ui.designsystem.AuraEmptyState
import com.awbuilds.auraspend.ui.designsystem.AuraSegmentedControl
import com.awbuilds.auraspend.ui.designsystem.AuraSkeleton
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing
import com.awbuilds.auraspend.ui.designsystem.AuraType
import com.awbuilds.auraspend.ui.designsystem.CategoryAvatar
import com.awbuilds.auraspend.ui.theme.extendedColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)

// Main Smart Add screen: message input, classification result, and save.

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
                title = { Text(stringResource(R.string.classification_title), modifier = Modifier.semantics { heading() }) },
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
