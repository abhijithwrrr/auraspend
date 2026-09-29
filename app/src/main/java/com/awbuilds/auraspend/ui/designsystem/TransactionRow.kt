package com.awbuilds.auraspend.ui.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.ui.theme.extendedColors
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/** Brand color for a stored category ARGB value. */
fun categoryColor(colorInt: Int?): Color =
    Color(colorInt?.toLong() ?: 0xFF757575L)

/** Human date for a transaction timestamp. */
fun formatRelativeDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 24 * 60 * 60 * 1000 -> "Today"
        diff < 48 * 60 * 60 * 1000 -> "Yesterday"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * The transaction row used by Home and Activity. Merges semantics so TalkBack
 * reads merchant, category, date and amount as a single item.
 */
@Composable
fun TransactionEntryRow(
    transaction: Transaction,
    categoryName: String,
    categoryColor: Color,
    categoryEmoji: String?,
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
            .semantics(mergeDescendants = true) {}
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button) { onClick() }
                } else {
                    Modifier
                }
            )
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
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                "$categoryName · ${
                    formatRelativeDate(
                        transaction.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                }",
                style = MaterialTheme.typography.bodySmall,
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
