package com.awbuilds.auraspend.ui.classification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awbuilds.auraspend.R
import com.awbuilds.auraspend.data.ai.ModelConstants
import com.awbuilds.auraspend.ui.designsystem.AuraCard
import com.awbuilds.auraspend.ui.designsystem.AuraCardStyle
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing

/**
 * Consent dialog shown before the ~397 MB local AI model is downloaded. The download itself runs
 * in the background via [com.awbuilds.auraspend.data.ai.ModelDownloadManager].
 *
 * Aurora pass: 28dp dialog shape matching the app's sheet scale, the model name/size grouped in a
 * tonal card, and spacing on the [AuraSpacing] grid. Copy and actions are unchanged.
 */
@Composable
fun ModelConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val modelSizeMb = ModelConstants.EXPECTED_SIZE_BYTES / (1024 * 1024)

    AlertDialog(
        onDismissRequest = onDecline,
        shape = RoundedCornerShape(28.dp),
        icon = { Icon(Icons.Filled.Download, contentDescription = null) },
        title = { Text(stringResource(R.string.consent_title)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.consent_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(AuraSpacing.lg))
                AuraCard(
                    modifier = Modifier.fillMaxWidth(),
                    style = AuraCardStyle.Tonal,
                    contentPadding = PaddingValues(AuraSpacing.md)
                ) {
                    Text(
                        text = ModelConstants.MODEL_LABEL,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(AuraSpacing.xxs))
                    Text(
                        text = stringResource(R.string.consent_model_size, modelSizeMb),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(AuraSpacing.md))
                Text(
                    stringResource(R.string.consent_footnote),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text(stringResource(R.string.action_download)) }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text(stringResource(R.string.action_not_now)) }
        }
    )
}
