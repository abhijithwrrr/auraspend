package com.awbuilds.auraspend.ui.transaction

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    transactions: List<Transaction>,
    categories: List<com.awbuilds.auraspend.domain.model.Category>,
    onSearch: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<TransactionType?>(null) }
    var showSearch by remember { mutableStateOf(false) }

    val filteredTransactions = transactions
        .filter { selectedFilter == null || it.type == selectedFilter }
        .filter {
            searchQuery.isBlank() ||
                    it.note.contains(searchQuery, ignoreCase = true) ||
                    (it.merchant?.contains(searchQuery, ignoreCase = true) == true)
        }

    Scaffold(
        topBar = {
            if (showSearch) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it; onSearch(it) },
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    leadingIcon = {
                        IconButton(onClick = { showSearch = false; searchQuery = "" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; onSearch("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                ) { }
            } else {
                TopAppBar(
                    title = { Text("Transactions") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All") },
                    leadingIcon = if (selectedFilter == null) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                )
                FilterChip(
                    selected = selectedFilter == TransactionType.EXPENSE,
                    onClick = { selectedFilter = TransactionType.EXPENSE },
                    label = { Text("Expense") },
                    leadingIcon = if (selectedFilter == TransactionType.EXPENSE) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                )
                FilterChip(
                    selected = selectedFilter == TransactionType.INCOME,
                    onClick = { selectedFilter = TransactionType.INCOME },
                    label = { Text("Income") },
                    leadingIcon = if (selectedFilter == TransactionType.INCOME) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                )
            }

            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No transactions found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        if (searchQuery.isNotBlank() || selectedFilter != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Try adjusting your search or filter.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val grouped = groupTransactionsByDate(filteredTransactions)
                    grouped.forEach { (dateLabel, items) ->
                        item(key = "header_$dateLabel") {
                            Text(
                                dateLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(items, key = { it.id }) { transaction ->
                            val category = categories.find { it.id == transaction.categoryId }
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        onDelete(transaction.id)
                                        true
                                    } else false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier.animateItem(),
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small).padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                },
                                enableDismissFromStartToEnd = false
                            ) {
                                TransactionListItem(
                                    transaction = transaction,
                                    categoryName = category?.name ?: "Other",
                                    categoryColor = Color(category?.color?.toLong() ?: 0xFF757575)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionListItem(
    transaction: Transaction,
    categoryName: String,
    categoryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Circle, contentDescription = null, tint = categoryColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.merchant ?: transaction.note,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(categoryName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}₹${String.format("%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Text(
                    formatTransactionDate(transaction.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class DateGroup(val label: String, val transactions: List<Transaction>)

private fun groupTransactionsByDate(transactions: List<Transaction>): List<DateGroup> {
    val now = System.currentTimeMillis()
    val today = now - now % (24 * 60 * 60 * 1000)
    val yesterday = today - 24 * 60 * 60 * 1000
    val weekAgo = today - 7 * 24 * 60 * 60 * 1000
    val monthAgo = today - 30 * 24 * 60 * 60 * 1000

    val todayItems = mutableListOf<Transaction>()
    val yesterdayItems = mutableListOf<Transaction>()
    val thisWeekItems = mutableListOf<Transaction>()
    val thisMonthItems = mutableListOf<Transaction>()
    val olderItems = mutableListOf<Transaction>()

    transactions.forEach { t ->
        val ts = t.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        when {
            ts >= today -> todayItems.add(t)
            ts >= yesterday -> yesterdayItems.add(t)
            ts >= weekAgo -> thisWeekItems.add(t)
            ts >= monthAgo -> thisMonthItems.add(t)
            else -> olderItems.add(t)
        }
    }

    val groups = mutableListOf<DateGroup>()
    if (todayItems.isNotEmpty()) groups.add(DateGroup("Today", todayItems))
    if (yesterdayItems.isNotEmpty()) groups.add(DateGroup("Yesterday", yesterdayItems))
    if (thisWeekItems.isNotEmpty()) groups.add(DateGroup("This Week", thisWeekItems))
    if (thisMonthItems.isNotEmpty()) groups.add(DateGroup("This Month", thisMonthItems))
    if (olderItems.isNotEmpty()) groups.add(DateGroup("Older", olderItems))
    return groups
}

private fun formatTransactionDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
