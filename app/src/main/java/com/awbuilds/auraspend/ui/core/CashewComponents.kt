package com.awbuilds.auraspend.ui.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.ui.designsystem.AuraProgressRing
import com.awbuilds.auraspend.ui.designsystem.AuraSegmentedControl
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing
import com.awbuilds.auraspend.ui.designsystem.AuraType
import com.awbuilds.auraspend.ui.designsystem.AnimatedMoney
import com.awbuilds.auraspend.ui.designsystem.CategoryAvatar
import com.awbuilds.auraspend.ui.designsystem.AuraDonutChart
import com.awbuilds.auraspend.ui.designsystem.AuraSlice
import com.awbuilds.auraspend.ui.designsystem.categoryIconGlyph
import com.awbuilds.auraspend.ui.designsystem.formatMoney as designMoney
import com.awbuilds.auraspend.ui.theme.extendedColors
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

// ─── Formatting ───────────────────────────────────────────────────────────────
//
// NOTE: this file is the Phase 0 compatibility layer. Screens are migrated to
// the Aurora design system phase by phase (P1–P3); these helpers then go away.

fun formatMoney(value: Double, currencySymbol: String = "₹"): String =
    designMoney(value, currencySymbol)

fun formatRelativeDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 24 * 60 * 60 * 1000 -> "Today"
        diff < 48 * 60 * 60 * 1000 -> "Yesterday"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}

fun categoryColor(colorInt: Int?): Color =
    Color(colorInt?.toLong() ?: 0xFF757575L)

fun categoryIconEmoji(icon: String?): String = categoryIconGlyph(icon)

// ─── Soft shadow (floating chrome only) ───────────────────────────────────────

/**
 * Subtle elevation for floating chrome (FAB, sheets). Content cards no longer
 * use shadows — they use hairline borders via [CashewCard].
 */
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

// ─── Aurora card (compat wrapper) ─────────────────────────────────────────────

@Composable
fun CashewCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(AuraSpacing.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val isPlainSurface = containerColor == MaterialTheme.colorScheme.surface
    Column(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .then(
                if (isPlainSurface) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(contentPadding),
        content = content
    )
}

// ─── Section header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeaderRow(
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

@Composable
fun ViewAllButton(onClick: () -> Unit, label: String = "View All Transactions") {
    Text(
        text = label,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = AuraSpacing.xl, vertical = AuraSpacing.sm),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

// ─── Segmented selector ───────────────────────────────────────────────────────

@Composable
fun SlidingSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AuraSegmentedControl(
        options = options,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier
    )
}

// ─── Income / Expense summary boxes ───────────────────────────────────────────

@Composable
fun AmountSummaryBox(
    label: String,
    amount: Double,
    transactionCount: Int?,
    amountColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    CashewCard(
        modifier = modifier,
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(horizontal = AuraSpacing.lg, vertical = AuraSpacing.lg),
        onClick = onClick
    ) {
        Text(
            label,
            style = AuraType.metricLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(AuraSpacing.sm))
        AnimatedMoney(
            amount = amount,
            modifier = Modifier.fillMaxWidth(),
            style = AuraType.moneyLarge,
            color = amountColor,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        if (transactionCount != null) {
            Spacer(modifier = Modifier.height(AuraSpacing.xs))
            Text(
                "$transactionCount ${if (transactionCount == 1) "transaction" else "transactions"}",
                fontSize = 12.sp,
                color = MaterialTheme.extendedColors.textLight,
                maxLines = 1
            )
        }
    }
}

// ─── Category icon circle ─────────────────────────────────────────────────────

@Composable
fun CategoryIconCircle(
    emoji: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    ringStroke: Dp = 3.dp
) {
    CategoryAvatar(
        icon = emoji,
        color = color,
        modifier = modifier,
        size = size,
        showRing = true,
        ringStroke = ringStroke
    )
}

// ─── Transaction row ──────────────────────────────────────────────────────────

@Composable
fun TransactionEntryRow(
    transaction: Transaction,
    categoryName: String,
    categoryColor: Color,
    categoryEmoji: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val extended = MaterialTheme.extendedColors
    val amountColor =
        if (transaction.type == TransactionType.EXPENSE) extended.expenseAmount
        else extended.incomeAmount

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = AuraSpacing.gutter, vertical = AuraSpacing.sm + AuraSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryAvatar(
            icon = categoryEmoji,
            color = categoryColor,
            size = 46.dp,
            showRing = true,
            ringStroke = 1.5.dp
        )
        Spacer(modifier = Modifier.width(AuraSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.merchant ?: transaction.note.ifBlank { categoryName },
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                "$categoryName · ${formatRelativeDate(transaction.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())}",
                fontSize = 13.sp,
                color = extended.textLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(AuraSpacing.sm))
        Text(
            "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${formatMoney(transaction.amount)}",
            style = AuraType.moneySmall,
            color = amountColor
        )
    }
}

// ─── Progress ring / donut ────────────────────────────────────────────────────

@Composable
fun CircularProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    stroke: Dp = 4.dp,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable () -> Unit = {}
) {
    AuraProgressRing(
        progress = progress,
        color = color,
        modifier = modifier,
        stroke = stroke,
        trackColor = trackColor,
        content = content
    )
}

data class PieSliceData(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun DonutChart(
    data: List<PieSliceData>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 36.dp,
    gapDegrees: Float = 1.8f,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    centerContent: @Composable () -> Unit = {}
) {
    AuraDonutChart(
        data = data.map { AuraSlice(it.label, it.value, it.color) },
        modifier = modifier,
        strokeWidth = strokeWidth,
        gapDegrees = gapDegrees,
        trackColor = trackColor,
        centerContent = centerContent
    )
}

// ─── Amount visibility toggle ─────────────────────────────────────────────────

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
