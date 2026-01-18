package org.example.project.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TyreAnalysisResult entity.
 * 
 * Provides CRUD operations for analysis results stored in Room database.
 */
@Dao
interface TyreAnalysisDao {
    
    /**
     * Insert a new analysis result.
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: TyreAnalysisResult): Long
    
    /**
     * Update an existing analysis result.
     */
    @Update
    suspend fun update(result: TyreAnalysisResult)
    
    /**
     * Delete an analysis result.
     */
    @Delete
    suspend fun delete(result: TyreAnalysisResult)
    
    /**
     * Delete analysis result by ID.
     */
    @Query("DELETE FROM tyre_analysis_results WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * Get all analysis results ordered by timestamp (newest first).
     */
    @Query("SELECT * FROM tyre_analysis_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<TyreAnalysisResult>>
    
    /**
     * Get a single analysis result by ID.
     */
    @Query("SELECT * FROM tyre_analysis_results WHERE id = :id")
    suspend fun getResultById(id: Long): TyreAnalysisResult?
    
    /**
     * Get analysis results by defect type.
     */
    @Query("SELECT * FROM tyre_analysis_results WHERE defectType = :defectType ORDER BY timestamp DESC")
    fun getResultsByDefectType(defectType: String): Flow<List<TyreAnalysisResult>>
    
    /**
     * Get recent analysis results (last N items).
     */
    @Query("SELECT * FROM tyre_analysis_results ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentResults(limit: Int = 10): List<TyreAnalysisResult>
    
    /**
     * Get unreviewed analysis results.
     */
    @Query("SELECT * FROM tyre_analysis_results WHERE isReviewed = 0 ORDER BY timestamp DESC")
    fun getUnreviewedResults(): Flow<List<TyreAnalysisResult>>
    
    /**
     * Mark result as reviewed.
     */
    @Query("UPDATE tyre_analysis_results SET isReviewed = 1 WHERE id = :id")
    suspend fun markAsReviewed(id: Long)
    
    /**
     * Get count of all analysis results.
     */
    @Query("SELECT COUNT(*) FROM tyre_analysis_results")
    suspend fun getResultCount(): Int
    
    /**
     * Get count of results by defect type.
     */
    @Query("SELECT COUNT(*) FROM tyre_analysis_results WHERE defectType = :defectType")
    suspend fun getResultCountByDefectType(defectType: String): Int
    
    /**
     * Delete all analysis results.
     */
    @Query("DELETE FROM tyre_analysis_results")
    suspend fun deleteAll()
    
    /**
     * Get results within a date range.
     */
    @Query("SELECT * FROM tyre_analysis_results WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getResultsInDateRange(startTime: Long, endTime: Long): Flow<List<TyreAnalysisResult>>
}
