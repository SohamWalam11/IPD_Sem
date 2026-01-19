package org.example.project.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.launch

// Theme colors
private val PrimaryViolet = Color(0xFF6200EA)
private val SecondaryPurple = Color(0xFFBB86FC)
private val DeepViolet = Color(0xFF3700B3)
private val DarkViolet = Color(0xFF4A148C)
private val LightLavender = Color(0xFFF3E5F5)

private val BrandGradient = Brush.horizontalGradient(
    colors = listOf(PrimaryViolet, DeepViolet)
)

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val lottieUrl: String? = null,  // URL for Lottie animation
    val lottieRes: Int? = null       // Raw resource ID (optional)
)

/**
 * Enhanced Onboarding Screen with Lottie Animations
 * Uses video-like Lottie animations instead of static rotating tyre
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedOnboardingScreen(
    modifier: Modifier = Modifier,
    onFinishOnboarding: () -> Unit
) {
    val pages = listOf(
        OnboardingPageData(
            title = "Know your tyres heartbeat",
            subtitle = "Connect to your car's smart sensors instantly. Monitor Pressure and Temperature in real-time, right from your dashboard",
            // Tyre/wheel monitoring animation
            lottieUrl = "https://lottie.host/2b2e4679-83fd-4953-888c-1a98b8c2f60b/aNH3CjwC5r.lottie"
        ),
        OnboardingPageData(
            title = "AI-Powered inspections",
            subtitle = "Don't just guess—know. Scan your tires with your camera to detect hidden cracks, wear, and defects instantly.",
            // AI/scanning animation
            lottieUrl = "https://lottie.host/embed/ef8ee65e-76ff-42cc-a47a-e8e94f5e3a37/NUVcRjrDwS.lottie"
        ),
        OnboardingPageData(
            title = "Drive with confidence",
            subtitle = "Get instant alerts for leaks or blowouts. Your personal TyreBot Assistant is always watching to keep you safe on the road",
            // The tyre loading animation from user's link
            lottieUrl = "https://lottie.host/cb5fb99a-70dd-42a6-a61d-bdb4ef03e52d/SLpT8EHOrd.lottie"
        )
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LightLavender,
                        Color.White,
                        LightLavender.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar with Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage != pages.lastIndex) {
                    TextButton(onClick = onFinishOnboarding) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepViolet
                        )
                    }
                }
            }

            // Pager with Lottie animations
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val pageData = pages[page]
                
                OnboardingPageContent(
                    pageData = pageData,
                    isCurrentPage = pagerState.currentPage == page
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pager indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    
                    // Animated indicator
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "indicator_width"
                    )
                    
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(indicatorWidth)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) PrimaryViolet
                                else Color.Gray.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary button with gradient
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                onClick = {
                    if (pagerState.currentPage < pages.lastIndex) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinishOnboarding()
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BrandGradient),
                    contentAlignment = Alignment.Center
                ) {
                    val isLastPage = pagerState.currentPage == pages.lastIndex
                    Text(
                        text = if (isLastPage) "GET STARTED" else "NEXT",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(
    pageData: OnboardingPageData,
    isCurrentPage: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Lottie Animation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            if (pageData.lottieUrl != null) {
                LottieAnimationFromUrl(
                    url = pageData.lottieUrl,
                    isPlaying = isCurrentPage,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback placeholder
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(PrimaryViolet.copy(alpha = 0.1f))
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Title with animation
        AnimatedVisibility(
            visible = isCurrentPage,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 },
            exit = fadeOut(tween(300))
        ) {
            Text(
                text = pageData.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 40.sp
                ),
                textAlign = TextAlign.Center,
                color = DarkViolet
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Subtitle with animation
        AnimatedVisibility(
            visible = isCurrentPage,
            enter = fadeIn(tween(500, delayMillis = 200)) + slideInVertically(tween(500, delayMillis = 200)) { it / 2 },
            exit = fadeOut(tween(300))
        ) {
            Text(
                text = pageData.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Lottie animation loaded from URL
 */
@Composable
private fun LottieAnimationFromUrl(
    url: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Url(url)
    )
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = LottieConstants.IterateForever,
        speed = 1f
    )
    
    // Loading state
    val isLoading = composition == null
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            // Show a subtle loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = PrimaryViolet.copy(alpha = 0.5f),
                strokeWidth = 2.dp
            )
        } else {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
