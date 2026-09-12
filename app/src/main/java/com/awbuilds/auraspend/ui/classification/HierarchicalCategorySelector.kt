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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.hierarchicalCategoryMap
import com.awbuilds.auraspend.ui.designsystem.AuraCard
import com.awbuilds.auraspend.ui.designsystem.AuraCardStyle
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing

/**
 * Hierarchical category selector dialog showing parent and subcategories.
 *
 * Aurora pass: the dialog container is an [AuraCard] with the 28dp dialog radius,
 * spacing comes from [AuraSpacing], and the confirm action is a full-width 52dp
 * rounded CTA. Selection state and callbacks are unchanged.
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
        AuraCard(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            style = AuraCardStyle.Filled,
            shape = RoundedCornerShape(28.dp),
            contentPadding = PaddingValues(AuraSpacing.lg)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AuraSpacing.lg)
            ) {
                // Header
                Text(
                    "Select Category",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Show merchant suggestion if available
                if (merchantSuggestion != null) {
                    SuggestionChip(
                        onClick = { /* Info only */ },
                        label = {
                            Text("AI suggests: ${merchantSuggestion.first} (${(merchantSuggestion.second * 100).toInt()}% confident)")
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Category list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
                ) {
                    items(hierarchicalCategoryMap.keys.toList()) { parentCategory ->
                        val isExpanded = selectedParent == parentCategory

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedParent = if (isExpanded) null else parentCategory }
                                .padding(AuraSpacing.sm)
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
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.rotate(if (isExpanded) 90f else 0f)
                                )
                            }

                            // Subcategories
                            if (isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = AuraSpacing.lg, top = AuraSpacing.sm),
                                    verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
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
                                                .padding(AuraSpacing.sm)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Done", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
        shape = RoundedCornerShape(28.dp),
        title = {
            Text("Possible Duplicate Found")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)
            ) {
                Text(
                    "We found a similar transaction from ${
                        java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
                            .format(java.util.Date(transaction.date.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()))
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )

                AuraCard(
                    modifier = Modifier.fillMaxWidth(),
                    style = AuraCardStyle.Tonal,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(AuraSpacing.md)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
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
            Button(
                onClick = onMarkAsDuplicate,
                shape = RoundedCornerShape(16.dp)
            ) {
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
