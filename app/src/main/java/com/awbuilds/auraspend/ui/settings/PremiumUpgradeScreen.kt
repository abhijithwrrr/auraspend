package com.awbuilds.auraspend.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumUpgradeScreen(onUpgrade: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("AuraSpend+") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(animationSpec = tween(500), initialScale = 0.6f) + fadeIn(animationSpec = tween(500))
            ) {
                Text(
                    "Unlock the Full Experience",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 200)) + slideInVertically(
                    animationSpec = tween(500, delayMillis = 200),
                    initialOffsetY = { it / 2 }
                )
            ) {
                Text(
                    "Get Pure AMOLED Mode, Ad-Free Experience, and Advanced Analytics.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 400)) + scaleIn(
                    animationSpec = tween(500, delayMillis = 400),
                    initialScale = 0.8f
                )
            ) {
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Upgrade to Premium")
                }
            }
        }
    }
}
