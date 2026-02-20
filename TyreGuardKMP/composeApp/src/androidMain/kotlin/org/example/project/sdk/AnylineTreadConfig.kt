package org.example.project.sdk

import org.example.project.BuildConfig

/**
 * Configuration for Anyline Tire Tread SDK.
 *
 * Get your license key from: https://anyline.com/products/tire-tread-scanner
 *
 * Features:
 * - AI-powered tread depth measurement
 * - Multi-point measurement (inner, center, outer)
 * - Heat map visualization of wear patterns
 * - PDF report generation
 */
object AnylineTreadConfig {

    /**
     * License key for Anyline SDK.
     * Loaded from .env file via BuildConfig.
     * Add ANYLINE_LICENSE_KEY=your_key to .env file.
     */
    val LICENSE_KEY: String = BuildConfig.ANYLINE_LICENSE_KEY

    /**
     * Michelin API key for tire recognition.
     * Loaded from .env file via BuildConfig.
     * Add MICHELIN_API_KEY=your_key to .env file.
     */
    val MICHELIN_API_KEY: String = BuildConfig.MICHELIN_API_KEY

    /**
     * Minimum tread depth threshold (mm) before warning.
     * Legal minimum in most regions is 1.6mm, but 3mm is recommended for safety.
     */
    const val TREAD_WARNING_THRESHOLD_MM = 3.0f

    /**
     * Critical tread depth threshold (mm) - immediate replacement recommended.
     */
    const val TREAD_CRITICAL_THRESHOLD_MM = 1.6f

    /**
     * New tire typical tread depth (mm).
     */
    const val NEW_TIRE_TREAD_DEPTH_MM = 8.0f

    /**
     * Enable debug logging for SDK.
     */
    const val DEBUG_ENABLED = true

    /**
     * Measurement quality thresholds.
     */
    object Quality {
        const val MIN_CONFIDENCE = 0.7f
        const val PREFERRED_CONFIDENCE = 0.85f
    }
}
