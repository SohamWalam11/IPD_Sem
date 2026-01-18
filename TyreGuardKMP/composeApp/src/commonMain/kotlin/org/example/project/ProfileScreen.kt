package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.auth.SupabaseAuthService
import org.example.project.data.UserDataRepository
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * User profile data for display (deprecated - use UserDataRepository instead)
 */
data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val vehicleName: String = "",
    val vehicleNumber: String = ""
)

/**
 * Profile Screen - Accessible from Dashboard header click.
 * Displays user information from UserDataRepository, vehicle details, and settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile = UserProfile(), // Fallback, prefer repository data
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    // Collect user data from repository
    val userProfileData by UserDataRepository.userProfile.collectAsState()
    val authUser by SupabaseAuthService.currentUser.collectAsState()
    
    // Build the display profile from repository data with fallbacks
    val displayName = userProfileData?.name?.ifBlank { null }
        ?: userProfileData?.displayName?.ifBlank { null }
        ?: authUser?.displayName
        ?: userProfile.name.ifBlank { "User" }
    
    val displayEmail = userProfileData?.email?.ifBlank { null }
        ?: authUser?.email
        ?: userProfile.email.ifBlank { "Not set" }
    
    val displayPhone = userProfileData?.mobileNumber?.let { 
        if (it.isNotBlank()) "+91 $it" else null 
    } ?: userProfile.phone.ifBlank { "Not set" }
    
    val displayVehicle = buildString {
        val brand = userProfileData?.vehicleBrand ?: ""
        val model = userProfileData?.carModel ?: ""
        if (brand.isNotBlank()) append(brand)
        if (model.isNotBlank()) {
            if (isNotBlank()) append(" ")
            append(model)
        }
    }.ifBlank { userProfile.vehicleName.ifBlank { "Not set" } }
    
    val displayPlateNumber = userProfileData?.fullPlateNumber?.ifBlank { null }
        ?: userProfile.vehicleNumber.ifBlank { "Not set" }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditProfile) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Profile"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ─────────────────────────────────────────────────────────────────
            // User Avatar Section
            // ─────────────────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large Avatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = displayEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // Contact Info Card
            // ─────────────────────────────────────────────────────────────────
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Contact Information",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        
                        ProfileInfoRow(
                            icon = Icons.Filled.Email,
                            label = "Email",
                            value = displayEmail
                        )
                        
                        ProfileInfoRow(
                            icon = Icons.Filled.Phone,
                            label = "Phone",
                            value = displayPhone
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // Vehicle Info Card
            // ─────────────────────────────────────────────────────────────────
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Vehicle Information",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        
                        ProfileInfoRow(
                            icon = Icons.Filled.DirectionsCar,
                            label = "Vehicle",
                            value = displayVehicle
                        )
                        
                        ProfileInfoRow(
                            icon = Icons.Filled.Numbers,
                            label = "Number",
                            value = displayPlateNumber
                        )
                        
                        // Additional vehicle info if available
                        userProfileData?.let { data ->
                            if (data.yearOfManufacture != null) {
                                ProfileInfoRow(
                                    icon = Icons.Filled.CalendarMonth,
                                    label = "Year",
                                    value = data.yearOfManufacture.toString()
                                )
                            }
                            if (data.fuelVariant.isNotBlank()) {
                                ProfileInfoRow(
                                    icon = Icons.Filled.LocalGasStation,
                                    label = "Fuel Type",
                                    value = data.fuelVariant
                                )
                            }
                            if (data.odometerReading.isNotBlank()) {
                                ProfileInfoRow(
                                    icon = Icons.Filled.Speed,
                                    label = "Odometer",
                                    value = "${data.odometerReading} km"
                                )
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // Settings Section
            // ─────────────────────────────────────────────────────────────────
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    subtitle = "Manage notification preferences",
                    onClick = onNotificationsClick
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Filled.Language,
                    title = "Language",
                    subtitle = "English",
                    onClick = onLanguageClick
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Filled.Help,
                    title = "Help & Support",
                    subtitle = "FAQs, Contact us",
                    onClick = onHelpClick
                )
            }

            // ─────────────────────────────────────────────────────────────────
            // Logout Button
            // ─────────────────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout")
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
@Preview
private fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(
            userProfile = UserProfile(),
            onBackClick = {},
            onEditProfile = {},
            onLogout = {}
        )
    }
}
