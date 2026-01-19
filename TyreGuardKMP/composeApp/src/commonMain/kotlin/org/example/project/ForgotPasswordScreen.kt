package org.example.project

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.auth.PasswordResetService

// Premium theme colors (matching LoginScreen)
private val PrimaryViolet = Color(0xFF6200EA)
private val SecondaryPurple = Color(0xFFBB86FC)
private val DeepViolet = Color(0xFF3700B3)
private val DarkViolet = Color(0xFF4A148C)
private val LightLavender = Color(0xFFF3E5F5)
private val SoftWhite = Color(0xFFFAFAFA)
private val SuccessGreen = Color(0xFF4CAF50)

private val BrandGradient = Brush.horizontalGradient(
    colors = listOf(PrimaryViolet, DeepViolet)
)

/**
 * Password reset step states
 */
enum class ResetStep {
    EnterEmail,    // Step 1: Enter email to receive OTP
    EnterOTP,      // Step 2: Enter OTP from email
    NewPassword,   // Step 3: Enter new password
    Success        // Complete!
}

/**
 * Forgot Password Screen with OTP verification flow
 * 
 * Flow:
 * 1. User enters email → Sends OTP to email
 * 2. User enters OTP → Verifies OTP
 * 3. User enters new password → Resets password
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    modifier: Modifier = Modifier,
    onBackToLogin: () -> Unit
) {
    var currentStep by remember { mutableStateOf(ResetStep.EnterEmail) }
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Background with gradient
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LightLavender, SoftWhite, LightLavender.copy(alpha = 0.5f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToLogin) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DarkViolet
                    )
                }
                
                Text(
                    text = when (currentStep) {
                        ResetStep.EnterEmail -> "Forgot Password"
                        ResetStep.EnterOTP -> "Verify OTP"
                        ResetStep.NewPassword -> "New Password"
                        ResetStep.Success -> "Success!"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkViolet
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Progress indicator
            StepProgressIndicator(currentStep = currentStep)
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Content based on current step
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    ResetStep.EnterEmail -> EmailInputStep(
                        email = email,
                        onEmailChange = { email = it; errorMessage = null },
                        errorMessage = errorMessage,
                        isLoading = isLoading,
                        onSubmit = {
                            if (email.isBlank() || !email.contains("@")) {
                                errorMessage = "Please enter a valid email address"
                                return@EmailInputStep
                            }
                            
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                
                                when (val result = PasswordResetService.requestOtp(email)) {
                                    is PasswordResetService.PasswordResetResult.Success -> {
                                        currentStep = ResetStep.EnterOTP
                                    }
                                    is PasswordResetService.PasswordResetResult.Error -> {
                                        errorMessage = result.message
                                    }
                                }
                                
                                isLoading = false
                            }
                        }
                    )
                    
                    ResetStep.EnterOTP -> OtpInputStep(
                        email = email,
                        otp = otp,
                        onOtpChange = { if (it.length <= 6) otp = it; errorMessage = null },
                        errorMessage = errorMessage,
                        isLoading = isLoading,
                        onSubmit = {
                            if (otp.length != 6) {
                                errorMessage = "Please enter the 6-digit OTP"
                                return@OtpInputStep
                            }
                            
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                
                                when (val result = PasswordResetService.verifyOtp(email, otp)) {
                                    is PasswordResetService.PasswordResetResult.Success -> {
                                        currentStep = ResetStep.NewPassword
                                    }
                                    is PasswordResetService.PasswordResetResult.Error -> {
                                        errorMessage = result.message
                                    }
                                }
                                
                                isLoading = false
                            }
                        },
                        onResendOtp = {
                            coroutineScope.launch {
                                isLoading = true
                                PasswordResetService.requestOtp(email)
                                isLoading = false
                                // Show a snackbar or toast here in production
                            }
                        }
                    )
                    
                    ResetStep.NewPassword -> NewPasswordStep(
                        newPassword = newPassword,
                        confirmPassword = confirmPassword,
                        passwordVisible = passwordVisible,
                        onNewPasswordChange = { newPassword = it; errorMessage = null },
                        onConfirmPasswordChange = { confirmPassword = it; errorMessage = null },
                        onToggleVisibility = { passwordVisible = !passwordVisible },
                        errorMessage = errorMessage,
                        isLoading = isLoading,
                        onSubmit = {
                            if (newPassword.length < 6) {
                                errorMessage = "Password must be at least 6 characters"
                                return@NewPasswordStep
                            }
                            if (newPassword != confirmPassword) {
                                errorMessage = "Passwords don't match"
                                return@NewPasswordStep
                            }
                            
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                
                                when (val result = PasswordResetService.resetPassword(email, otp, newPassword)) {
                                    is PasswordResetService.PasswordResetResult.Success -> {
                                        currentStep = ResetStep.Success
                                    }
                                    is PasswordResetService.PasswordResetResult.Error -> {
                                        errorMessage = result.message
                                    }
                                }
                                
                                isLoading = false
                            }
                        }
                    )
                    
                    ResetStep.Success -> SuccessStep(
                        onBackToLogin = onBackToLogin
                    )
                }
            }
        }
    }
}

/**
 * Step progress indicator showing 1 → 2 → 3 → ✓
 */
