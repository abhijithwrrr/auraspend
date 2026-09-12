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
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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

/** Top-level destinations shown in the app chrome. */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home),
    ACTIVITY("activity", "Activity", Icons.AutoMirrored.Filled.ListAlt),
    PLAN("plan", "Plan", Icons.Filled.Savings),
    INSIGHTS("insights", "Insights", Icons.Filled.BarChart)
}

/**
 * Aurora app frame.
 *
 * Compact widths get a bottom navigation bar with a center add button; medium
 * and expanded widths get a navigation rail so tablets and foldables feel
 * native instead of stretched. Chrome is only shown on top-level destinations.
 */
@Composable
fun AuraAppChrome(
    showChrome: Boolean,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 600.dp

        if (!showChrome) {
            content()
            return@BoxWithConstraints
        }

        if (expanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    header = {
                        AddButton(
                            onClick = onAddClick,
                            modifier = Modifier.padding(top = AuraSpacing.md, bottom = AuraSpacing.sm)
                        )
                    }
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationRailItem(
                            selected = currentRoute == destination.route,
                            onClick = { onNavigate(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            alwaysShowLabel = true
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                Box(modifier = Modifier.weight(1f)) { content() }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) { content() }
                AuraBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    onAddClick = onAddClick
                )
            }
        }
    }
}

@Composable
private fun AuraBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit
) {
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
                    destination = TopLevelDestination.HOME,
                    selected = currentRoute == TopLevelDestination.HOME.route,
                    onClick = { onNavigate(TopLevelDestination.HOME.route) },
                    modifier = Modifier.weight(1f)
                )
                NavItem(
                    destination = TopLevelDestination.ACTIVITY,
                    selected = currentRoute == TopLevelDestination.ACTIVITY.route,
                    onClick = { onNavigate(TopLevelDestination.ACTIVITY.route) },
                    modifier = Modifier.weight(1f)
                )
                AddButton(onClick = onAddClick, modifier = Modifier.weight(1f))
                NavItem(
                    destination = TopLevelDestination.PLAN,
                    selected = currentRoute == TopLevelDestination.PLAN.route,
                    onClick = { onNavigate(TopLevelDestination.PLAN.route) },
                    modifier = Modifier.weight(1f)
                )
                NavItem(
                    destination = TopLevelDestination.INSIGHTS,
                    selected = currentRoute == TopLevelDestination.INSIGHTS.route,
                    onClick = { onNavigate(TopLevelDestination.INSIGHTS.route) },
                    modifier = Modifier.weight(1f)
                )
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
    destination: TopLevelDestination,
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
            Icon(destination.icon, contentDescription = destination.label, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(AuraSpacing.xs))
        Text(
            destination.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
            maxLines = 1
        )
    }
}
