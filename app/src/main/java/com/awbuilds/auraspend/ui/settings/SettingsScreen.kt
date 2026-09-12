package com.awbuilds.auraspend.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.data.ai.AiModelState
import com.awbuilds.auraspend.ui.core.CashewCard
import com.awbuilds.auraspend.ui.theme.extendedColors
import com.awbuilds.auraspend.ui.theme.AppThemeMode

@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    dynamicColor: Boolean = true,
    onDynamicColorChanged: (Boolean) -> Unit = {},
    onBack: () -> Unit = {},
    onExportCsv: () -> Unit = {},
    onImportCsv: () -> Unit = {},
    onManageCategories: () -> Unit = {},
    onManageSubscriptions: () -> Unit = {},
    onManageBudgets: () -> Unit = {},
    aiModelState: AiModelState = AiModelState.NotDownloaded,
    onDownloadModel: () -> Unit = {},
    onCancelModelDownload: () -> Unit = {},
    onDeleteModel: () -> Unit = {},
    autoDetectEnabled: Boolean = false,
    onAutoDetectChanged: (Boolean) -> Unit = {}
) {
    val extended = MaterialTheme.extendedColors
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Settings",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 13.dp, top = 10.dp, bottom = 4.dp)
        )

        // ── Appearance
        SettingsSectionHeader("APPEARANCE")
        Box(modifier = Modifier.padding(horizontal = 13.dp)) {
            CashewCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = extended.textLight
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("Theme", fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))
                SettingToggleRow(
                    icon = Icons.Default.Palette,
                    title = "Dynamic Color",
                    subtitle = "Use wallpaper-based colors",
                    checked = dynamicColor,
                    onCheckedChange = onDynamicColorChanged
                )
            }
        }

        // ── Data Management
        SettingsSectionHeader("DATA MANAGEMENT")
        Box(modifier = Modifier.padding(horizontal = 13.dp)) {
            CashewCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Default.FileDownload,
                    title = "Export to CSV",
                    subtitle = "Save transactions to a CSV file",
                    onClick = onExportCsv
                )
                DividerSpacer()
                SettingsRow(
                    icon = Icons.Default.FileUpload,
                    title = "Import from CSV",
                    subtitle = "Import transactions from a CSV file",
                    onClick = onImportCsv
                )
            }
        }

        // ── Intelligent Features
        SettingsSectionHeader("INTELLIGENT FEATURES")
        Box(modifier = Modifier.padding(horizontal = 13.dp)) {
            CashewCard(modifier = Modifier.fillMaxWidth()) {
                AiModelSection(
                    aiModelState = aiModelState,
                    onDownload = onDownloadModel,
                    onCancel = onCancelModelDownload,
                    onDelete = { showDeleteDialog = true }
                )
                DividerSpacer()
                AutoDetectSection(
                    enabled = autoDetectEnabled,
                    onToggle = onAutoDetectChanged
                )
            }
        }

        // ── Finance Management
        SettingsSectionHeader("FINANCE MANAGEMENT")
        Box(modifier = Modifier.padding(horizontal = 13.dp)) {
            CashewCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Default.Category,
                    title = "Manage Categories",
                    subtitle = "Add, edit, or reorder categories",
                    onClick = onManageCategories
                )
                DividerSpacer()
                SettingsRow(
                    icon = Icons.Default.Subscriptions,
                    title = "Manage Subscriptions",
                    subtitle = "Track your recurring subscriptions",
                    onClick = onManageSubscriptions
                )
                DividerSpacer()
                SettingsRow(
                    icon = Icons.Default.AccountBalance,
                    title = "Budget Settings",
                    subtitle = "Set spending limits per category",
                    onClick = onManageBudgets
                )
            }
        }

        // ── About
        SettingsSectionHeader("ABOUT")
        Box(modifier = Modifier.padding(horizontal = 13.dp)) {
            CashewCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "AuraSpend",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Version 0.1.0",
                        fontSize = 13.sp,
                        color = extended.textLight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Made with ❤️ by AW Builds",
                        fontSize = 13.sp,
                        color = extended.textLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.extendedColors.textLight,
        modifier = Modifier.padding(start = 15.dp, top = 22.dp, bottom = 8.dp)
    )
}

@Composable
private fun DividerSpacer() {
    Spacer(modifier = Modifier.height(4.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.extendedColors.textLight)
        }
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.extendedColors.textLight
        )
    }
}

@Composable
private fun SettingToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.extendedColors.textLight, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.extendedColors.textLight)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AiModelSection(
    aiModelState: AiModelState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val (title, subtitle) = when (aiModelState) {
        is AiModelState.Ready -> "Local AI ready" to "Messages are auto-categorised on-device."
        is AiModelState.Downloading -> "Downloading local AI" to "${(aiModelState.progress * 100).toInt()}% complete…"
        AiModelState.Failed -> "Download failed" to "Check your connection and try again."
        else -> "Local AI not downloaded" to "Download ~380 MB to auto-categorise messages into categories, income or expense."
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.extendedColors.textLight)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        when (aiModelState) {
            is AiModelState.Downloading -> {
                LinearProgressIndicator(
                    progress = { aiModelState.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
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

@Composable
private fun AutoDetectSection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    // Optimistic state: the switch flips immediately on tap; side effects (prefs write,
    // WorkManager enqueue, permission prompt) run afterwards without blocking recomposition.
    var checked by remember(enabled) { mutableStateOf(enabled) }
    SettingToggleRow(
        icon = Icons.Default.Sms,
        title = "Auto-categorize messages",
        subtitle = "Read new device SMS and save them automatically.",
        checked = checked,
        onCheckedChange = { next ->
            checked = next
            onToggle(next)
        }
    )
}
