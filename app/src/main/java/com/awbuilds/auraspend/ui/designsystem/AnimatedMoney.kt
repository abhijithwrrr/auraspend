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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import java.util.Locale
import kotlin.math.abs

/**
 * Money text that counts from the previous value to the new one.
 *
 * Uses tabular figures, so the surrounding layout never jitters while the number
 * animates. Pass `animate = false` for values inside large lists where dozens of
 * simultaneous counters would be wasteful.
 *
 * The rendered text is also exposed as the accessibility `contentDescription`,
 * because an animated counter is otherwise read out digit-by-digit by TalkBack.
 */
@Composable
fun AnimatedMoney(
    amount: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = AuraType.moneyMedium,
    color: Color = LocalContentColor.current,
    // Defaults to the theme's currency, NOT a hardcoded INR. A composable default
    // may call a composable, so this tracks the user's setting and the device locale
    // exactly as `formatMoney` does — the two must never disagree on one screen.
    currency: CurrencyStyle = LocalCurrencyStyle.current,
    locale: Locale = Locale.getDefault(),
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
    val text = sign + formatMoney(magnitude, currency, locale)

    Text(
        text = text,
        modifier = modifier.semantics { contentDescription = text },
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
