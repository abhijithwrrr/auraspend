package com.awbuilds.auraspend.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awbuilds.auraspend.R
import com.awbuilds.auraspend.ui.designsystem.AuraCard
import com.awbuilds.auraspend.ui.designsystem.AuraCardStyle
import com.awbuilds.auraspend.ui.designsystem.AuraGradients
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class OnboardingPage(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
)

private val onboardingPages = listOf(
    OnboardingPage(
        R.string.onboarding_page1_title,
        R.string.onboarding_page1_description,
        Icons.Default.Wallet
    ),
    OnboardingPage(
        R.string.onboarding_page2_title,
        R.string.onboarding_page2_description,
        Icons.Default.AutoAwesome
    ),
    OnboardingPage(
        R.string.onboarding_page3_title,
        R.string.onboarding_page3_description,
        Icons.Default.Savings
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
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { onFinished() }

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
                .padding(horizontal = AuraSpacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinished, enabled = !isRestoring) { Text(stringResource(R.string.action_skip)) }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageOffset =
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                OnboardingPageContent(onboardingPages[page], pageOffset)
            }

            PageIndicator(
                currentPage = pagerState.currentPage,
                totalPages = onboardingPages.size
            )

            Spacer(modifier = Modifier.height(AuraSpacing.xxl))

            Button(
                onClick = {
                    coroutineScope.launch {
                        if (pagerState.currentPage < onboardingPages.size - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onFinished()
                            }
                        }
                    }
                },
                enabled = !isRestoring,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    if (pagerState.currentPage == onboardingPages.size - 1) stringResource(R.string.action_get_started)
                    else stringResource(R.string.action_continue),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (pagerState.currentPage == onboardingPages.size - 1) {
                Spacer(modifier = Modifier.height(AuraSpacing.md))
                AuraCard(
                    style = AuraCardStyle.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(AuraSpacing.lg),
                    onClick = { if (!isRestoring) onRestoreFromDrive() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isRestoring) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(AuraSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.onboarding_restore_title),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                stringResource(R.string.onboarding_restore_subtitle),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AuraSpacing.xxl))
        }

        if (isRestoring) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                AuraCard(style = AuraCardStyle.Filled, contentPadding = PaddingValues(AuraSpacing.xxl)) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(AuraSpacing.lg))
                    Text(stringResource(R.string.onboarding_restoring))
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
private fun OnboardingPageContent(page: OnboardingPage, pageOffset: Float) {
    val alpha by animateFloatAsState(
        targetValue = (1f - abs(pageOffset) * 0.6f).coerceIn(0f, 1f),
        label = "pageAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = (1f - abs(pageOffset) * 0.08f).coerceIn(0.85f, 1f),
        label = "pageScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                translationX = pageOffset * 120f
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(CircleShape)
                .background(AuraGradients.aurora),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                page.icon,
                contentDescription = null,
                tint = AuraGradients.onAurora,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(AuraSpacing.xxxl))

        Text(
            stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(AuraSpacing.md))

        Text(
            stringResource(page.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AuraSpacing.lg)
        )
    }
}

@Composable
private fun PageIndicator(currentPage: Int, totalPages: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
        repeat(totalPages) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 26.dp else 8.dp,
                label = "dotWidth"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}
