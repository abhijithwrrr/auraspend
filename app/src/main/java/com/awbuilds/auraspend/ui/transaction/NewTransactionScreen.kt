package com.awbuilds.auraspend.ui.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.repository.TransactionRepository
import com.awbuilds.auraspend.ui.designsystem.AuraSkeleton
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Full-screen creation flow backed by the shared [TransactionEditor]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionScreen(
    repository: TransactionRepository,
    onBack: () -> Unit
) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        categories = repository.getAllCategories().first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (categories.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = AuraSpacing.gutter),
                verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)
            ) {
                Spacer(modifier = Modifier.height(AuraSpacing.md))
                AuraSkeleton(modifier = Modifier.fillMaxWidth().height(44.dp))
                AuraSkeleton(modifier = Modifier.fillMaxWidth().height(56.dp))
                AuraSkeleton(modifier = Modifier.fillMaxWidth().height(56.dp))
                AuraSkeleton(modifier = Modifier.fillMaxWidth().height(120.dp))
            }
        } else {
            TransactionEditor(
                transaction = null,
                categories = categories,
                isNew = true,
                onCancel = onBack,
                onSave = { transaction ->
                    scope.launch {
                        repository.saveTransaction(transaction)
                        onBack()
                    }
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
}
