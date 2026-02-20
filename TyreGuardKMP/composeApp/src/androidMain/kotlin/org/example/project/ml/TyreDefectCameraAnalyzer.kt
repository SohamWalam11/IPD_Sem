package org.example.project.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "TyreDefectAnalyzer"

/**
 * CameraX [ImageAnalysis.Analyzer] that converts each frame to a correctly
 * oriented [Bitmap] and forwards it to [TyreTaskVisionDetector].
 *
 * ## Key correctness guarantee
 * The `ImageProxy.imageInfo.rotationDegrees` value tells us how many degrees
 * clockwise the sensor image needs to be rotated to appear upright.  We apply
 * that rotation via a [Matrix] *before* sending the bitmap to the model, so
 * the detector always sees pixels the right way up regardless of device
 * orientation or which camera (front/back) is active.
 *
 * ## YUV → Bitmap conversion
 * CameraX delivers frames in `YUV_420_888`.  We convert via
 * `YuvImage → JPEG → BitmapFactory` — the only approach that is reliable
 * across all Android API levels without requiring NDK or Renderscript.
 *
 * @param detector        The Task Vision detector to run on each frame.
 * @param onFrameResult   Callback invoked on the calling thread with the
 *                        latest [TyreHealthReport] plus the rotated bitmap.
 * @param analyzeEveryN   Process one in every N frames (default 5) for
 *                        smooth preview performance.
 */
class TyreDefectCameraAnalyzer(
    private val detector: TyreTaskVisionDetector,
    private val onFrameResult: (bitmap: Bitmap, report: TyreHealthReport) -> Unit,
    private val analyzeEveryN: Int = 5
) : ImageAnalysis.Analyzer {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val busy = AtomicBoolean(false)
    private var frameCounter = 0

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        frameCounter++

        // Drop frames when the detector is still busy or we're skipping
        if (frameCounter % analyzeEveryN != 0 || !busy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            busy.set(false)
            return
        }

        // Capture rotation BEFORE closing the proxy
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        // ── YUV_420_888 → Bitmap ──────────────────────────────────────────
        val nv21 = yuv420ToNv21(imageProxy)
        imageProxy.close()           // ← close as soon as raw bytes are copied

        scope.launch {
            try {
                val rawBitmap = nv21ToBitmap(nv21, mediaImage.width, mediaImage.height)
                    ?: run { busy.set(false); return@launch }

                // ── Apply rotation ────────────────────────────────────────
                val rotatedBitmap = if (rotationDegrees != 0) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                        .also { if (it !== rawBitmap) rawBitmap.recycle() }
                } else {
                    rawBitmap
                }

                // ── Run Task Vision inference ─────────────────────────────
                val report = detector.analyzeAndScore(rotatedBitmap)
                onFrameResult(rotatedBitmap, report)
            } catch (e: Exception) {
                Log.e(TAG, "Frame analysis failed: ${e.message}", e)
            } finally {
                busy.set(false)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts a CameraX YUV_420_888 [ImageProxy] to an NV21 byte array.
     * NV21 is what [YuvImage] understands.
     */
    private fun yuv420ToNv21(imageProxy: ImageProxy): ByteArray {
        val yPlane = imageProxy.planes[0]
        val uPlane = imageProxy.planes[1]
        val vPlane = imageProxy.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        // NV21 interleaves V then U; some devices deliver U and V separately — handle both
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        return nv21
    }

    /** Converts NV21 bytes → [Bitmap] via JPEG intermediate. */
    private fun nv21ToBitmap(nv21: ByteArray, width: Int, height: Int): Bitmap? {
        return try {
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
            val jpegBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "YUV→Bitmap conversion failed: ${e.message}", e)
            null
        }
    }
}
