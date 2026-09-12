package com.awbuilds.auraspend.ui.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.ui.theme.extendedColors

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
 * Cashew-style bottom navigation: a flat surface bar with icon+label items and
 * a raised circular add button in the middle.
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
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp
            ) {
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
                    // Center add button (like Cashew's +)
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val addShape = CircleShape
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(addShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onAddClick
                                )
                                .padding(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .softShadow(addShape, elevation = 8.dp)
                                    .size(52.dp)
                                    .clip(addShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
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

@Composable
private fun NavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.extendedColors.textLight,
        animationSpec = tween(250),
        label = "navTint"
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            item.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            maxLines = 1
        )
    }
}

