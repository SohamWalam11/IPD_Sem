package com.tyreguard.service

import com.tyreguard.dto.LoginRequest
import com.tyreguard.dto.SignUpRequest
import com.tyreguard.dto.AuthResponse
import com.tyreguard.model.User
import com.tyreguard.model.UserPreferences
import com.tyreguard.repository.UserRepository
import com.tyreguard.security.JwtProvider
import mu.KotlinLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) {

    fun signUp(request: SignUpRequest): AuthResponse {
        logger.info("Attempting to sign up user with email: ${request.email}")

        // Check if user already exists
        if (userRepository.existsByEmail(request.email)) {
            logger.warn("User with email ${request.email} already exists")
            throw IllegalArgumentException("User with this email already exists")
        }

        // Validate password strength
        if (request.password.length < 8) {
            throw IllegalArgumentException("Password must be at least 8 characters long")
        }

        // Create new user
        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            firstName = request.firstName,
            lastName = request.lastName,
            preferences = UserPreferences(
                notificationsEnabled = true,
                alertThreshold = 40,
                theme = "light",
                locationEnabled = false,
                alertTypes = mutableListOf("health", "service", "system")
            )
        )

        val savedUser = userRepository.save(user)
        logger.info("User ${savedUser.id} created successfully")

        // Generate tokens
        val token = jwtProvider.generateToken(savedUser.id, savedUser.email)
        val refreshToken = jwtProvider.generateRefreshToken(savedUser.id)

        return AuthResponse(
            userId = savedUser.id,
            token = token,
            refreshToken = refreshToken,
            expiresIn = 86400, // 24 hours
            user = savedUser.toDTO()
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        logger.info("Attempting to login user with email: ${request.email}")

        val user = userRepository.findByEmail(request.email)
            .orElseThrow { IllegalArgumentException("Invalid email or password") }

        // Verify password
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            logger.warn("Invalid password for user ${user.id}")
            throw IllegalArgumentException("Invalid email or password")
        }

        logger.info("User ${user.id} logged in successfully")

        // Generate tokens
        val token = jwtProvider.generateToken(user.id, user.email)
        val refreshToken = jwtProvider.generateRefreshToken(user.id)

        return AuthResponse(
            userId = user.id,
            token = token,
            refreshToken = refreshToken,
            expiresIn = 86400, // 24 hours
            user = user.toDTO()
        )
    }

    fun refreshToken(userId: String): AuthResponse {
        logger.info("Refreshing token for user: $userId")

        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val token = jwtProvider.generateToken(user.id, user.email)
        val refreshToken = jwtProvider.generateRefreshToken(user.id)

        return AuthResponse(
            userId = user.id,
            token = token,
            refreshToken = refreshToken,
            expiresIn = 86400,
            user = user.toDTO()
        )
    }

    fun validateToken(token: String): Boolean {
        return jwtProvider.validateToken(token)
    }

    fun getUserById(userId: String): User {
        return userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
    }

    private fun User.toDTO() = mapOf(
        "id" to id,
        "email" to email,
        "firstName" to firstName,
        "lastName" to lastName,
        "phoneNumber" to phoneNumber,
        "profileImageUrl" to profileImageUrl,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}
