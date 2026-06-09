package com.awbuilds.auraspend.ui.recurring

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.awbuilds.auraspend.domain.model.RecurrenceFrequency
import com.awbuilds.auraspend.domain.model.Subscription
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.domain.repository.TransactionRepository
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    repository: TransactionRepository,
    onBack: () -> Unit
) {
    val subscriptions by repository.getActiveSubscriptions().collectAsState(initial = emptyList())
    val categories by repository.getAllCategories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showAddSheet by remember { mutableStateOf(false) }
    var subscriptionName by remember { mutableStateOf("") }
    var subscriptionAmount by remember { mutableStateOf("") }
    var subscriptionCategory by remember { mutableStateOf("") }
    var subscriptionCycle by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Subscription")
                    }
                }
            )
        }
    ) { padding ->
        if (subscriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Subscriptions,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No subscriptions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Track your recurring payments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Monthly Total",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    "₹${String.format("%.0f", subscriptions.sumOf { it.amount })}/mo",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                Icons.Default.Subscriptions,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                items(subscriptions) { sub ->
                    val category = categories.find { it.id == sub.categoryId }
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(category?.color?.toLong() ?: 0xFF757575).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Subscriptions,
                                    contentDescription = null,
                                    tint = Color(category?.color?.toLong() ?: 0xFF757575),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    sub.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${category?.name ?: "Other"} · ${sub.billingCycle.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "₹${String.format("%.0f", sub.amount)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Next: ${sub.nextBillingDate.format(DateTimeFormatter.ofPattern("dd MMM"))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                scope.launch { repository.deleteSubscription(sub.id, false) }
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Subscriptions,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            "Add Subscription",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Track a recurring payment",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = subscriptionName,
                    onValueChange = { subscriptionName = it },
                    label = { Text("Name") },
                    placeholder = { Text("Netflix, Spotify, Rent...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                OutlinedTextField(
                    value = subscriptionAmount,
                    onValueChange = { subscriptionAmount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = {
                        Icon(
                            Icons.Default.CurrencyRupee,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Billing Cycle",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecurrenceFrequency.entries.forEach { cycle ->
                            FilterChip(
                                selected = subscriptionCycle == cycle,
                                onClick = { subscriptionCycle = cycle },
                                label = {
                                    Text(
                                        cycle.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Category",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (categories.isEmpty()) {
                        Text(
                            "No categories available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        categories.take(6).forEach { cat ->
                            val catColor = Color(cat.color.toLong())
                            FilterChip(
                                selected = subscriptionCategory == cat.id,
                                onClick = { subscriptionCategory = cat.id },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(catColor)
                                        )
                                        Text(
                                            cat.name,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val amt = subscriptionAmount.toDoubleOrNull() ?: return@Button
                        if (subscriptionName.isBlank() || subscriptionCategory.isBlank()) return@Button
                        scope.launch {
                            repository.saveSubscription(
                                Subscription(
                                    name = subscriptionName,
                                    amount = amt,
                                    categoryId = subscriptionCategory,
                                    billingCycle = subscriptionCycle,
                                    nextBillingDate = LocalDateTime.now().plusMonths(1)
                                )
                            )
                        }
                        showAddSheet = false
                        subscriptionName = ""
                        subscriptionAmount = ""
                        subscriptionCategory = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = subscriptionName.isNotBlank() &&
                            subscriptionAmount.toDoubleOrNull() != null &&
                            subscriptionCategory.isNotBlank()
                ) {
                    Text("Add Subscription")
                }
            }
        }
    }
}
