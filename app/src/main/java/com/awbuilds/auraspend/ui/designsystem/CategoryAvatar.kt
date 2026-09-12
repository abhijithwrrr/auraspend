package com.awbuilds.auraspend.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders the category's icon string. Default categories store Material icon
 * names ("restaurant"); until the vector icon set lands in P2 we render an
 * emoji fallback in the brand avatar.
 */
fun categoryIconGlyph(icon: String?): String =
    icon?.take(2)
        ?.takeIf { it.isNotBlank() && it.any { c -> c.code > 127 } }
        ?: "🏷️"

/**
 * Circular category badge with an optional hairline ring.
 * The tonal fill keeps the row calm; the ring adds definition on busy screens.
 */
@Composable
fun CategoryAvatar(
    icon: String?,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showRing: Boolean = false,
    ringStroke: Dp = 1.5.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        if (showRing) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = ringStroke.toPx()
                drawCircle(
                    color = color.copy(alpha = 0.45f),
                    radius = this.size.minDimension / 2 - strokePx / 2,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = categoryIconGlyph(icon),
            fontSize = (size.value / 2.3f).sp
        )
    }
}
