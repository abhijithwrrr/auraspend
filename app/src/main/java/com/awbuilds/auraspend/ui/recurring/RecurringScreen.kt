package com.awbuilds.auraspend.ui.recurring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.awbuilds.auraspend.domain.model.RecurrenceFrequency
import com.awbuilds.auraspend.domain.model.Subscription
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.domain.repository.TransactionRepository
import com.awbuilds.auraspend.ui.core.ShimmerListItem
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    repository: TransactionRepository,
    onBack: () -> Unit
) {
    val subscriptions by repository.getActiveSubscriptions().collectAsState(initial = emptyList())
    val categories by repository.getAllCategories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
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
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Subscription")
                    }
                }
            )
        }
    ) { padding ->
        if (subscriptions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Subscriptions, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No subscriptions", style = MaterialTheme.typography.titleMedium)
                    Text("Track your recurring payments.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                            animationSpec = tween(400),
                            initialOffsetY = { it / 2 }
                        )
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Monthly Total", style = MaterialTheme.typography.labelMedium)
                                    Text("₹${String.format("%.0f", subscriptions.sumOf { it.amount })}/mo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.Default.Subscriptions, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }

                items(subscriptions, key = { it.id }) { sub ->
                    val category = categories.find { it.id == sub.categoryId }
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                            animationSpec = tween(300),
                            initialOffsetY = { it / 3 }
                        ),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(category?.color?.toLong() ?: 0xFF757575).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Subscriptions, contentDescription = null, tint = Color(category?.color?.toLong() ?: 0xFF757575), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sub.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("${category?.name ?: "Other"} · ${sub.billingCycle.name.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("₹${String.format("%.0f", sub.amount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Next: ${sub.nextBillingDate.format(DateTimeFormatter.ofPattern("dd MMM"))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    scope.launch { repository.deleteSubscription(sub.id, false) }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Subscription") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = subscriptionName, onValueChange = { subscriptionName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = subscriptionAmount, onValueChange = { subscriptionAmount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth(), singleLine = true, prefix = { Text("₹") })
                    Text("Billing Cycle", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecurrenceFrequency.entries.forEach { cycle ->
                            FilterChip(selected = subscriptionCycle == cycle, onClick = { subscriptionCycle = cycle }, label = { Text(cycle.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    categories.take(6).forEach { cat ->
                        FilterChip(selected = subscriptionCategory == cat.id, onClick = { subscriptionCategory = cat.id }, label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = subscriptionAmount.toDoubleOrNull() ?: return@TextButton
                    if (subscriptionName.isBlank() || subscriptionCategory.isBlank()) return@TextButton
                    scope.launch {
                        repository.saveSubscription(
                            Subscription(
                                name = subscriptionName,
                                amount = amt,
                                categoryId = subscriptionCategory,
                                billingCycle = subscriptionCycle,
                                nextBillingDate = java.time.LocalDateTime.now().plusMonths(1)
                            )
                        )
                    }
                    showAddDialog = false
                    subscriptionName = ""
                    subscriptionAmount = ""
                    subscriptionCategory = ""
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