@Composable
private fun StepProgressIndicator(currentStep: ResetStep) {
    val steps = listOf("Email", "OTP", "Password", "Done")
    val currentIndex = ResetStep.entries.indexOf(currentStep)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val isCompleted = index < currentIndex
            val isCurrent = index == currentIndex
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step circle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                isCompleted -> SuccessGreen
                                isCurrent -> PrimaryViolet
                                else -> Color.LightGray
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            color = if (isCurrent) Color.White else Color.DarkGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isCompleted -> SuccessGreen
                        isCurrent -> PrimaryViolet
                        else -> Color.Gray
                    }
                )
            }
            
            // Connector line (not after last step)
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(2.dp)
                        .background(
                            if (index < currentIndex) SuccessGreen else Color.LightGray
                        )
                )
            }
        }
    }
}

/**
 * Step 1: Email Input
 */
@Composable
private fun EmailInputStep(
    email: String,
    onEmailChange: (String) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        Icon(
            Icons.Filled.Email,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = PrimaryViolet
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Enter your email address",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = DarkViolet
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "We'll send a 6-digit OTP to verify your identity",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Error message
        AnimatedVisibility(visible = errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Email input
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            leadingIcon = {
                Icon(Icons.Filled.Email, contentDescription = null, tint = PrimaryViolet)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = PrimaryViolet
            ),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Submit button
        GradientButton(
            text = "Send OTP",
            isLoading = isLoading,
            onClick = onSubmit
        )
    }
}

/**
 * Step 2: OTP Input
 */
@Composable
private fun OtpInputStep(
    email: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onResendOtp: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        Icon(
            Icons.Filled.Pin,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = PrimaryViolet
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Enter OTP",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = DarkViolet
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "We've sent a 6-digit code to\n$email",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Error message
        AnimatedVisibility(visible = errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // OTP input with large centered digits
        OutlinedTextField(
            value = otp,
            onValueChange = { if (it.all { c -> c.isDigit() }) onOtpChange(it) },
            label = { Text("6-Digit OTP") },
            leadingIcon = {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryViolet)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = PrimaryViolet
            ),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Resend OTP link
        TextButton(
            onClick = onResendOtp,
            enabled = !isLoading
        ) {
            Text(
                text = "Didn't receive code? Resend OTP",
                color = PrimaryViolet
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Submit button
        GradientButton(
            text = "Verify OTP",
            isLoading = isLoading,
            onClick = onSubmit
        )
    }
}

/**
 * Step 3: New Password Input
 */
@Composable
private fun NewPasswordStep(
    newPassword: String,
    confirmPassword: String,
    passwordVisible: Boolean,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        Icon(
            Icons.Filled.LockReset,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = PrimaryViolet
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Create New Password",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = DarkViolet
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your new password must be at least 6 characters",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Error message
        AnimatedVisibility(visible = errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // New password input
        OutlinedTextField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            label = { Text("New Password") },
            leadingIcon = {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryViolet)
            },
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = PrimaryViolet
            ),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Confirm password input
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            leadingIcon = {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryViolet)
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = PrimaryViolet
            ),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Submit button
        GradientButton(
            text = "Reset Password",
            isLoading = isLoading,
            onClick = onSubmit
        )
    }
}

/**
 * Success Step - Password reset complete
 */
@Composable
private fun SuccessStep(
    onBackToLogin: () -> Unit
) {
    // Success animation
    val infiniteTransition = rememberInfiniteTransition(label = "success")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Success icon with animation
        Box(
            modifier = Modifier
                .size((80 * scale).dp)
                .clip(RoundedCornerShape(50))
                .background(SuccessGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Password Reset Successfully!",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = SuccessGreen
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Your password has been updated.\nYou can now sign in with your new password.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Back to login button
        GradientButton(
            text = "Back to Sign In",
            isLoading = false,
            onClick = onBackToLogin
        )
    }
}

/**
 * Premium gradient button
 */
@Composable
private fun GradientButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(12.dp),
        enabled = !isLoading
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isLoading) Modifier.background(Color.Gray)
                    else Modifier.background(BrandGradient)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
