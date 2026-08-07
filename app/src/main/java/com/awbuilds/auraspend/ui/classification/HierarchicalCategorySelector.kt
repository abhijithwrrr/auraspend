package com.awbuilds.auraspend.ui.classification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.hierarchicalCategoryMap

/**
 * Hierarchical category selector dialog showing parent and subcategories
 */
@Composable
fun HierarchicalCategoryDialog(
    categories: List<Category>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    merchantSuggestion: Pair<String, Float>? = null
) {
    var selectedParent by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    "Select Category",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                // Show merchant suggestion if available
                if (merchantSuggestion != null) {
                    SuggestionChip(
                        onClick = { /* Info only */ },
                        label = {
                            Text("AI suggests: ${merchantSuggestion.first} (${(merchantSuggestion.second * 100).toInt()}% confident)")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Category list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hierarchicalCategoryMap.keys.toList()) { parentCategory ->
                        val isExpanded = selectedParent == parentCategory

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedParent = if (isExpanded) null else parentCategory }
                                .padding(8.dp)
                        ) {
                            // Parent category
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    parentCategory,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.rotate(if (isExpanded) 90f else 0f)
                                )
                            }

                            // Subcategories
                            if (isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    hierarchicalCategoryMap[parentCategory]?.forEach { subcategory ->
                                        val localCategoryId = mapHierarchicalToLocalId(parentCategory, subcategory)
                                        val isSelected = localCategoryId == selectedCategoryId

                                        Text(
                                            subcategory,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier
                                                .clickable {
                                                    onCategorySelected(localCategoryId)
                                                    onDismiss()
                                                }
                                                .padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider()
                    }
                }

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}

/**
 * Map hierarchical category to local category ID
 */
private fun mapHierarchicalToLocalId(parentCategory: String, subcategory: String): String {
    return when {
        parentCategory.contains("Food", ignoreCase = true) -> "cat_food"
        parentCategory.contains("Transport", ignoreCase = true) -> "cat_transport"
        parentCategory.contains("Shopping", ignoreCase = true) -> "cat_shopping"
        parentCategory.contains("Entertainment", ignoreCase = true) -> "cat_entertainment"
        parentCategory.contains("Healthcare", ignoreCase = true) -> "cat_healthcare"
        parentCategory.contains("Bills", ignoreCase = true) -> "cat_bills"
        parentCategory.contains("Education", ignoreCase = true) -> "cat_education"
        parentCategory.contains("Subscription", ignoreCase = true) -> "cat_subscription"
        parentCategory.contains("Travel", ignoreCase = true) -> "cat_transport"
        parentCategory.contains("Personal", ignoreCase = true) -> "cat_personal_care"
        parentCategory.contains("Salary", ignoreCase = true) -> "cat_income"
        parentCategory.contains("Transfer", ignoreCase = true) -> "cat_transfer"
        else -> "cat_other"
    }
}

/**
 * Duplicate detection warning dialog
 */
@Composable
fun DuplicateDetectionDialog(
    duplicateTransaction: Pair<com.awbuilds.auraspend.domain.model.Transaction, Float>,
    onIgnore: () -> Unit,
    onMarkAsDuplicate: () -> Unit,
    onDismiss: () -> Unit
) {
    val (transaction, similarity) = duplicateTransaction

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Possible Duplicate Found")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "We found a similar transaction from ${
                        java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
                            .format(java.util.Date(transaction.date.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()))
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DialogDetailRow(
                            label = "Merchant",
                            value = transaction.merchant ?: "Unknown"
                        )
                        DialogDetailRow(
                            label = "Amount",
                            value = "₹${String.format("%.2f", transaction.amount)}"
                        )
                        DialogDetailRow(
                            label = "Similarity",
                            value = "${(similarity * 100).toInt()}%"
                        )
                    }
                }

                Text(
                    "Is this a duplicate transaction?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(onClick = onMarkAsDuplicate) {
                Text("Mark as Duplicate")
            }
        },
        dismissButton = {
            TextButton(onClick = onIgnore) {
                Text("Save Anyway")
            }
        }
    )
}

// Helper composable for displaying detail rows
@Composable
private fun DialogDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
