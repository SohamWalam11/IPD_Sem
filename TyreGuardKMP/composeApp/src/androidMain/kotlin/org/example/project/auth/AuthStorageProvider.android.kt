package org.example.project.auth

import org.example.project.data.AuthStorage

/**
 * Android implementation - returns the AuthStorage object
 */
actual fun getAuthStorageProvider(): AuthStorageProvider = AuthStorage
