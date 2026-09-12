package com.awbuilds.auraspend.ui.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.ui.designsystem.AuraGradients
import com.awbuilds.auraspend.ui.designsystem.AuraMotion
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing

enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home),
    TRANSACTIONS("transactions", "Transactions", Icons.Filled.ListAlt),
    ANALYTICS("analytics", "Stats", Icons.Filled.BarChart),
    SETTINGS("settings", "Settings", Icons.Filled.Settings)
}

/**
 * Aurora app frame: a flat, hairline-topped navigation bar with a raised
 * gradient add button. The selected tab gets a tonal pill and a spring, and
 * every tap carries a light haptic tick.
 */
@Composable
fun AuraSpendScaffold(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Hairline separation instead of a drop shadow.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(72.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavItem(
                            item = BottomNavItem.HOME,
                            selected = currentRoute == BottomNavItem.HOME.route,
                            onClick = { onNavigate(BottomNavItem.HOME.route) },
                            modifier = Modifier.weight(1f)
                        )
                        NavItem(
                            item = BottomNavItem.TRANSACTIONS,
                            selected = currentRoute == BottomNavItem.TRANSACTIONS.route,
                            onClick = { onNavigate(BottomNavItem.TRANSACTIONS.route) },
                            modifier = Modifier.weight(1f)
                        )
                        AddButton(
                            onClick = onAddClick,
                            modifier = Modifier.weight(1f)
                        )
                        NavItem(
                            item = BottomNavItem.ANALYTICS,
                            selected = currentRoute == BottomNavItem.ANALYTICS.route,
                            onClick = { onNavigate(BottomNavItem.ANALYTICS.route) },
                            modifier = Modifier.weight(1f)
                        )
                        NavItem(
                            item = BottomNavItem.SETTINGS,
                            selected = currentRoute == BottomNavItem.SETTINGS.route,
                            onClick = { onNavigate(BottomNavItem.SETTINGS.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = AuraMotion.snappy(),
        label = "addButtonScale"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .softShadow(CircleShape, elevation = 8.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(AuraGradients.aurora)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add transaction",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun NavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = AuraMotion.standard(),
        label = "navTint"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = AuraMotion.standard(),
        label = "navPill"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f * pillAlpha))
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(AuraSpacing.xs))
        Text(
            item.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
            maxLines = 1
        )
    }
}
