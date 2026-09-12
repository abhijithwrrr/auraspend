package com.awbuilds.auraspend.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.data.ai.AiModelState
import com.awbuilds.auraspend.ui.designsystem.AuraCard
import com.awbuilds.auraspend.ui.designsystem.AuraCardStyle
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing
import com.awbuilds.auraspend.ui.theme.AmoledBackground
import com.awbuilds.auraspend.ui.theme.AppThemeMode
import com.awbuilds.auraspend.ui.theme.AuroraCream
import com.awbuilds.auraspend.ui.theme.AuroraInk
import com.awbuilds.auraspend.ui.theme.AuroraLavenderLight
import com.awbuilds.auraspend.ui.theme.AuroraPurple
import com.awbuilds.auraspend.ui.theme.extendedColors

private const val GITHUB_URL = "https://github.com/auraspend/auraspend"

@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    dynamicColor: Boolean = false,
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
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete AI model?") },
            text = { Text("This removes the local model (~380 MB). You can re-download it anytime; your transactions are never affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteModel()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = AuraSpacing.gutter, top = AuraSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        SettingsSectionHeader("Appearance")
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(AuraSpacing.md))
                Text("Theme", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(AuraSpacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(AuraSpacing.md)) {
                ThemePreviewTile(
                    label = "Light",
                    background = AuroraCream,
                    accent = AuroraPurple,
                    selected = currentTheme == AppThemeMode.LIGHT,
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeChanged(AppThemeMode.LIGHT) }
                )
                ThemePreviewTile(
                    label = "Dark",
                    background = AuroraInk,
                    accent = AuroraLavenderLight,
                    selected = currentTheme == AppThemeMode.DARK,
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeChanged(AppThemeMode.DARK) }
                )
                ThemePreviewTile(
                    label = "AMOLED",
                    background = AmoledBackground,
                    accent = AuroraLavenderLight,
                    selected = currentTheme == AppThemeMode.AMOLED,
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeChanged(AppThemeMode.AMOLED) }
                )
            }
            Spacer(modifier = Modifier.height(AuraSpacing.lg))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(AuraSpacing.md))
            SettingToggleRow(
                icon = Icons.Default.Palette,
                title = "Dynamic color",
                subtitle = "Match your wallpaper (Android 12+)",
                checked = dynamicColor,
                onCheckedChange = onDynamicColorChanged
            )
        }

        SettingsSectionHeader("Intelligent features")
        SettingsCard {
            AiModelSection(
                aiModelState = aiModelState,
                onDownload = onDownloadModel,
                onCancel = onCancelModelDownload,
                onDelete = { showDeleteDialog = true }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(AuraSpacing.sm))
            AutoDetectSection(
                enabled = autoDetectEnabled,
                onToggle = onAutoDetectChanged
            )
        }

        SettingsSectionHeader("Data")
        SettingsCard {
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
                subtitle = "Restore transactions from a CSV file",
                onClick = onImportCsv
            )
        }

        SettingsSectionHeader("Finance")
        SettingsCard {
            SettingsRow(
                icon = Icons.Default.Category,
                title = "Categories",
                subtitle = "Add, edit, or organize categories",
                onClick = onManageCategories
            )
            DividerSpacer()
            SettingsRow(
                icon = Icons.Default.Subscriptions,
                title = "Subscriptions",
                subtitle = "Track recurring payments",
                onClick = onManageSubscriptions
            )
            DividerSpacer()
            SettingsRow(
                icon = Icons.Default.AccountBalance,
                title = "Budgets",
                subtitle = "Set spending limits per category",
                onClick = onManageBudgets
            )
        }

        SettingsSectionHeader("About")
        SettingsCard {
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
                    "Version 0.1.0 · Apache-2.0",
                    fontSize = 13.sp,
                    color = MaterialTheme.extendedColors.textLight
                )
                Spacer(modifier = Modifier.height(AuraSpacing.sm))
                Text(
                    "Made with ❤️ by AW Builds",
                    fontSize = 13.sp,
                    color = MaterialTheme.extendedColors.textLight
                )
                Spacer(modifier = Modifier.height(AuraSpacing.md))
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
                        )
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(AuraSpacing.sm))
                    Text("View on GitHub")
                }
            }
        }

        Spacer(modifier = Modifier.height(AuraSpacing.xxxl))
    }
}

// ─── Building blocks ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.extendedColors.textLight,
        modifier = Modifier.padding(start = AuraSpacing.gutter, top = AuraSpacing.xxl, bottom = AuraSpacing.sm)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.padding(horizontal = AuraSpacing.gutter)) {
        AuraCard(
            style = AuraCardStyle.Outlined,
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun DividerSpacer() {
    Spacer(modifier = Modifier.height(AuraSpacing.sm))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    Spacer(modifier = Modifier.height(AuraSpacing.sm))
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = AuraSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(AuraSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.extendedColors.textLight)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(AuraSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.extendedColors.textLight)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemePreviewTile(
    label: String,
    background: Color,
    accent: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "tileBorder"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(background)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(AuraSpacing.sm)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent)
                )
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent.copy(alpha = 0.55f))
                )
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent.copy(alpha = 0.35f))
                )
            }
        }
        Spacer(modifier = Modifier.height(AuraSpacing.xs))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = AuraSpacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(AuraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.extendedColors.textLight)
            }
        }
        Spacer(modifier = Modifier.height(AuraSpacing.md))
        when (aiModelState) {
            is AiModelState.Downloading -> {
                val animatedProgress by animateFloatAsState(
                    targetValue = aiModelState.progress,
                    label = "modelProgress"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(AuraSpacing.md))
                OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(14.dp)) { Text("Cancel") }
            }
            AiModelState.Ready -> {
                OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(14.dp)) { Text("Delete model") }
            }
            else -> {
                Button(onClick = onDownload, shape = RoundedCornerShape(14.dp)) { Text("Download model") }
            }
        }
    }
}

@Composable
private fun AutoDetectSection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    // Optimistic: the switch flips immediately; side effects run afterwards.
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
