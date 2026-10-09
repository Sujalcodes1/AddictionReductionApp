package com.example.addictionreductionapp.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val titlePrefix: String,
    val titleHighlight: String,
    val subtitle: String,
    val accentColor: Color,
    val pageNumber: String
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    val pages = remember {
        listOf(
            OnboardingPage(
                Icons.Default.Psychology,
                "Reclaim Your ",
                "Focus",
                "Take control of your screen time and build healthy digital habits with AI-powered focus tools.",
                CyanAccent,
                "01"
            ),
            OnboardingPage(
                Icons.Default.Shield,
                "Block ",
                "Distractions",
                "Select distracting apps, set custom daily limits, and auto-block impulse usage during work hours.",
                CyanAccent,
                "02"
            ),
            OnboardingPage(
                Icons.Default.Timer,
                "Deep Focus ",
                "Timer",
                "Lock your session with ambient sounds and Pomodoro rhythms to keep your productivity flowing.",
                CyanAccent,
                "03"
            ),
            OnboardingPage(
                Icons.Default.EmojiEvents,
                "Earn ",
                "Rewards",
                "Build daily focus streaks, level up your shield, and track real-time habit analytics.",
                CyanAccent,
                "04"
            )
        )
    }

    val currentPage = pages[pagerState.currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Dynamic background radial aura matching current page accent
        val auraAlpha by animateFloatAsState(
            targetValue = 0.25f,
            animationSpec = tween(600),
            label = "auraAlpha"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            currentPage.accentColor.copy(alpha = auraAlpha),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header Bar: Step indicator + Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkCard.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, FocusCardStroke.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentPage.pageNumber,
                            color = currentPage.accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = " / 04",
                            color = SubtitleWhite,
                            fontSize = 13.sp
                        )
                    }
                }

                if (pagerState.currentPage < 3) {
                    TextButton(onClick = onComplete) {
                        Text(
                            text = "Skip",
                            color = SubtitleWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Main ViewPager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                // LAG FIX: preload only adjacent page — default preloads all,
                // each running its own infiniteTransition pulse.
                beyondViewportPageCount = 1
            ) { page ->
                OnboardingPageContent(pages[page], isVisible = (page == pagerState.currentPage))
            }

            Spacer(Modifier.height(16.dp))

            // Bottom Navigation Control Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dynamic Animated Indicator Pills
                Row(
                    modifier = Modifier.padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isSelected = pagerState.currentPage == index
                        // LAG FIX: was spring(Bouncy, Low stiffness) — long overshoot
                        // re-layouts 4 indicators for ~1s per swipe. Snappy tween.
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 32.dp else 8.dp,
                            animationSpec = tween(250),
                            label = "indicatorWidth"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) currentPage.accentColor
                                    else DarkCardLight.copy(alpha = 0.6f)
                                )
                        )
                    }
                }

                // Action Buttons (Next / Get Started)
                if (pagerState.currentPage == 3) {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = currentPage.accentColor),
                        colors = ButtonDefaults.buttonColors(containerColor = currentPage.accentColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Get Started",
                                color = DarkBackground,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pagerState.currentPage > 0) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            ) {
                                Text("← Back", color = SubtitleWhite, fontSize = 16.sp)
                            }
                        } else {
                            Spacer(Modifier.width(60.dp))
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier
                                .height(52.dp)
                                .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = currentPage.accentColor),
                            colors = ButtonDefaults.buttonColors(containerColor = currentPage.accentColor),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Next",
                                    color = DarkBackground,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = DarkBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, isVisible: Boolean) {
    // LAG FIX: 4 pages x 2 infinite animations = 8 perpetual recompositions even
    // off-screen. Freeze pulse when page isn't visible.
    val infiniteTransition = rememberInfiniteTransition(label = "glowPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (isVisible) 1.05f else 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isVisible) 0.65f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Floating Animated Badge with Radial Glow Rings
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .scale(pulseScale)
        ) {
            // Outer Halo Ring
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                page.accentColor.copy(alpha = glowAlpha),
                                page.accentColor.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Glass Inner Container
            Surface(
                modifier = Modifier.size(128.dp),
                shape = CircleShape,
                color = DarkCard.copy(alpha = 0.75f),
                border = BorderStroke(1.5.dp, page.accentColor.copy(alpha = 0.6f)),
                shadowElevation = 16.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = page.accentColor,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(36.dp))

        // Two-Tone Bold Title
        Text(
            text = buildAnnotatedString {
                append(page.titlePrefix)
                withStyle(SpanStyle(color = page.accentColor, fontWeight = FontWeight.ExtraBold)) {
                    append(page.titleHighlight)
                }
            },
            color = TextWhite,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp
        )

        Spacer(Modifier.height(16.dp))

        // Glassmorphic Body Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = DarkCard.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, FocusCardStroke.copy(alpha = 0.35f))
        ) {
            Text(
                text = page.subtitle,
                color = SubtitleWhite,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 23.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
            )
        }
    }
}

