package org.example.project.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing tyre analysis data.
 * 
 * Combines ImageStorageManager and Room database operations
 * to provide a unified API for saving and retrieving analysis results.
 */
class TyreAnalysisRepository(context: Context) {
    
    private val database = TyreGuardDatabase.getInstance(context)
    private val dao = database.tyreAnalysisDao()
    private val imageStorage = ImageStorageManager(context)
    
    /**
     * Save a complete analysis result (image + data).
     * 
     * @param bitmap The tyre image to save
     * @param defectType Detected defect type
     * @param confidence Detection confidence
     * @param sensitivityLevel AI sensitivity used
     * @return The saved TyreAnalysisResult, or null if save failed
     */
    suspend fun saveAnalysis(
        bitmap: android.graphics.Bitmap,
        defectType: String,
        confidence: Float,
        sensitivityLevel: Float = 0.5f
    ): TyreAnalysisResult? {
        // Save image first
        val imagePath = imageStorage.saveImage(bitmap, "analysis")
            ?: return null
        
        // Create and save result to database
        val result = TyreAnalysisResult(
            imagePath = imagePath,
            defectType = defectType,
            confidence = confidence,
            sensitivityLevel = sensitivityLevel
        )
        
        val id = dao.insert(result)
        return result.copy(id = id)
    }
    
    /**
     * Save analysis result with existing image path.
     */
    suspend fun saveAnalysisWithPath(
        imagePath: String,
        defectType: String,
        confidence: Float,
        sensitivityLevel: Float = 0.5f
    ): TyreAnalysisResult {
        val result = TyreAnalysisResult(
            imagePath = imagePath,
            defectType = defectType,
            confidence = confidence,
            sensitivityLevel = sensitivityLevel
        )
        val id = dao.insert(result)
        return result.copy(id = id)
    }
    
    /**
     * Get all analysis results as a Flow.
     */
    fun getAllResults(): Flow<List<TyreAnalysisResult>> {
        return dao.getAllResults()
    }
    
    /**
     * Get recent analysis results.
     */
    suspend fun getRecentResults(limit: Int = 10): List<TyreAnalysisResult> {
        return dao.getRecentResults(limit)
    }
    
    /**
     * Get results by defect type.
     */
    fun getResultsByDefectType(defectType: String): Flow<List<TyreAnalysisResult>> {
        return dao.getResultsByDefectType(defectType)
    }
    
    /**
     * Get a single result by ID.
     */
    suspend fun getResultById(id: Long): TyreAnalysisResult? {
        return dao.getResultById(id)
    }
    
    /**
     * Delete a result (also deletes the associated image).
     */
    suspend fun deleteResult(result: TyreAnalysisResult) {
        // Delete image first
        imageStorage.deleteImage(result.imagePath)
        // Then delete from database
        dao.delete(result)
    }
    
    /**
     * Delete result by ID.
     */
    suspend fun deleteResultById(id: Long) {
        val result = dao.getResultById(id)
        if (result != null) {
            deleteResult(result)
        }
    }
    
    /**
     * Mark result as reviewed.
     */
    suspend fun markAsReviewed(id: Long) {
        dao.markAsReviewed(id)
    }
    
    /**
     * Update notes for a result.
     */
    suspend fun updateNotes(id: Long, notes: String) {
        val result = dao.getResultById(id)
        if (result != null) {
            dao.update(result.copy(notes = notes))
        }
    }
    
    /**
     * Get total result count.
     */
    suspend fun getResultCount(): Int {
        return dao.getResultCount()
    }
    
    /**
     * Get unreviewed results.
     */
    fun getUnreviewedResults(): Flow<List<TyreAnalysisResult>> {
        return dao.getUnreviewedResults()
    }
    
    /**
     * Clear all data (results and images).
     */
    suspend fun clearAllData() {
        dao.deleteAll()
        imageStorage.clearAllImages()
    }
    
    /**
     * Get storage statistics.
     */
    suspend fun getStorageStats(): StorageStats {
        return StorageStats(
            resultCount = dao.getResultCount(),
            storageUsedBytes = imageStorage.getStorageUsedBytes()
        )
    }
}

/**
 * Storage statistics data class.
 */
data class StorageStats(
    val resultCount: Int,
    val storageUsedBytes: Long
) {
    val storageUsedMB: Float
        get() = storageUsedBytes / (1024f * 1024f)
}
