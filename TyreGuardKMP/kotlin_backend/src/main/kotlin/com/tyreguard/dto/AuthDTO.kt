package com.tyreguard.dto

data class SignUpRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val userId: String,
    val token: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: Map<String, Any?>
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class PasswordResetRequest(
    val email: String
)

data class PasswordResetConfirmRequest(
    val token: String,
    val newPassword: String
)
