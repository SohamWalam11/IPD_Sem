package org.example.project.ml

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import io.anyline.tiretread.sdk.InternalAPI
import io.anyline.tiretread.sdk.NoConnectionException
import io.anyline.tiretread.sdk.Response
import io.anyline.tiretread.sdk.SdkLicenseKeyForbiddenException
import io.anyline.tiretread.sdk.SdkLicenseKeyInvalidException
import io.anyline.tiretread.sdk.getTreadDepthReportResult
import io.anyline.tiretread.sdk.init
import io.anyline.tiretread.sdk.config.TireTreadConfig
import io.anyline.tiretread.sdk.scanner.ScanEvent
import io.anyline.tiretread.sdk.scanner.composer.TireTreadScanView
import io.anyline.tiretread.sdk.types.TreadDepthResult
import kotlinx.coroutines.launch
import org.example.project.BuildConfig

// ──────────────────────────────────────────────────────────────────────────────
// Colour palette (matches app-wide dark theme)
// ──────────────────────────────────────────────────────────────────────────────
private val ScreenBg      = Color(0xFF0D0D1A)
private val CardBg        = Color(0xFF1A1A2E)
private val CardBgAlt     = Color(0xFF16213E)
private val ScanRed       = Color(0xFFE53935)
private val ScanRedDark   = Color(0xFFB71C1C)
private val AccentGreen   = Color(0xFF4CAF50)
private val AccentAmber   = Color(0xFFFFC107)
private val AccentRed     = Color(0xFFEF5350)
private val TextPrimary   = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.60f)

// ──────────────────────────────────────────────────────────────────────────────
// Phase state machine
// ──────────────────────────────────────────────────────────────────────────────
private sealed interface AnylinePhase {
    object Initializing                                                      : AnylinePhase
    object Ready                                                             : AnylinePhase
    /** Live Compose camera viewer is shown; scanning in progress */
    data class Scanning(val config: TireTreadConfig)                         : AnylinePhase
    data class FetchingResult(val uuid: String)                              : AnylinePhase
    data class ShowResult(val uuid: String, val zones: List<ZoneDepth>)      : AnylinePhase
    data class Failure(val message: String)                                  : AnylinePhase
}

private data class ZoneDepth(val label: String, val depthMm: Double) {
    val isAvailable: Boolean get() = depthMm >= 0.0
    val verdict: ZoneVerdict get() = when {
        !isAvailable  -> ZoneVerdict.UNKNOWN
        depthMm < 1.6 -> ZoneVerdict.CRITICAL
        depthMm < 3.0 -> ZoneVerdict.WARNING
        else          -> ZoneVerdict.GOOD
    }
}

private enum class ZoneVerdict { GOOD, WARNING, CRITICAL, UNKNOWN }

