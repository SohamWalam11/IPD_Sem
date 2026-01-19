package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val PrimaryViolet = Color(0xFF6200EA)
private val DeepViolet = Color(0xFF3700B3)
private val LightLavender = Color(0xFFF3E5F5)

/**
 * iOS implementation - permissions are handled by the system
 * Just shows a brief splash and continues
 */
@Composable
actual fun PermissionScreenPlaceholder(
    modifier: Modifier,
    onPermissionsGranted: () -> Unit,
    onSkip: () -> Unit
) {
    // On iOS, permissions are requested when needed
    // Just show a brief transition and continue
    LaunchedEffect(Unit) {
        delay(500)
        onPermissionsGranted()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LightLavender, Color.White, LightLavender.copy(alpha = 0.5f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = PrimaryViolet,
                strokeWidth = 3.dp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Setting up TyreGuard...",
                style = MaterialTheme.typography.bodyLarge,
                color = DeepViolet,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
