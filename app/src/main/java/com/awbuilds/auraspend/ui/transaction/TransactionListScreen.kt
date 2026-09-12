package com.awbuilds.auraspend.ui.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.R
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.ui.designsystem.AuraEmptyState
import com.awbuilds.auraspend.ui.designsystem.AuraSegmentedControl
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing
import com.awbuilds.auraspend.ui.designsystem.SettingsAvatarButton
import com.awbuilds.auraspend.ui.designsystem.TransactionEntryRow
import com.awbuilds.auraspend.ui.designsystem.categoryColor
import com.awbuilds.auraspend.ui.designsystem.categoryIconGlyph
import com.awbuilds.auraspend.ui.designsystem.formatMoney
import com.awbuilds.auraspend.ui.theme.extendedColors
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

private enum class TxnFilter(val labelRes: Int) {
    ALL(R.string.activity_filter_all),
    OUTGOING(R.string.activity_filter_outgoing),
    INCOMING(R.string.activity_filter_incoming)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    transactions: List<Transaction>,
    categories: List<com.awbuilds.auraspend.domain.model.Category>,
    onSearch: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    onRestore: (Transaction) -> Unit = {},
    onOpenTransaction: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    /** Aggregate-provided month totals; falls back to the loaded page when null. */
    monthTotalsOverride: Pair<Double, Double>? = null,
    hasMore: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(TxnFilter.ALL) }
    var showSearch by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val extended = MaterialTheme.extendedColors
    val transactionDeletedMessage = stringResource(R.string.activity_transaction_deleted)
    val undoLabel = stringResource(R.string.action_undo)

    val filteredTransactions = remember(transactions, filter, searchQuery, selectedCategoryId) {
        transactions
            .filter {
                when (filter) {
                    TxnFilter.OUTGOING -> it.type == TransactionType.EXPENSE
                    TxnFilter.INCOMING -> it.type == TransactionType.INCOME
                    TxnFilter.ALL -> true
                }
            }
            .filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
            .filter {
                searchQuery.isBlank() ||
                        it.note.contains(searchQuery, ignoreCase = true) ||
                        (it.merchant?.contains(searchQuery, ignoreCase = true) == true)
            }
    }

    val grouped = remember(filteredTransactions) { groupTransactionsByDate(filteredTransactions) }

    // Month-to-date flow context: SQL aggregate when provided, else the loaded page.
    val computedMonthTotals = remember(transactions) {
        val zone = ZoneId.systemDefault()
        val monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        var incoming = 0.0
        var outgoing = 0.0
        transactions.forEach { txn ->
            if (txn.date.atZone(zone).toInstant().toEpochMilli() >= monthStart) {
                if (txn.type == TransactionType.INCOME) incoming += txn.amount else outgoing += txn.amount
            }
        }
        incoming to outgoing
    }
    val (monthIn, monthOut) = monthTotalsOverride ?: computedMonthTotals

    fun deleteWithUndo(transaction: Transaction) {
        onDelete(transaction.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = transactionDeletedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onRestore(transaction)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            // ── Header
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; onSearch(it) },
                    placeholder = { Text(stringResource(R.string.activity_search_hint)) },
                    leadingIcon = {
                        IconButton(onClick = {
                            showSearch = false
                            searchQuery = ""
                            onSearch("")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; onSearch("") }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear))
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AuraSpacing.gutter, vertical = AuraSpacing.xs)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = AuraSpacing.gutter, end = AuraSpacing.sm, top = AuraSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.activity_title),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            if (hasMore) pluralStringResource(R.plurals.activity_records_more, transactions.size, transactions.size)
                            else pluralStringResource(R.plurals.activity_records, transactions.size, transactions.size),
                            fontSize = 13.sp,
                            color = extended.textLight
                        )
                    }
                    IconButton(onClick = { showSearch = true }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.action_search),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    SettingsAvatarButton(onClick = onOpenSettings)
                }
            }

            // ── Month-to-date summary strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AuraSpacing.gutter, vertical = AuraSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
            ) {
                SummaryPill(
                    label = stringResource(R.string.activity_summary_in),
                    value = stringResource(R.string.common_amount_plus, formatMoney(monthIn)),
                    valueColor = extended.incomeAmount,
                    modifier = Modifier.weight(1f)
                )
                SummaryPill(
                    label = stringResource(R.string.activity_summary_out),
                    value = stringResource(R.string.common_amount_minus, formatMoney(monthOut)),
                    valueColor = extended.expenseAmount,
                    modifier = Modifier.weight(1f)
                )
                SummaryPill(
                    label = stringResource(R.string.activity_summary_net),
                    value = formatMoney(monthIn - monthOut),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Type filter
            AuraSegmentedControl(
                options = TxnFilter.entries.map { stringResource(it.labelRes) },
                selectedIndex = filter.ordinal,
                onSelect = { filter = TxnFilter.entries[it] },
                modifier = Modifier.padding(horizontal = AuraSpacing.gutter)
            )

            // ── Category chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AuraSpacing.gutter, vertical = AuraSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
            ) {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { selectedCategoryId = null },
                    label = { Text(stringResource(R.string.activity_filter_all)) }
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = {
                            selectedCategoryId =
                                if (selectedCategoryId == category.id) null else category.id
                        },
                        leadingIcon = {
                            com.awbuilds.auraspend.ui.designsystem.CategoryAvatar(
                                icon = category.icon,
                                color = categoryColor(category.color),
                                size = 22.dp
                            )
                        },
                        label = {
                            Text(category.name, maxLines = 1)
                        }
                    )
                }
            }

            // ── List
            if (grouped.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AuraEmptyState(
                        icon = if (transactions.isEmpty()) Icons.Default.ReceiptLong else Icons.Default.SearchOff,
                        title = if (transactions.isEmpty()) stringResource(R.string.activity_empty_title)
                        else stringResource(R.string.activity_empty_title_filtered),
                        message = if (searchQuery.isNotBlank() || filter != TxnFilter.ALL || selectedCategoryId != null)
                            stringResource(R.string.activity_empty_message_filtered)
                        else stringResource(R.string.activity_empty_message)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AuraSpacing.gutter,
                        end = AuraSpacing.gutter,
                        bottom = AuraSpacing.xxl
                    )
                ) {
                    grouped.forEach { group ->
                        stickyHeader(key = "header_${group.label}") {
                            StickyGroupHeader(labelRes = group.labelRes, netAmount = group.netAmount)
                        }
                        items(
                            items = group.transactions,
                            key = { it.id },
                            contentType = { "transaction" }
                        ) { transaction ->
                            val category = categories.find { it.id == transaction.categoryId }
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        deleteWithUndo(transaction)
                                        true
                                    } else false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.errorContainer),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.action_delete),
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = AuraSpacing.xxl)
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false,
                                modifier = Modifier.animateItem()
                            ) {
                                TransactionEntryRow(
                                    transaction = transaction,
                                    categoryName = category?.name ?: stringResource(R.string.activity_other_category),
                                    categoryColor = categoryColor(category?.color),
                                    categoryEmoji = categoryIconGlyph(category?.icon),
                                    onClick = { onOpenTransaction(transaction.id) }
                                )
                            }
                        }
                    }
                    if (hasMore) {
                        item(key = "load_more") {
                            OutlinedButton(
                                onClick = onLoadMore,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AuraSpacing.md)
                            ) {
                                Text(stringResource(R.string.activity_load_earlier))
                            }
                        }
                    }
                }
            }
        }

        // Floating snackbar (above the chrome).
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AuraSpacing.xxl)
        )
    }
}

