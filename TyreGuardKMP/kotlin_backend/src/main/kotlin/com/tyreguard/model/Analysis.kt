package com.tyreguard.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "tire_analyses")
data class TireAnalysis(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tire_id", nullable = false)
    val tire: Tire? = null,

    @Column(nullable = false)
    val imageUrl: String = "",

    @Column(nullable = false, updatable = false)
    val analysisDate: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val healthScore: Int = 0,

    @Column(nullable = false)
    val wearPercentage: Int = 0,

    @ElementCollection
    @CollectionTable(name = "wear_patterns", joinColumns = [JoinColumn(name = "analysis_id")])
    @Column(name = "pattern")
    val wearPatterns: MutableList<String> = mutableListOf(),

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "analysis_id")
    val detectedDamages: MutableList<Damage> = mutableListOf(),

    @ElementCollection
    @CollectionTable(name = "recommendations", joinColumns = [JoinColumn(name = "analysis_id")])
    @Column(name = "recommendation")
    val recommendations: MutableList<String> = mutableListOf(),

    @Column
    val modelGenerationJobId: String? = null,

    @Column
    val modelUrl: String? = null,

    @Column(nullable = false)
    val confidence: Float = 0f,

    @Column(nullable = false)
    val mlServiceUsed: String = "google_ml_kit"
)

@Entity
@Table(name = "damages")
data class Damage(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @Column(nullable = false)
    val type: String = "", // puncture, cut, bulge, sidewall, etc.

    @Column(nullable = false)
    val severity: String = "", // low, medium, high, critical

    @Column(nullable = false)
    val location: String = "",

    @Column(nullable = false)
    val description: String = "",

    @Column(nullable = false)
    val confidence: Float = 0f
)

@Entity
@Table(name = "model_generation_jobs")
data class ModelGenerationJob(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    val analysis: TireAnalysis? = null,

    @Column(nullable = false)
    val externalJobId: String = "",

    @Column(nullable = false)
    val status: String = "pending", // pending, processing, completed, failed

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    val completedAt: LocalDateTime? = null,

    @Column
    val modelUrl: String? = null,

    @Column
    val errorMessage: String? = null,

    @Column(nullable = false)
    val retryCount: Int = 0
)
