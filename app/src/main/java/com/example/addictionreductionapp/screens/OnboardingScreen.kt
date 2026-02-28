package com.example.addictionreductionapp.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.components.PrimaryButton
import com.example.addictionreductionapp.data.AppDataStore
import com.example.addictionreductionapp.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accentColor: Color
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(
            Icons.Default.Psychology,
            "Reclaim Your Focus",
            "Take control of your screen time and build better digital habits with smart tools and AI-powered coaching.",
            RegainTeal
        ),
        OnboardingPage(
            Icons.Default.Timer,
            "Focus Timer",
            "Use the study timer with ambient sounds to stay concentrated. Lock your phone and enter deep focus mode.",
            RegainPurple
        ),
        OnboardingPage(
            Icons.Default.Block,
            "Block Distractions",
            "Select apps to block, set daily limits, and schedule blocking windows. Stay productive effortlessly.",
            RegainBlue
        ),
        OnboardingPage(
            Icons.Default.EmojiEvents,
            "Earn Rewards",
            "Build streaks, unlock achievements, and track your progress. Make productivity fun and engaging.",
            RegainOrange
        )
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            // Page indicators
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(
                                width = if (isActive) 24.dp else 8.dp,
                                height = 8.dp
                            )
                            .clip(CircleShape)
                            .background(
                                if (isActive) pages[index].accentColor
                                else DarkCardLight
                            )
                    )
                }
            }

            if (pagerState.currentPage == 3) {
                PrimaryButton(
                    text = "Get Started",
                    onClick = {
                        AppDataStore.hasCompletedOnboarding.value = true
                        AppDataStore.saveToPrefs(context)
                        onComplete()
                    }
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        AppDataStore.hasCompletedOnboarding.value = true
                        AppDataStore.saveToPrefs(context)
                        onComplete()
                    }) {
                        Text("Skip", color = TextGray, fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RegainTeal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next", color = Color.Black, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing icon container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            // Glow effect
            Box(
                Modifier
                    .size(160.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                page.accentColor.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            // Icon circle
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = page.accentColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        page.icon,
                        contentDescription = null,
                        tint = page.accentColor,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        Text(
            page.title,
            color = TextWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            page.subtitle,
            color = TextGray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(100.dp))
    }
}
