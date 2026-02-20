package com.tyreguard.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notifications")
data class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User? = null,

    @Column(nullable = false)
    val type: String = "", // health_alert, service_reminder, system

    @Column(nullable = false)
    val title: String = "",

    @Column(nullable = false)
    val message: String = "",

    @Column
    val tireId: String? = null,

    @Column
    val analysisId: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    val readAt: LocalDateTime? = null,

    @Column
    val actionUrl: String? = null
)

@Entity
@Table(name = "service_centers")
data class ServiceCenter(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false)
    val address: String = "",

    @Column(nullable = false)
    val latitude: Double = 0.0,

    @Column(nullable = false)
    val longitude: Double = 0.0,

    @Column(nullable = false)
    val phoneNumber: String = "",

    @Column(nullable = false)
    val hours: String = "",

    @ElementCollection
    @CollectionTable(name = "service_center_services", joinColumns = [JoinColumn(name = "service_center_id")])
    @Column(name = "service")
    val services: MutableList<String> = mutableListOf(),

    @Column(nullable = false)
    val rating: Float = 0f,

    @Column(nullable = false)
    val reviewCount: Int = 0
)
