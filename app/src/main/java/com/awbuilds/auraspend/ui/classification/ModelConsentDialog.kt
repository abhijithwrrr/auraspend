package com.awbuilds.auraspend.ui.classification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awbuilds.auraspend.data.ai.ModelConstants

/**
 * Consent dialog shown before the ~397 MB local AI model is downloaded. The download itself runs
 * in the background via [com.awbuilds.auraspend.data.ai.ModelDownloadManager].
 */
@Composable
fun ModelConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val modelSizeMb = ModelConstants.EXPECTED_SIZE_BYTES / (1024 * 1024)

    AlertDialog(
        onDismissRequest = onDecline,
        icon = { Icon(Icons.Filled.Download, contentDescription = null) },
        title = { Text("Download local AI model?") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "To auto-categorise your messages into subscriptions, categories, income, " +
                        "expense and other use this on-device AI. It works fully offline once downloaded.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "\n\n${ModelConstants.MODEL_LABEL} (~${modelSizeMb} MB)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "The download happens in the background on your device. No data leaves your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("Download") }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text("Not now") }
        }
    )
}
