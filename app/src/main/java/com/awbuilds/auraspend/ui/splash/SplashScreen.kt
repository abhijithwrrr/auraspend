package com.awbuilds.auraspend.ui.splash

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var footerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(400)
        subtitleVisible = true
        delay(600)
        footerVisible = true
        delay(1000)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(600)) + scaleIn(
                    animationSpec = androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    initialScale = 0.6f
                )
            ) {
                Text(
                    "AuraSpend",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = subtitleVisible,
                enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) + slideInVertically(
                    animationSpec = androidx.compose.animation.core.tween(500),
                    initialOffsetY = { it / 2 }
                )
            ) {
                Text(
                    "Your Personal Finance Manager",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        AnimatedVisibility(
            visible = footerVisible,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(400))
        ) {
            Text(
                "Made with ❤️ by AW Builds",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}
