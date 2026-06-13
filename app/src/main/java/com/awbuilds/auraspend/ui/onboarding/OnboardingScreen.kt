package com.awbuilds.auraspend.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: @Composable () -> Unit
)

val onboardingPages = listOf(
    OnboardingPage(
        "Track Your Wealth",
        "Manage your income and expenses effortlessly. Get insights into your spending habits.",
        icon = {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ),
    OnboardingPage(
        "Smart SMS Classification",
        "Paste bank messages and let AuraSpend auto-detect amounts, merchants, and categories.",
        icon = {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ),
    OnboardingPage(
        "Budgets & Subscriptions",
        "Set spending limits, track recurring subscriptions, and stay on top of your finances.",
        icon = {
            Icon(
                Icons.Default.Subscriptions,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    )
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onRestoreFromDrive: () -> Unit = {},
    isRestoring: Boolean = false,
    restoreError: String? = null,
    onRestoreErrorDismissed: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(restoreError) {
        restoreError?.let {
            snackbarHostState.showSnackbar(it)
            onRestoreErrorDismissed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button at top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinished, enabled = !isRestoring) {
                    Text("Skip")
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(onboardingPages[page], page)
            }

            // Page Indicator + Next/Get Started
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spacer for balance
                Spacer(modifier = Modifier.width(72.dp))

                PageIndicator(
                    currentPage = pagerState.currentPage,
                    totalPages = onboardingPages.size
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < onboardingPages.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                onFinished()
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.height(48.dp),
                    enabled = !isRestoring
                ) {
                    Text(
                        if (pagerState.currentPage == onboardingPages.size - 1) "Get Started" else "Next"
                    )
                }
            }

            // Restore from Drive - only on last page
            if (pagerState.currentPage == onboardingPages.size - 1) {
                TextButton(
                    onClick = { showRestoreConfirm = true },
                    enabled = !isRestoring
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Restore from Google Drive")
                }
            }
        }

        // Restore confirm dialog
        if (showRestoreConfirm) {
            AlertDialog(
                onDismissRequest = { showRestoreConfirm = false },
                title = { Text("Restore Data") },
                text = {
                    Text("This will replace all existing data with your Google Drive backup. Continue?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showRestoreConfirm = false
                            onRestoreFromDrive()
                        }
                    ) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Loading overlay
        if (isRestoring) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Restoring from Drive...")
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage, pageIndex: Int) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(pageIndex) {
        visible = false
        kotlinx.coroutines.delay(100)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon Circle
            Surface(
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    page.icon()
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun PageIndicator(currentPage: Int, totalPages: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalPages) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 24.dp else 8.dp, 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}
