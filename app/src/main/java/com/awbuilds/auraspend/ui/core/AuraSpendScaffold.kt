package com.awbuilds.auraspend.ui.core

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    TRANSACTIONS("transactions", "Transactions", Icons.Filled.ListAlt, Icons.Outlined.ListAlt),
    ADD("add", "Add", Icons.Filled.Add, Icons.Outlined.Add),
    ANALYTICS("analytics", "Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

private val tabRoutes = BottomNavItem.entries.filter { it != BottomNavItem.ADD }.map { it.route }

@Composable
fun AuraSpendScaffold(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val currentIndex = tabRoutes.indexOf(currentRoute).coerceAtLeast(0)
    var previousIndex by remember { mutableIntStateOf(currentIndex) }

    LaunchedEffect(currentIndex) {
        previousIndex = currentIndex
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = {
                        val currentTabIdx = tabRoutes.indexOf(targetState).coerceAtLeast(0)
                        val prevTabIdx = tabRoutes.indexOf(initialState).coerceAtLeast(0)
                        val slideDirection = if (currentTabIdx > prevTabIdx) 1 else -1
                        (slideInHorizontally(
                            animationSpec = androidx.compose.animation.core.tween(300),
                            initialOffsetX = { fullWidth -> slideDirection * fullWidth / 4 }
                        ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(300)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = androidx.compose.animation.core.tween(300),
                                    targetOffsetX = { fullWidth -> -slideDirection * fullWidth / 4 }
                                ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                            )
                    },
                    label = "tab_content"
                ) {
                    content()
                }
            }

            NavigationBar(
                modifier = Modifier.navigationBarsPadding()
            ) {
                BottomNavItem.entries.forEach { item ->
                    when (item) {
                        BottomNavItem.ADD -> {
                            NavigationBarItem(
                                selected = false,
                                onClick = onAddClick,
                                icon = {
                                    FloatingActionButton(
                                        onClick = onAddClick,
                                        modifier = Modifier.size(48.dp),
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Add")
                                    }
                                },
                                label = { }
                            )
                        }
                        else -> {
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onNavigate(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }
}
