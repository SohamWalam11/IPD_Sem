package org.example.project

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi

data class OnboardingPage(
    val title: String,
    val subtitle: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onFinishOnboarding: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "Know your tyres heartbeat",
            subtitle = "Connect to your car’s smart sensors instantly. Monitor Pressure and Temperature in real-time, right from your dashboard"
        ),
        OnboardingPage(
            title = "AI-Powered inspections",
            subtitle = "Don't just guess—know. Scan your tires with your camera to detect hidden cracks, wear, and defects instantly."
        ),
        OnboardingPage(
            title = "Drive with confidence",
            subtitle = "Get instant alerts for leaks or blowouts. Your personal TyreBot Assistant is always watching to keep you safe on the road"
        )
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    // Background tire rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "tireRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Animated tire background
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
                .rotate(rotation)
                .alpha(0.08f)
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = size / 2f
                val radius = size.minDimension / 2.2f

                // Outer ring
                drawCircle(
                    color = primaryColor,
                    radius = radius,
                    style = Stroke(width = 8f)
                )

                // Inner ring
                drawCircle(
                    color = primaryColor,
                    radius = radius * 0.65f,
                    style = Stroke(width = 6f)
                )

                // Hub
                drawCircle(
                    color = primaryColor,
                    radius = radius * 0.25f,
                    style = Stroke(width = 4f)
                )

                // Simple "spokes"
                val spokeCount = 6
                for (i in 0 until spokeCount) {
                    val angle = (Math.PI * 2 / spokeCount * i).toFloat()
                    val startRadius = radius * 0.3f
                    val endRadius = radius * 0.9f

                    val startX = center.width + startRadius * kotlin.math.cos(angle)
                    val startY = center.height + startRadius * kotlin.math.sin(angle)
                    val endX = center.width + endRadius * kotlin.math.cos(angle)
                    val endY = center.height + endRadius * kotlin.math.sin(angle)

                    drawLine(
                        color = primaryColor,
                        start = androidx.compose.ui.geometry.Offset(startX, startY),
                        end = androidx.compose.ui.geometry.Offset(endX, endY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Foreground pager content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar with optional Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage != pages.lastIndex) {
                    TextButton(onClick = onFinishOnboarding) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Center content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val pageData = pages[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = pageData.title,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 40.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = pageData.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pager indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .background(
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary button
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
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
                val isLastPage = pagerState.currentPage == pages.lastIndex
                Text(
                    text = if (isLastPage) "CONTINUE" else "NEXT",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
@Preview
private fun OnboardingScreenPreview() {
    MaterialTheme {
        OnboardingScreen(onFinishOnboarding = {})
    }
}

