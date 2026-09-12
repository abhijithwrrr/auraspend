package com.awbuilds.auraspend.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Subtle elevation for floating chrome (FAB, sheets) — never content cards. */
fun Modifier.softShadow(
    shape: Shape,
    elevation: Dp = 6.dp
): Modifier = composed {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    this.shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = if (dark) Color(0x2E000000) else Color(0x1A1C1B1F),
        spotColor = if (dark) Color(0x40000000) else Color(0x33201A24)
    )
}

private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue

/** Section header with an optional trailing slot. */
@Composable
fun AuraSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AuraSpacing.gutter, vertical = AuraSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        trailing?.invoke()
    }
}

/** Eye toggle that hides every money value on the screen. */
@Composable
fun HideAmountIconButton(
    hidden: Boolean,
    onToggle: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onToggle()
        }
    ) {
        Icon(
            imageVector = if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (hidden) "Show amounts" else "Hide amounts",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
