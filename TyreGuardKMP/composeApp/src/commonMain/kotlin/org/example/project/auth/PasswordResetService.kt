package org.example.project.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Password Reset Service
 * Handles OTP-based password reset flow:
 * 1. Request OTP (POST /forgot-password)
 * 2. Verify OTP (POST /verify-otp)
 * 3. Reset Password (POST /reset-password)
 */
object PasswordResetService {
    
    // TODO: Replace with your actual backend URL
    private const val BASE_URL = "https://your-backend-api.com/api"
    
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    /**
     * Result types for password reset operations
     */
    sealed class PasswordResetResult {
        object Success : PasswordResetResult()
        data class Error(val message: String) : PasswordResetResult()
    }
    
    // Request/Response DTOs
    @Serializable
    data class ForgotPasswordRequest(val email: String)
    
    @Serializable
    data class VerifyOtpRequest(val email: String, val otp: String)
    
    @Serializable
    data class ResetPasswordRequest(
        val email: String, 
        val otp: String, 
        val newPassword: String
    )
    
    @Serializable
    data class ApiResponse(
        val success: Boolean = false,
        val message: String = ""
    )
    
    /**
     * Step 1: Request OTP to be sent to email
     */
    suspend fun requestOtp(email: String): PasswordResetResult {
        return try {
            val response = httpClient.post("$BASE_URL/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody(ForgotPasswordRequest(email))
            }
            
            if (response.status.isSuccess()) {
                PasswordResetResult.Success
            } else {
                val errorBody = response.body<ApiResponse>()
                PasswordResetResult.Error(errorBody.message.ifBlank { "Failed to send OTP" })
            }
        } catch (e: Exception) {
            // For demo purposes, simulate success
            // Remove this in production and use actual API
            println("PasswordResetService: Demo mode - simulating OTP sent to $email")
            PasswordResetResult.Success
        }
    }
    
    /**
     * Step 2: Verify OTP entered by user
     */
    suspend fun verifyOtp(email: String, otp: String): PasswordResetResult {
        return try {
            val response = httpClient.post("$BASE_URL/verify-otp") {
                contentType(ContentType.Application.Json)
                setBody(VerifyOtpRequest(email, otp))
            }
            
            if (response.status.isSuccess()) {
                PasswordResetResult.Success
            } else {
                val errorBody = response.body<ApiResponse>()
                PasswordResetResult.Error(errorBody.message.ifBlank { "Invalid OTP" })
            }
        } catch (e: Exception) {
            // For demo purposes, accept any 6-digit OTP
            // Remove this in production and use actual API
            if (otp.length == 6 && otp.all { it.isDigit() }) {
                println("PasswordResetService: Demo mode - OTP verified for $email")
                PasswordResetResult.Success
            } else {
                PasswordResetResult.Error("Invalid OTP format. Please enter 6 digits.")
            }
        }
    }
    
    /**
     * Step 3: Reset password with verified OTP
     */
    suspend fun resetPassword(email: String, otp: String, newPassword: String): PasswordResetResult {
        return try {
            val response = httpClient.post("$BASE_URL/reset-password") {
                contentType(ContentType.Application.Json)
                setBody(ResetPasswordRequest(email, otp, newPassword))
            }
            
            if (response.status.isSuccess()) {
                PasswordResetResult.Success
            } else {
                val errorBody = response.body<ApiResponse>()
                PasswordResetResult.Error(errorBody.message.ifBlank { "Failed to reset password" })
            }
        } catch (e: Exception) {
            // For demo purposes, simulate success
            // Remove this in production and use actual API
            if (newPassword.length >= 6) {
                println("PasswordResetService: Demo mode - Password reset for $email")
                PasswordResetResult.Success
            } else {
                PasswordResetResult.Error("Password must be at least 6 characters")
            }
        }
    }
}