@Composable
private fun StickyGroupHeader(labelRes: Int, netAmount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = AuraSpacing.md, bottom = AuraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(labelRes),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.extendedColors.textLight,
            modifier = Modifier.weight(1f)
        )
        val sign = if (netAmount >= 0) "+" else "-"
        Text(
            "$sign${formatMoney(kotlin.math.abs(netAmount))}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (netAmount >= 0) MaterialTheme.extendedColors.incomeAmount
            else MaterialTheme.extendedColors.expenseAmount
        )
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = AuraSpacing.md, vertical = AuraSpacing.sm)
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.extendedColors.textLight, maxLines = 1)
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1
        )
    }
}

// ─── Grouping (local-calendar aware) ────────────────────────────────────────────

data class DateGroup(
    val label: String,
    val labelRes: Int,
    val netAmount: Double,
    val transactions: List<Transaction>
)

internal fun groupTransactionsByDate(
    transactions: List<Transaction>,
    today: LocalDate = LocalDate.now()
): List<DateGroup> {
    data class Bucket(
        val label: String,
        val labelRes: Int,
        val start: LocalDate?,
        val list: MutableList<Transaction> = mutableListOf()
    )

    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val monthStart = today.withDayOfMonth(1)

    val buckets = listOf(
        Bucket("Today", R.string.activity_group_today, today),
        Bucket("Yesterday", R.string.activity_group_yesterday, today.minusDays(1)),
        Bucket("This Week", R.string.activity_group_this_week, weekStart),
        Bucket("This Month", R.string.activity_group_this_month, monthStart),
        Bucket("Older", R.string.activity_group_older, null)
    )

    transactions.forEach { t ->
        val d = t.date.atZone(ZoneId.systemDefault()).toLocalDate()
        buckets.firstOrNull { b -> b.start != null && !d.isBefore(b.start) }?.list?.add(t)
            ?: buckets.last().list.add(t)
    }

    return buckets.filter { it.list.isNotEmpty() }.map { b ->
        val net = b.list.sumOf {
            if (it.type == TransactionType.EXPENSE) -it.amount else it.amount
        }
        DateGroup(b.label, b.labelRes, net, b.list)
    }
}
