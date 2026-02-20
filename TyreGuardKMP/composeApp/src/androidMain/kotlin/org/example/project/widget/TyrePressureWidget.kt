package org.example.project.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.example.project.MainActivity
import org.example.project.R

/**
 * Samsung-Style Home Screen Widget for TyreGuard AI.
 * Uses Jetpack Glance for modern, responsive UI.
 */
class TyrePressureWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                TyreWidgetContent(context)
            }
        }
    }

    @Composable
    private fun TyreWidgetContent(context: Context) {
        // Retrieve state from DataStore (PreferencesGlanceStateDefinition)
        val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
        
        val flPsi = prefs[TyreWidgetKeys.PSI_FL] ?: 32.5f
        val frPsi = prefs[TyreWidgetKeys.PSI_FR] ?: 32.2f
        val rlPsi = prefs[TyreWidgetKeys.PSI_RL] ?: 32.8f
        val rrPsi = prefs[TyreWidgetKeys.PSI_RR] ?: 32.4f
        
        val lastUpdated = prefs[TyreWidgetKeys.LAST_UPDATED] ?: "Just now"

        // Samsung Style Container: Dark transparency with rounded corners
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xE6121212))) // 90% opacity dark
                .cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TyreGuard AI",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = lastUpdated,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFAAAAAA)),
                                fontSize = 12.sp
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    // Example Status Icon (Green Dot)
                    Box(
                        modifier = GlanceModifier
                            .size(12.dp)
                            .background(ColorProvider(Color(0xFF00E676))) // Green
                            .cornerRadius(6.dp)
                    ) {}
                }

                Spacer(modifier = GlanceModifier.height(16.dp))

                // Car & Tyres Layout
                // Simplified view: Top row for Front, Bottom row for Rear
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Left Side
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TyreBubble(label = "FL", psi = flPsi, isLow = flPsi < 30f)
                        Spacer(modifier = GlanceModifier.height(12.dp))
                        TyreBubble(label = "RL", psi = rlPsi, isLow = rlPsi < 30f)
                    }

                    Spacer(modifier = GlanceModifier.width(24.dp))

                    // Center Car Image (Text placeholder if image not available, 
                    // ideally use ImageProvider(R.drawable.car_top_view))
                    // For now, a vertical line representing the car chassis
                    Box(
                        modifier = GlanceModifier
                            .width(40.dp)
                            .height(100.dp)
                            .background(ColorProvider(Color(0xFF333333)))
                            .cornerRadius(12.dp)
                    ) {}

                    Spacer(modifier = GlanceModifier.width(24.dp))

                    // Right Side
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TyreBubble(label = "FR", psi = frPsi, isLow = frPsi < 30f)
                        Spacer(modifier = GlanceModifier.height(12.dp))
                        TyreBubble(label = "RR", psi = rrPsi, isLow = rrPsi < 30f)
                    }
                }
            }
        }
    }

    @Composable
    private fun TyreBubble(label: String, psi: Float, isLow: Boolean) {
        val bgColor = if (isLow) Color(0xFFB00020) else Color(0xFF2C2C2C)
        val textColor = if (isLow) Color.White else Color(0xFFE0E0E0)
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier
                .background(ColorProvider(bgColor))
                .cornerRadius(12.dp)
                .padding(8.dp)
                .width(64.dp)
        ) {
            Text(
                text = "${"%.1f".format(psi)}",
                style = TextStyle(
                    color = ColorProvider(textColor),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "PSI",
                style = TextStyle(
                    color = ColorProvider(textColor.copy(alpha = 0.7f)),
                    fontSize = 10.sp
                )
            )
        }
    }
}