// ──────────────────────────────────────────────────────────────────────────────
// Main screen
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun AnylineTreadScanScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var phase by remember { mutableStateOf<AnylinePhase>(AnylinePhase.Initializing) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            phase = AnylinePhase.Scanning(TireTreadConfig())
        } else {
            phase = AnylinePhase.Failure("Camera permission is required for tread scanning")
        }
    }

    // ── SDK init ─────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        val key = BuildConfig.ANYLINE_LICENSE_KEY
        if (key.isNullOrBlank() || key == "null") {
            phase = AnylinePhase.Failure(
                "ANYLINE_LICENSE_KEY is not set.\n" +
                "Add it to your .env file:\n  ANYLINE_LICENSE_KEY=<your_key>\n\n" +
                "Free trial key at: anyline.com"
            )
            return@LaunchedEffect
        }
        try {
            @OptIn(InternalAPI::class)
            AnylineTireTreadSdk.init(licenseKey = key, context = context)
            phase = AnylinePhase.Ready
        } catch (e: SdkLicenseKeyInvalidException) {
            phase = AnylinePhase.Failure("Invalid Anyline license key — check ANYLINE_LICENSE_KEY in .env")
        } catch (e: SdkLicenseKeyForbiddenException) {
            phase = AnylinePhase.Failure("Anyline license key has expired or is forbidden")
        } catch (e: NoConnectionException) {
            phase = AnylinePhase.Failure("No internet connection — Anyline SDK requires connectivity")
        } catch (e: Exception) {
            phase = AnylinePhase.Failure(e.message ?: "Anyline SDK init failed")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        AnimatedContent(
            targetState = phase,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "anyline_phase"
        ) { currentPhase ->
            when (currentPhase) {
                is AnylinePhase.Initializing -> Column(Modifier.fillMaxSize()) {
                    TopBar(onBack = onBack)
                    InitializingView()
                }

                is AnylinePhase.Ready -> Column(Modifier.fillMaxSize()) {
                    TopBar(onBack = onBack)
                    ReadyView(onStartScan = {
                        if (hasCameraPermission) {
                            phase = AnylinePhase.Scanning(TireTreadConfig())
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    })
                }

                is AnylinePhase.Scanning -> {
                    // The Anyline Compose camera view fills the full screen
                    Box(Modifier.fillMaxSize()) {
                        TireTreadScanView(
                            /* config              */ currentPhase.config,
                            /* onScanAborted       */ { _: String? -> },
                            /* onScanProcessCompleted */ { uuid: String ->
                                phase = AnylinePhase.FetchingResult(uuid)
                                scope.launch { fetchResult(uuid) { p -> phase = p } }
                            },
                            /* onScanEvent (ScanEvent) */ { _: ScanEvent -> },
                            /* onError             */ { uuid: String?, exception: Exception ->
                                phase = AnylinePhase.Failure(
                                    exception.message ?: "Scan error${if (!uuid.isNullOrEmpty()) " (uuid=$uuid)" else ""}"
                                )
                            }
                        )

                        // Abort scan button
                        IconButton(
                            onClick  = { phase = AnylinePhase.Ready },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Abort scan",
                                tint     = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                is AnylinePhase.FetchingResult -> Column(Modifier.fillMaxSize()) {
                    TopBar(onBack = onBack)
                    FetchingResultView(currentPhase.uuid)
                }

                is AnylinePhase.ShowResult -> Column(Modifier.fillMaxSize()) {
                    TopBar(onBack = onBack)
                    ResultView(
                        uuid     = currentPhase.uuid,
                        zones    = currentPhase.zones,
                        onRescan = { phase = AnylinePhase.Ready }
                    )
                }

                is AnylinePhase.Failure -> Column(Modifier.fillMaxSize()) {
                    TopBar(onBack = onBack)
                    FailureView(
                        message   = currentPhase.message,
                        onDismiss = { phase = AnylinePhase.Ready },
                        onBack    = onBack
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Cloud result fetch
// ──────────────────────────────────────────────────────────────────────────────

private fun fetchResult(uuid: String, onPhaseChange: (AnylinePhase) -> Unit) {
    AnylineTireTreadSdk.getTreadDepthReportResult(uuid) { response: Response<TreadDepthResult> ->
        when (response) {
            is Response.Success -> {
                val result     = response.data
                val zoneLabels = listOf("Left", "Center", "Right")
                val regions    = result.regions

                val zones: List<ZoneDepth> = if (regions.isNotEmpty()) {
                    regions.take(3).mapIndexed { i, region ->
                        ZoneDepth(
                            label   = zoneLabels.getOrElse(i) { "Zone ${i + 1}" },
                            depthMm = if (region.isAvailable) region.valueMm else -1.0
                        )
                    }
                } else {
                    val globalMm = if (result.global.isAvailable) result.global.valueMm else -1.0
                    zoneLabels.map { label -> ZoneDepth(label, globalMm) }
                }

                onPhaseChange(AnylinePhase.ShowResult(uuid, zones))
            }
            is Response.Error     -> onPhaseChange(
                AnylinePhase.Failure(response.errorMessage ?: "Failed to fetch result from Anyline cloud")
            )
            is Response.Exception -> onPhaseChange(
                AnylinePhase.Failure(response.exception.message ?: "Exception while fetching result")
            )
            else -> onPhaseChange(AnylinePhase.Failure("Unexpected response from Anyline SDK"))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Spacer(Modifier.width(4.dp))
        Column {
            Text(
                "Tread Depth Scanner",
                color      = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp
            )
            Text(
                "Powered by Anyline TireTread SDK",
                color    = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun InitializingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ScanRed, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(20.dp))
            Text("Initializing Anyline SDK…", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ReadyView(onStartScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue  = 0.95f,
            targetValue   = 1.05f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
            label         = "scale"
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(ScanRedDark, ScreenBg)))
                .border(2.dp, ScanRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Scanner,
                contentDescription = null,
                tint     = ScanRed,
                modifier = Modifier.size(56.dp)
            )
        }

        Text(
            "Ready to Scan",
            color      = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize   = 24.sp
        )

        Text(
            "Point the camera at the tyre tread surface.\nThe SDK will guide you through the scan.",
            color      = TextSecondary,
            fontSize   = 14.sp,
            textAlign  = TextAlign.Center,
            lineHeight = 22.sp
        )

        TipCard("Hold phone parallel to tread")
        TipCard("Ensure good lighting conditions")
        TipCard("Scan across the full tyre width")

        Spacer(Modifier.height(8.dp))

        Button(
            onClick  = onStartScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ScanRed)
        ) {
            Icon(Icons.Default.Scanner, null, tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text(
                "Start Tread Scan",
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
                color      = Color.White
            )
        }
    }
}

@Composable
private fun TipCard(tip: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Info, null, tint = ScanRed, modifier = Modifier.size(18.dp))
            Text(tip, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun FetchingResultView(uuid: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color     = ScanRed,
                modifier  = Modifier.size(56.dp),
                strokeWidth = 4.dp
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Analyzing tread depth…",
                color      = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 16.sp
            )
            Spacer(Modifier.height(6.dp))
            Text("Processing results on the cloud", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "UUID: ${uuid.take(8)}…",
                color      = TextSecondary.copy(alpha = 0.4f),
                fontSize   = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ResultView(uuid: String, zones: List<ZoneDepth>, onRescan: () -> Unit) {
    val overallVerdict = when {
        zones.any { it.verdict == ZoneVerdict.CRITICAL } -> ZoneVerdict.CRITICAL
        zones.any { it.verdict == ZoneVerdict.WARNING  } -> ZoneVerdict.WARNING
        zones.any { it.verdict == ZoneVerdict.UNKNOWN  } -> ZoneVerdict.UNKNOWN
        else                                             -> ZoneVerdict.GOOD
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall verdict banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(20.dp),
            colors   = CardDefaults.cardColors(
                containerColor = when (overallVerdict) {
                    ZoneVerdict.GOOD     -> Color(0xFF1B3A2D)
                    ZoneVerdict.WARNING  -> Color(0xFF2D2800)
                    ZoneVerdict.CRITICAL -> Color(0xFF3A1B1B)
                    ZoneVerdict.UNKNOWN  -> CardBg
                }
            )
        ) {
            Row(
                Modifier.padding(20.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    when (overallVerdict) {
                        ZoneVerdict.GOOD     -> Icons.Default.CheckCircle
                        ZoneVerdict.WARNING  -> Icons.Default.Warning
                        ZoneVerdict.CRITICAL -> Icons.Default.Error
                        ZoneVerdict.UNKNOWN  -> Icons.Default.Info
                    },
                    null,
                    tint     = verdictColor(overallVerdict),
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text(
                        verdictLabel(overallVerdict),
                        color      = verdictColor(overallVerdict),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 18.sp
                    )
                    Text(verdictSubtitle(overallVerdict), color = TextSecondary, fontSize = 13.sp)
                }
            }
        }

        Text(
            "Zone Analysis",
            color      = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize   = 16.sp,
            modifier   = Modifier.padding(start = 4.dp)
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            zones.forEach { zone ->
                ZoneCard(modifier = Modifier.weight(1f), zone = zone)
            }
        }

        // Legal info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp),
            colors   = CardDefaults.cardColors(containerColor = CardBgAlt)
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Info, null,
                    tint     = AccentAmber,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "Legal minimum: 1.6 mm  •  Recommended: ≥ 3.0 mm",
                    color    = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // UUID card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp),
            colors   = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Scan ID", color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    uuid,
                    color      = TextPrimary,
                    fontSize   = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Button(
            onClick  = onRescan,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ScanRed)
        ) {
            Icon(
                Icons.Default.Refresh, null,
                tint     = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Scan Again", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun ZoneCard(modifier: Modifier = Modifier, zone: ZoneDepth) {
    val clr = verdictColor(zone.verdict)
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(clr.copy(alpha = 0.15f))
                    .border(1.5.dp, clr, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (zone.verdict) {
                        ZoneVerdict.GOOD     -> Icons.Default.CheckCircle
                        ZoneVerdict.WARNING  -> Icons.Default.Warning
                        ZoneVerdict.CRITICAL -> Icons.Default.Error
                        ZoneVerdict.UNKNOWN  -> Icons.Default.Info
                    },
                    null,
                    tint     = clr,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(zone.label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                text       = if (!zone.isAvailable) "—" else "${"%.1f".format(zone.depthMm)} mm",
                color      = clr,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 20.sp
            )
            Box(
                modifier = Modifier
                    .background(clr.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    verdictBadge(zone.verdict),
                    color      = clr,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FailureView(message: String, onDismiss: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Error, null, tint = AccentRed, modifier = Modifier.size(64.dp))
            Text(
                "Scan Error",
                color      = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize   = 22.sp
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFF3A1B1B))
            ) {
                Text(
                    message,
                    color      = TextPrimary,
                    fontSize   = 14.sp,
                    lineHeight = 22.sp,
                    modifier   = Modifier.padding(16.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick  = onBack,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) { Text("Go Back") }
                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = ScanRed)
                ) { Text("Try Again", color = Color.White) }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Verdict helpers
// ──────────────────────────────────────────────────────────────────────────────

private fun verdictColor(v: ZoneVerdict): Color = when (v) {
    ZoneVerdict.GOOD     -> AccentGreen
    ZoneVerdict.WARNING  -> AccentAmber
    ZoneVerdict.CRITICAL -> AccentRed
    ZoneVerdict.UNKNOWN  -> Color.Gray
}

private fun verdictLabel(v: ZoneVerdict): String = when (v) {
    ZoneVerdict.GOOD     -> "TYRE OK"
    ZoneVerdict.WARNING  -> "MONITOR WEAR"
    ZoneVerdict.CRITICAL -> "REPLACE TYRE"
    ZoneVerdict.UNKNOWN  -> "DATA UNAVAILABLE"
}

private fun verdictSubtitle(v: ZoneVerdict): String = when (v) {
    ZoneVerdict.GOOD     -> "Tread depth is within safe limits"
    ZoneVerdict.WARNING  -> "Tread depth is below recommended level"
    ZoneVerdict.CRITICAL -> "Tread depth is at or below legal minimum"
    ZoneVerdict.UNKNOWN  -> "Could not retrieve measurement data"
}

private fun verdictBadge(v: ZoneVerdict): String = when (v) {
    ZoneVerdict.GOOD     -> "GOOD"
    ZoneVerdict.WARNING  -> "WARN"
    ZoneVerdict.CRITICAL -> "CRIT"
    ZoneVerdict.UNKNOWN  -> "N/A"
}
