package com.tyreguard.repository

import com.tyreguard.model.TireAnalysis
import com.tyreguard.model.ModelGenerationJob
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TireAnalysisRepository : JpaRepository<TireAnalysis, String> {
    fun findByTireId(tireId: String): List<TireAnalysis>
    fun findFirstByTireIdOrderByAnalysisDateDesc(tireId: String): Optional<TireAnalysis>
}

@Repository
interface ModelGenerationJobRepository : JpaRepository<ModelGenerationJob, String> {
    fun findByExternalJobId(externalJobId: String): Optional<ModelGenerationJob>
    fun findByAnalysisId(analysisId: String): Optional<ModelGenerationJob>
    fun findByStatus(status: String): List<ModelGenerationJob>
}
