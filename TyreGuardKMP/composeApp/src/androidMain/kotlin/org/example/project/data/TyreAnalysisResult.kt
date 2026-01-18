package org.example.project.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a tyre analysis result stored in Room database.
 * 
 * Stores the defect detection results along with the path to the saved image.
 */
@Entity(tableName = "tyre_analysis_results")
data class TyreAnalysisResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Absolute path to the saved tyre image */
    val imagePath: String,
    
    /** Detected defect type (e.g., "Cracked", "Worn", "Good") */
    val defectType: String,
    
    /** Confidence score from 0.0 to 1.0 */
    val confidence: Float,
    
    /** Timestamp when the analysis was performed (epoch millis) */
    val timestamp: Long = System.currentTimeMillis(),
    
    /** Optional user notes about the analysis */
    val notes: String? = null,
    
    /** AI sensitivity level used for this detection */
    val sensitivityLevel: Float = 0.5f,
    
    /** Whether the user marked this as reviewed */
    val isReviewed: Boolean = false
)
