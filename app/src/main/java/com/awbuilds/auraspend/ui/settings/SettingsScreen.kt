package com.awbuilds.auraspend.ui.settings

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
    onManageBudgets: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
