package com.awbuilds.auraspend.ui.core

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.ui.theme.extendedColors
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

// ─── Formatting ───────────────────────────────────────────────────────────────

fun formatMoney(value: Double, currencySymbol: String = "₹"): String {
    val absValue = kotlin.math.abs(value)
    val whole = absValue.toLong()
    val decimal = ((absValue - whole) * 100).toInt()
    return buildString {
        if (value < 0) append("-")
        append(currencySymbol)
        append(String.format(Locale.US, "%,d", whole))
        if (decimal != 0) append(".").append(String.format(Locale.US, "%02d", decimal))
    }
}

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

fun categoryIconEmoji(icon: String?): String =
    icon?.take(2)
        ?.takeIf { it.isNotBlank() && it.any { c -> c.code > 127 } }
        ?: "🏷️"

// ─── Soft-shadow card ─────────────────────────────────────────────────────────

fun Modifier.softShadow(
    shape: Shape,
    elevation: Dp = 7.dp
): Modifier = composed {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    this.shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = if (dark) Color(0x33000000) else Color(0x245A5A5A),
        spotColor = if (dark) Color(0x4D000000) else Color(0x525A5A5A)
    )
}

private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue

/**
 * Cashew-style elevated card: rounded surface with a soft drop shadow.
 */
@Composable
fun CashewCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .softShadow(shape)
            .clip(shape)
            .background(containerColor)
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
            .padding(horizontal = 13.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        trailing?.invoke()
    }
}

@Composable
fun ViewAllButton(onClick: () -> Unit, label: String = "View All Transactions") {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.extendedColors.textLight
        )
    }
}

// ─── Sliding selector (All | Outgoing | Incoming) ─────────────────────────────

@Composable
fun SlidingSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val segmentWidth = maxWidth / options.size
        val indicatorX by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "selectorIndicator"
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorX)
                .padding(4.dp)
                .width(segmentWidth - 8.dp)
                .fillMaxHeight()
                .softShadow(CircleShape, elevation = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        )
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.extendedColors.textLight
                    )
                }
            }
        }
    }
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
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 17.dp),
        onClick = onClick
    ) {
        Text(
            label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            formatMoney(amount),
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = amountColor
        )
        if (transactionCount != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "$transactionCount ${if (transactionCount == 1) "transaction" else "transactions"}",
                fontSize = 13.sp,
                color = MaterialTheme.extendedColors.textLight,
                maxLines = 1
            )
        }
    }
}

// ─── Category icon circle with ring ───────────────────────────────────────────

@Composable
fun CategoryIconCircle(
    emoji: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    ringStroke: Dp = 3.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = color,
                radius = this.size.minDimension / 2 - ringStroke.toPx() / 2,
                style = Stroke(width = ringStroke.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(emoji, fontSize = (size.value / 2.2f).sp)
    }
}

// ─── Transaction entry row (Cashew style) ─────────────────────────────────────

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
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconCircle(
            emoji = categoryEmoji,
            color = categoryColor,
            size = 50.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.merchant ?: transaction.note.ifBlank { categoryName },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "$categoryName · ${formatRelativeDate(transaction.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())}",
                fontSize = 14.sp,
                color = extended.textLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${formatMoney(transaction.amount)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

// ─── Circular progress ring ───────────────────────────────────────────────────

@Composable
fun CircularProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    stroke: Dp = 4.dp,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = progress.coerceIn(0f, 1f) * 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

// ─── Donut chart ──────────────────────────────────────────────────────────────

data class PieSliceData(
    val label: String,
    val value: Double,
    val color: Color
)

/**
 * Cashew-style donut chart: slices separated by thin gaps with optional
 * content (e.g. the period total) in the middle hole.
 */
@Composable
fun DonutChart(
    data: List<PieSliceData>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 36.dp,
    gapDegrees: Float = 1.8f,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    centerContent: @Composable () -> Unit = {}
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val total = data.sumOf { it.value }
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2
            val arcSize = Size(size.minDimension - strokePx, size.minDimension - strokePx)
            val topLeft = Offset(
                (size.width - arcSize.width) / 2,
                (size.height - arcSize.height) / 2
            )
            if (total <= 0.0 || data.isEmpty()) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokePx, cap = StrokeCap.Butt)
                )
                return@Canvas
            }
            var cursor = -90f
            data.forEach { slice ->
                val sweep = (slice.value / total * 360.0).toFloat()
                val drawableSweep = (sweep - gapDegrees).coerceAtLeast(0.5f)
                drawArc(
                    color = slice.color,
                    startAngle = cursor + gapDegrees / 2,
                    sweepAngle = drawableSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokePx, cap = StrokeCap.Butt)
                )
                cursor += sweep
            }
        }
        centerContent()
    }
}

// ─── Amount visibility toggle ─────────────────────────────────────────────────

@Composable
fun HideAmountIconButton(
    hidden: Boolean,
    onToggle: () -> Unit
) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (hidden) "Show amounts" else "Hide amounts",
            tint = MaterialTheme.extendedColors.textLight
        )
    }
}
