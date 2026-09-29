package com.awbuilds.auraspend.ui.classification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SmsFailed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.awbuilds.auraspend.R
import com.awbuilds.auraspend.data.local.UnrecognizedSmsRepository
import com.awbuilds.auraspend.data.local.entities.UnrecognizedSmsEntity
import com.awbuilds.auraspend.ui.designsystem.AuraCard
import com.awbuilds.auraspend.ui.designsystem.AuraCardStyle
import com.awbuilds.auraspend.ui.designsystem.AuraEmptyState
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing

/**
 * Bank messages the app could not turn into a transaction.
 *
 * This screen is the other half of a decision made in handoff 0012. The on-device
 * model was changed from a 468 MB generator to a 22 MB encoder partly because the
 * generator would *delete* real transactions — it called them "not a transaction"
 * and the pipeline dropped the row. This screen exists so that whatever the
 * classifier cannot read is visible to the person whose money it is, rather than
 * quietly absent from their history.
 *
 * Only the raw message is shown. No amount, merchant or category is displayed,
 * because nothing was reliably derived and a half-guessed figure presented as
 * fact is worse than no figure. The user adds the transaction by hand, where they
 * can see every field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnrecognizedSmsScreen(
    repository: UnrecognizedSmsRepository,
    onBack: () -> Unit,
    onFileManually: (UnrecognizedSmsEntity) -> Unit
) {
    val messages by repository.observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.unrecognized_title), modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (messages.isEmpty()) {
            EmptyStateBody(padding) {
                AuraEmptyState(
                    icon = Icons.Default.SmsFailed,
                    title = stringResource(R.string.unrecognized_empty_title),
                    message = stringResource(R.string.unrecognized_empty_message)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(AuraSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)
            ) {
                item {
                    Text(
                        stringResource(R.string.unrecognized_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = AuraSpacing.xs)
                    )
                }
                items(messages, key = { it.id }) { message ->
                    UnrecognizedSmsRow(
                        message = message,
                        onFile = { onFileManually(message) },
                        onDismiss = { scope.launch { repository.delete(message.id) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun UnrecognizedSmsRow(
    message: UnrecognizedSmsEntity,
    onFile: () -> Unit,
    onDismiss: () -> Unit
) {
    AuraCard(
        modifier = Modifier.fillMaxWidth(),
        style = AuraCardStyle.Tonal,
        contentPadding = PaddingValues(AuraSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.sender.ifBlank { stringResource(R.string.unrecognized_sender_unknown) },
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(AuraSpacing.xxs)) 
                // The raw body, verbatim. Not truncated on one line: a bank SMS
                // is multi-line by nature and truncating it defeats the point.
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.unrecognized_dismiss)
                )
            }
        }
        Spacer(modifier = Modifier.height(AuraSpacing.xs))
        TextButton(
            onClick = onFile,
            // heightIn, not height: the label must survive a 200% font scale.
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.unrecognized_open))
        }
    }
}

@Composable
private fun EmptyStateBody(
    padding: PaddingValues,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) { content() }
}
