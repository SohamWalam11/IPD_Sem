package com.tyreguard.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @Column(unique = true, nullable = false)
    val email: String = "",

    @Column(nullable = false)
    val passwordHash: String = "",

    @Column(nullable = false)
    val firstName: String = "",

    @Column(nullable = false)
    val lastName: String = "",

    @Column
    val phoneNumber: String? = null,

    @Column
    val profileImageUrl: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val vehicles: MutableList<Vehicle> = mutableListOf(),

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "preferences_id")
    val preferences: UserPreferences? = null
)

@Entity
@Table(name = "user_preferences")
data class UserPreferences(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @Column(nullable = false)
    val notificationsEnabled: Boolean = true,

    @Column(nullable = false)
    val alertThreshold: Int = 40,

    @Column(nullable = false)
    val theme: String = "light",

    @Column(nullable = false)
    val locationEnabled: Boolean = false,

    @ElementCollection
    @CollectionTable(name = "user_alert_types", joinColumns = [JoinColumn(name = "preferences_id")])
    @Column(name = "alert_type")
    val alertTypes: MutableList<String> = mutableListOf("health", "service", "system")
)

@Entity
@Table(name = "vehicles")
data class Vehicle(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User? = null,

    @Column(nullable = false)
    val make: String = "",

    @Column(nullable = false)
    val model: String = "",

    @Column(nullable = false)
    val year: Int = 0,

    @Column(nullable = false)
    val tireSize: String = "",

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "vehicle", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tires: MutableList<Tire> = mutableListOf()
)

@Entity
@Table(name = "tires")
data class Tire(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    val vehicle: Vehicle? = null,

    @Column(nullable = false)
    val position: String = "", // front-left, front-right, rear-left, rear-right

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val currentHealthScore: Int = 0,

    @Column
    val lastAnalysisDate: LocalDateTime? = null,

    @OneToMany(mappedBy = "tire", cascade = [CascadeType.ALL], orphanRemoval = true)
    val analyses: MutableList<TireAnalysis> = mutableListOf()
)
