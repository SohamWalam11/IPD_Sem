package com.tyreguard.controller

import com.tyreguard.dto.LoginRequest
import com.tyreguard.dto.SignUpRequest
import com.tyreguard.dto.AuthResponse
import com.tyreguard.dto.RefreshTokenRequest
import com.tyreguard.service.AuthService
import com.tyreguard.security.JwtProvider
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = ["*"], maxAge = 3600)
class AuthController(
    private val authService: AuthService,
    private val jwtProvider: JwtProvider
) {

    @PostMapping("/signup")
    fun signUp(@RequestBody request: SignUpRequest): ResponseEntity<AuthResponse> {
        return try {
            logger.info("Sign up request for email: ${request.email}")
            val response = authService.signUp(request)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: IllegalArgumentException) {
            logger.error("Sign up failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        } catch (e: Exception) {
            logger.error("Sign up error", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        return try {
            logger.info("Login request for email: ${request.email}")
            val response = authService.login(request)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            logger.error("Login failed: ${e.message}")
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        } catch (e: Exception) {
            logger.error("Login error", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/refresh")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        return try {
            logger.info("Token refresh request")
            // In a real implementation, validate the refresh token
            val userId = SecurityContextHolder.getContext().authentication.principal as String
            val response = authService.refreshToken(userId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Token refresh error", e)
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    @GetMapping("/validate")
    fun validateToken(): ResponseEntity<Map<String, Any>> {
        return try {
            val userId = SecurityContextHolder.getContext().authentication.principal as String
            val user = authService.getUserById(userId)
            
            ResponseEntity.ok(mapOf(
                "valid" to true,
                "user" to mapOf(
                    "id" to user.id,
                    "email" to user.email,
                    "firstName" to user.firstName,
                    "lastName" to user.lastName
                )
            ))
        } catch (e: Exception) {
            logger.error("Token validation error", e)
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "ok"))
    }
}
