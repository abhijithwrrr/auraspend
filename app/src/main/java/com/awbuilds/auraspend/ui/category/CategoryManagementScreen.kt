package com.awbuilds.auraspend.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.ui.designsystem.AuraCard
import com.awbuilds.auraspend.ui.designsystem.AuraCardStyle
import com.awbuilds.auraspend.ui.designsystem.AuraEmptyState
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    categories: List<Category>,
    onSaveCategory: (Category) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf<Category?>(null) }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                }
            )
        }
    ) { padding ->
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                AuraEmptyState(
                    icon = Icons.Default.Category,
                    title = "No categories",
                    message = "Add a category to organize your transactions."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    horizontal = AuraSpacing.gutter,
                    vertical = AuraSpacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
            ) {
                items(categories, key = { it.id }) { category ->
                    CategoryCard(
                        category = category,
                        onEdit = { editCategory = category },
                        onDelete = { deleteConfirmId = category.id }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        CategoryEditDialog(
            title = "Add Category",
            initialName = "",
            onSave = { name ->
                onSaveCategory(
                    Category(
                        name = name,
                        icon = "category",
                        color = 0xFF757575.toInt(),
                        isDefault = false
                    )
                )
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editCategory?.let { cat ->
        CategoryEditDialog(
            title = "Edit Category",
            initialName = cat.name,
            onSave = { name ->
                onSaveCategory(cat.copy(name = name))
                editCategory = null
            },
            onDismiss = { editCategory = null }
        )
    }

    deleteConfirmId?.let { id ->
        val cat = categories.find { it.id == id }
        AlertDialog(
            onDismissRequest = { deleteConfirmId = null },
            title = { Text("Delete Category") },
            text = {
                if (cat?.isDefault == true) {
                    Text("This is a default category and cannot be deleted.")
                } else {
                    Text("Delete \"${cat?.name ?: ""}\"? Transactions using this category will be affected.")
                }
            },
            confirmButton = {
                if (cat?.isDefault == true) {
                    TextButton(onClick = { deleteConfirmId = null }) {
                        Text("OK")
                    }
                } else {
                    TextButton(onClick = {
                        onDeleteCategory(id)
                        deleteConfirmId = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = Color(category.color.toLong())
    AuraCard(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        style = AuraCardStyle.Outlined
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(category.icon),
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(AuraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (category.isDefault) {
                    Spacer(modifier = Modifier.height(AuraSpacing.xxs))
                    Text(
                        "Default",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!category.isDefault) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun CategoryEditDialog(
    title: String,
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onSave(name.trim())
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun categoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "restaurant" -> Icons.Default.Restaurant
        "directions_car" -> Icons.Default.DirectionsCar
        "shopping_bag" -> Icons.Default.ShoppingBag
        "receipt_long" -> Icons.AutoMirrored.Filled.ReceiptLong
        "movie" -> Icons.Default.Movie
        "local_hospital" -> Icons.Default.LocalHospital
        "school" -> Icons.Default.School
        "account_balance" -> Icons.Default.AccountBalance
        "subscriptions" -> Icons.Default.Subscriptions
        "swap_horiz" -> Icons.Default.SwapHoriz
        "local_grocery_store" -> Icons.Default.LocalGroceryStore
        "category" -> Icons.Default.Category
        else -> Icons.Default.Category
    }
}
