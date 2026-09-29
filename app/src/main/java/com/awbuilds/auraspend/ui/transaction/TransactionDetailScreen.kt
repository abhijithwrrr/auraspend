package com.awbuilds.auraspend.ui.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.R
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.domain.repository.TransactionRepository
import com.awbuilds.auraspend.ui.designsystem.AuraCard
import com.awbuilds.auraspend.ui.designsystem.AuraCardStyle
import com.awbuilds.auraspend.ui.designsystem.AuraEmptyState
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing
import com.awbuilds.auraspend.ui.designsystem.AuraType
import com.awbuilds.auraspend.ui.designsystem.AnimatedMoney
import com.awbuilds.auraspend.ui.designsystem.CategoryAvatar
import com.awbuilds.auraspend.ui.designsystem.formatMoney
import com.awbuilds.auraspend.ui.theme.extendedColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Transaction detail — view, edit, duplicate and delete without leaving the
 * screen. This is the destination every row in the app opens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: String,
    repository: TransactionRepository,
    onBack: () -> Unit,
    onOpenTransaction: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val transactionUpdatedMessage = stringResource(R.string.txn_detail_updated)
    val duplicatedMessage = stringResource(R.string.txn_detail_duplicated)

    var transaction by remember { mutableStateOf<Transaction?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }

    LaunchedEffect(transactionId) {
        val all = repository.getAllTransactions().first()
        transaction = all.find { it.id == transactionId }
        loaded = true
    }
    LaunchedEffect(Unit) {
        categories = repository.getAllCategories().first()
    }

    val snackbarMessage = { message: String ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing) stringResource(R.string.txn_detail_edit_title) else stringResource(R.string.txn_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = { if (editing) editing = false else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (!editing) {
                        IconButton(onClick = { editing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val current = transaction
        when {
            !loaded -> Box(Modifier.fillMaxSize().padding(padding))
            current == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                AuraEmptyState(
                    icon = Icons.Default.Edit,
                    title = stringResource(R.string.txn_detail_not_found_title),
                    message = stringResource(R.string.txn_detail_not_found_message),
                    actionLabel = stringResource(R.string.txn_detail_go_back),
                    onAction = onBack
                )
            }
            editing -> TransactionEditor(
                transaction = current,
                categories = categories,
                isNew = false,
                onCancel = { editing = false },
                onSave = { updated ->
                    scope.launch {
                        repository.saveTransaction(updated)
                        transaction = updated
                        editing = false
                        snackbarMessage(transactionUpdatedMessage)
                    }
                },
                modifier = Modifier.padding(padding)
            )
            else -> TransactionDetails(
                transaction = current,
                category = categories.find { it.id == current.categoryId },
                modifier = Modifier.padding(padding),
                onDuplicate = {
                    val copy = current.copy(
                        id = UUID.randomUUID().toString(),
                        date = LocalDateTime.now(),
                        sourceSmsId = null
                    )
                    scope.launch {
                        repository.saveTransaction(copy)
                        snackbarMessage(duplicatedMessage)
                        onOpenTransaction(copy.id)
                    }
                }
            )
        }
    }

    if (confirmDelete) {
        val current = transaction
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.txn_detail_delete_title), modifier = Modifier.semantics { heading() }) },
            text = {
                Text(
                    current?.let {
                        stringResource(
                            R.string.txn_detail_delete_message,
                            it.merchant ?: it.note.ifBlank { stringResource(R.string.txn_detail_this_transaction) },
                            formatMoney(it.amount)
                        )
                    } ?: stringResource(R.string.txn_detail_delete_fallback)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        scope.launch {
                            current?.let { repository.deleteTransaction(it.id) }
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun TransactionDetails(
    transaction: Transaction,
    category: Category?,
    modifier: Modifier = Modifier,
    onDuplicate: () -> Unit
) {
    val extended = MaterialTheme.extendedColors
    val isExpense = transaction.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) extended.expenseAmount else extended.incomeAmount
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMM yyyy · h:mm a") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AuraSpacing.gutter)
    ) {
        Spacer(modifier = Modifier.height(AuraSpacing.md))

        AuraCard(style = AuraCardStyle.Tonal, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryAvatar(
                    icon = category?.icon,
                    color = androidx.compose.ui.graphics.Color(category?.color?.toLong() ?: 0xFF757575),
                    size = 52.dp,
                    showRing = true
                )
                Spacer(modifier = Modifier.width(AuraSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        category?.name ?: stringResource(R.string.txn_detail_uncategorized),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (isExpense) stringResource(R.string.txn_detail_type_expense) else stringResource(R.string.txn_detail_type_income),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(AuraSpacing.lg))
            AnimatedMoney(
                amount = if (isExpense) -transaction.amount else transaction.amount,
                style = AuraType.moneyHero,
                color = amountColor,
                signed = true
            )
            Spacer(modifier = Modifier.height(AuraSpacing.xs))
            Text(
                transaction.date.atZone(ZoneId.systemDefault()).format(dateFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(AuraSpacing.lg))

        AuraCard(style = AuraCardStyle.Outlined, modifier = Modifier.fillMaxWidth()) {
            DetailRow(stringResource(R.string.txn_detail_merchant), transaction.merchant ?: stringResource(R.string.txn_detail_dash))
            DetailRow(stringResource(R.string.txn_detail_note), transaction.note.ifBlank { stringResource(R.string.txn_detail_dash) })
            DetailRow(stringResource(R.string.txn_detail_bank), transaction.bankName ?: stringResource(R.string.txn_detail_dash))
            if (transaction.isRecurring) {
                DetailRow(stringResource(R.string.txn_detail_recurring), transaction.subscriptionName ?: stringResource(R.string.txn_detail_yes))
            }
            if (transaction.sourceSmsId != null) {
                DetailRow(stringResource(R.string.txn_detail_source), stringResource(R.string.txn_detail_source_sms))
            }
        }

        Spacer(modifier = Modifier.height(AuraSpacing.lg))

        OutlinedButton(
            onClick = onDuplicate,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(AuraSpacing.sm))
            Text(stringResource(R.string.action_duplicate))
        }

        Spacer(modifier = Modifier.height(AuraSpacing.xxl))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AuraSpacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}
