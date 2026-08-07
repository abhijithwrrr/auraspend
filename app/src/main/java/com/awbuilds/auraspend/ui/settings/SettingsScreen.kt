package com.awbuilds.auraspend.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awbuilds.auraspend.data.ai.AiModelState
import com.awbuilds.auraspend.ui.theme.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    dynamicColor: Boolean = true,
    onDynamicColorChanged: (Boolean) -> Unit = {},
    onBack: () -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onManageCategories: () -> Unit,
    onManageSubscriptions: () -> Unit,
    onManageBudgets: () -> Unit,
    aiModelState: AiModelState = AiModelState.NotDownloaded,
    onDownloadModel: () -> Unit = {},
    onCancelModelDownload: () -> Unit = {},
    onDeleteModel: () -> Unit = {},
    autoDetectEnabled: Boolean = false,
    onAutoDetectChanged: (Boolean) -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete AI Model?") },
            text = { Text("This will delete the local AI model (~380 MB). You'll need to re-download it to use automatic categorization.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteModel()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Theme Section
            SectionHeader("Appearance")
            ThemeSelector(currentTheme = currentTheme, onThemeChanged = onThemeChanged)
            DynamicColorToggle(dynamicColor = dynamicColor, onDynamicColorChanged = onDynamicColorChanged)

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Data Management
            SectionHeader("Data Management")
            SettingsItem(
                icon = Icons.Default.FileDownload,
                title = "Export to CSV",
                subtitle = "Save transactions to a CSV file",
                onClick = onExportCsv
            )
            SettingsItem(
                icon = Icons.Default.FileUpload,
                title = "Import from CSV",
                subtitle = "Import transactions from a CSV file",
                onClick = onImportCsv
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // On-device AI
            SectionHeader("Intelligent Features")
            AiModelSettingsCard(
                aiModelState = aiModelState,
                onDownload = onDownloadModel,
                onCancel = onCancelModelDownload,
                onDelete = { showDeleteDialog = true }
            )
            AutoDetectSettingsCard(
                enabled = autoDetectEnabled,
                onToggle = onAutoDetectChanged
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Finance Management
            SectionHeader("Finance Management")
            SettingsItem(
                icon = Icons.Default.Category,
                title = "Manage Categories",
                subtitle = "Add, edit, or reorder categories",
                onClick = onManageCategories
            )
            SettingsItem(
                icon = Icons.Default.Subscriptions,
                title = "Manage Subscriptions",
                subtitle = "Track your recurring subscriptions",
                onClick = onManageSubscriptions
            )
            SettingsItem(
                icon = Icons.Default.AccountBalance,
                title = "Budget Settings",
                subtitle = "Set spending limits per category",
                onClick = onManageBudgets
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // About
            SectionHeader("About")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "AuraSpend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Version 0.1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Made with ❤️ by AW Builds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DynamicColorToggle(
    dynamicColor: Boolean,
    onDynamicColorChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Dynamic Color",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "Use wallpaper-based colors",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = dynamicColor,
                onCheckedChange = onDynamicColorChanged
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun ThemeSelector(
    currentTheme: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Theme",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = currentTheme == AppThemeMode.LIGHT,
                    onClick = { onThemeChanged(AppThemeMode.LIGHT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) { Text("Light") }
                SegmentedButton(
                    selected = currentTheme == AppThemeMode.DARK,
                    onClick = { onThemeChanged(AppThemeMode.DARK) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) { Text("Dark") }
                SegmentedButton(
                    selected = currentTheme == AppThemeMode.AMOLED,
                    onClick = { onThemeChanged(AppThemeMode.AMOLED) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) { Text("AMOLED") }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun AiModelSettingsCard(
    aiModelState: AiModelState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val (title, subtitle) = when (aiModelState) {
        is AiModelState.Ready -> "Local AI ready" to "Messages are auto-categorised on-device."
        is AiModelState.Downloading -> "Downloading local AI" to "${(aiModelState.progress * 100).toInt()}% complete…"
        AiModelState.Failed -> "Download failed" to "Check your connection and try again."
        else -> "Local AI not downloaded" to "Download ~380 MB to auto-categorise into subscriptions, categories, income, expense and other."
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            when (aiModelState) {
                is AiModelState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { aiModelState.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onCancel) { Text("Cancel") }
                }
                AiModelState.Ready -> {
                    OutlinedButton(onClick = onDelete) { Text("Delete model") }
                }
                else -> {
                    Button(onClick = onDownload) { Text("Download model") }
                }
            }
        }
    }
}


@Composable
private fun AutoDetectSettingsCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Sms,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-categorize messages", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Read new device SMS and save them as income / expense automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

