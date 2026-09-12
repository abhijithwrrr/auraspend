package com.awbuilds.auraspend.ui.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import java.util.Locale
import kotlin.math.abs

/**
 * Locale-stable money formatter (Indian grouping, tabular-friendly).
 * Mirrors the app's `₹1,23,456` style without locale surprises.
 */
fun formatMoney(value: Double, currencySymbol: String = "₹"): String {
    val absValue = abs(value)
    val whole = absValue.toLong()
    val decimal = ((absValue - whole) * 100).toInt()
    return buildString {
        if (value < 0) append("-")
        append(currencySymbol)
        append(String.format(Locale.US, "%,d", whole))
        if (decimal != 0) append(".").append(String.format(Locale.US, "%02d", decimal))
    }
}

/**
 * Money text that counts from the previous value to the new one.
 *
 * Uses tabular figures, so the surrounding layout never jitters while the
 * number animates. Pass `animate = false` for values inside large lists where
 * dozens of simultaneous counters would be wasteful.
 */
@Composable
fun AnimatedMoney(
    amount: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = AuraType.moneyMedium,
    color: Color = LocalContentColor.current,
    currencySymbol: String = "₹",
    signed: Boolean = false,
    animate: Boolean = true,
    maxLines: Int = 1,
    textAlign: TextAlign? = null
) {
    val from = remember { AnimatableAmount(amount) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(amount, animate) {
        if (!animate || from.value == amount) {
            from.value = amount
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(AuraMotion.DURATION_SLOW, easing = FastOutSlowInEasing)
        )
        from.value = amount
    }

    val displayed = if (animate) from.value + (amount - from.value) * progress.value else amount
    val sign = if (signed) if (amount < 0) "-" else "+" else ""
    val magnitude = if (signed) abs(displayed) else displayed

    Text(
        text = sign + formatMoney(magnitude, currencySymbol),
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        textAlign = textAlign,
        overflow = TextOverflow.Ellipsis
    )
}

/** Tiny holder so the animation start value is stable across recompositions. */
private class AnimatableAmount(initial: Double) {
    var value by mutableStateOf(initial)
}
