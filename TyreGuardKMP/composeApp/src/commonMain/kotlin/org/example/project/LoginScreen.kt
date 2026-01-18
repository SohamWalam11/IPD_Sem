package org.example.project

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.auth.AuthResult
import org.example.project.auth.SupabaseAuthService
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ============ PREMIUM THEME COLORS ============
private val PrimaryViolet = Color(0xFF6200EA)
private val SecondaryPurple = Color(0xFFBB86FC)
private val DeepViolet = Color(0xFF3700B3)
private val DarkViolet = Color(0xFF4A148C)
private val LightLavender = Color(0xFFF3E5F5)
private val SoftWhite = Color(0xFFFAFAFA)

// Premium gradients for brand identity
private val BrandGradient = Brush.horizontalGradient(
    colors = listOf(PrimaryViolet, DeepViolet)
)

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(LightLavender, SoftWhite, LightLavender.copy(alpha = 0.5f))
)

/**
 * Authentication mode - Sign In or Sign Up
 */
enum class AuthMode {
    SignIn,
    SignUp
}

/**
 * Enhanced Login state with auth mode
 */
sealed class EnhancedLoginState {
    object Idle : EnhancedLoginState()
    object Loading : EnhancedLoginState()
    object BiometricSetup : EnhancedLoginState()
    object Success : EnhancedLoginState()
    data class Error(val message: String) : EnhancedLoginState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit
) {
    var loginState by remember { mutableStateOf<EnhancedLoginState>(EnhancedLoginState.Idle) }
    var authMode by remember { mutableStateOf(AuthMode.SignIn) }
    var hasQuickLoginEnabled by remember { mutableStateOf(false) }
    
    // Form fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Error states
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Start animation trigger for tyre rolling in
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }
    
    // Mesh gradient animation - breathing effect
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_gradient")
    
    val meshOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mesh1"
    )
    
    val meshOffset2 by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mesh2"
    )
    
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )
    
    // Handle biometric sheet
    val showBiometricSheet = loginState is EnhancedLoginState.BiometricSetup
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Animated Mesh Gradient Background
        MeshGradientBackground(
            meshOffset1 = meshOffset1,
            meshOffset2 = meshOffset2,
            breathingAlpha = breathingAlpha
        )
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top section - Rolling Tyre Logo with animation
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))
                
                // ANIMATION: Tyre rolls in from left and keeps spinning
                AnimatedVisibility(
                    visible = startAnimation,
                    enter = slideInHorizontally(
                        initialOffsetX = { -it * 2 },
                        animationSpec = tween(1000, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(800))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RollingTyreAnimation(
                            modifier = Modifier.size(100.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "TyreGuard",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkViolet
                            )
                        )
                        
                        Text(
                            text = "Your personal pit crew",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Gray
                            )
                        )
                    }
                }
            }
            
            // Middle section - Auth form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Premium Pill-Style Auth Mode Toggle
                PillAuthModeToggle(
                    currentMode = authMode,
                    onModeChange = { 
                        authMode = it
                        generalError = null
                        emailError = null
                        passwordError = null
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Error banner
                AnimatedVisibility(
                    visible = generalError != null || (loginState is EnhancedLoginState.Error),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val errorMsg = generalError ?: (loginState as? EnhancedLoginState.Error)?.message ?: ""
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Loading state
                if (loginState is EnhancedLoginState.Loading) {
                    CircularProgressIndicator(color = PrimaryViolet)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (authMode == AuthMode.SignIn) "Signing in..." else "Creating account...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // Display name field (Sign Up only) with AnimatedContent transition
                    AnimatedVisibility(
                        visible = authMode == AuthMode.SignUp,
                        enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200))
                    ) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name") },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryViolet)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryViolet,
                                unfocusedBorderColor = Color.LightGray,
                                focusedLabelColor = PrimaryViolet
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Email field with premium styling
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            emailError = null
                        },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = PrimaryViolet)
                        },
                        isError = emailError != null,
                        supportingText = emailError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryViolet,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = PrimaryViolet
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Password field with premium styling
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            passwordError = null
                        },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryViolet)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        isError = passwordError != null,
                        supportingText = passwordError?.let { { Text(it) } },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (authMode == AuthMode.SignUp) ImeAction.Next else ImeAction.Done
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryViolet,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = PrimaryViolet
                        )
                    )
                    
                    // Confirm password field (Sign Up only) with smooth transition
                    AnimatedVisibility(
                        visible = authMode == AuthMode.SignUp,
                        enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
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
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Main action button
                    Button(
                        onClick = {
                            // Validate
                            var isValid = true
                            
                            if (email.isBlank() || !email.contains("@")) {
                                emailError = "Please enter a valid email"
                                isValid = false
                            }
                            
                            if (password.length < 6) {
                                passwordError = "Password must be at least 6 characters"
                                isValid = false
                            }
                            
                            if (authMode == AuthMode.SignUp && password != confirmPassword) {
                                passwordError = "Passwords don't match"
                                isValid = false
                            }
                            
                            if (isValid) {
                                coroutineScope.launch {
                                    loginState = EnhancedLoginState.Loading
                                    generalError = null
                                    
                                    val result = if (authMode == AuthMode.SignUp) {
                                        SupabaseAuthService.signUp(
                                            email = email,
                                            password = password,
                                            displayName = displayName.ifBlank { null }
                                        )
                                    } else {
                                        SupabaseAuthService.signIn(email, password)
                                    }
                                    
                                    when (result) {
                                        is AuthResult.Success -> {
                                            loginState = EnhancedLoginState.BiometricSetup
                                        }
                                        is AuthResult.UserAlreadyExists -> {
                                            loginState = EnhancedLoginState.Error("User already exists! Please sign in instead.")
                                            generalError = "User already exists! Please sign in instead."
                                        }
                                        is AuthResult.InvalidCredentials -> {
                                            loginState = EnhancedLoginState.Error(result.message)
                                            generalError = result.message
                                        }
                                        is AuthResult.Error -> {
                                            loginState = EnhancedLoginState.Error(result.message)
                                            generalError = result.message
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        // Gradient Button Interior
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BrandGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (authMode == AuthMode.SignIn) "SIGN IN" else "CREATE ACCOUNT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Divider with "OR"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                        Text(
                            text = "  OR  ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Social login buttons with premium styling
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                loginState = EnhancedLoginState.Loading
                                val result = SupabaseAuthService.signInWithGoogle()
                                when (result) {
                                    is AuthResult.Success -> {
                                        loginState = EnhancedLoginState.BiometricSetup
                                    }
                                    else -> {
                                        loginState = EnhancedLoginState.Error("Google sign in failed")
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.LightGray, Color.LightGray)
                            )
                        )
                    ) {
                        Text("G", modifier = Modifier.padding(end = 8.dp), color = PrimaryViolet, fontWeight = FontWeight.Bold)
                        Text("Continue with Google", color = Color.DarkGray)
                    }
                }
            }
            
            // Bottom section - biometric indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hasQuickLoginEnabled) "🔒" else "🔓",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (hasQuickLoginEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                if (hasQuickLoginEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Quick Login enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Biometric setup bottom sheet
        if (showBiometricSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    loginState = EnhancedLoginState.Success
                    onLoginSuccess()
                },
                sheetState = bottomSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔐",
                        fontSize = 48.sp
                    )
                    
                    Text(
                        text = "Enable Quick Login?",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Use biometrics to log in instantly next time. Fast, secure, and convenient.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Gradient biometric button
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .padding(top = 4.dp),
                        onClick = {
                            coroutineScope.launch {
                                delay(1500)
                                hasQuickLoginEnabled = true
                                loginState = EnhancedLoginState.Success
                                onLoginSuccess()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BrandGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enable Biometrics", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    TextButton(
                        onClick = {
                            loginState = EnhancedLoginState.Success
                            onLoginSuccess()
                        }
                    ) {
                        Text("Skip for now", color = PrimaryViolet)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Premium Pill-Style Auth Mode Toggle with AnimatedContent
 */
@Composable
private fun PillAuthModeToggle(
    currentMode: AuthMode,
    onModeChange: (AuthMode) -> Unit
) {
    Surface(
        modifier = Modifier.padding(bottom = 24.dp),
        shape = RoundedCornerShape(50),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            PillTabButton(
                text = "Sign In",
                isSelected = currentMode == AuthMode.SignIn,
                onClick = { onModeChange(AuthMode.SignIn) }
            )
            PillTabButton(
                text = "Sign Up",
                isSelected = currentMode == AuthMode.SignUp,
                onClick = { onModeChange(AuthMode.SignUp) }
            )
        }
    }
}

@Composable
private fun PillTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryViolet else Color.Transparent,
        animationSpec = tween(300),
        label = "tab_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.Gray,
        animationSpec = tween(300),
        label = "tab_content"
    )
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = null,
        shape = RoundedCornerShape(50),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

/**
 * Old Auth Mode Toggle - kept for compatibility
 */
@Composable
private fun AuthModeToggle(
    currentMode: AuthMode,
    onModeChange: (AuthMode) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(4.dp)
        ) {
            AuthModeTab(
                text = "Sign In",
                isSelected = currentMode == AuthMode.SignIn,
                onClick = { onModeChange(AuthMode.SignIn) },
                modifier = Modifier.weight(1f)
            )
            AuthModeTab(
                text = "Sign Up",
                isSelected = currentMode == AuthMode.SignUp,
                onClick = { onModeChange(AuthMode.SignUp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AuthModeTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        onClick = onClick
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            color = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
@Preview
private fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(onLoginSuccess = {})
    }
}

/**
 * Animated 3D-style tyre for the logo with rotation
 */
@Composable
private fun AnimatedLogoTyre(
    rotation: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val outerRadius = size.minDimension / 2 - 4f
        val innerRadius = outerRadius * 0.35f
        val rimRadius = outerRadius * 0.7f
        
        rotate(rotation, pivot = Offset(centerX, centerY)) {
            // Outer tyre ring with gradient effect
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF2D2D2D),
                        Color(0xFF1A1A1A)
                    ),
                    center = Offset(centerX, centerY),
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = Offset(centerX, centerY)
            )
            
            // Tread pattern - dynamic grooves
            val grooveCount = 16
            for (i in 0 until grooveCount) {
                val angle = (i * 360f / grooveCount) * (Math.PI / 180f)
                val startX = centerX + cos(angle).toFloat() * rimRadius
                val startY = centerY + sin(angle).toFloat() * rimRadius
                val endX = centerX + cos(angle).toFloat() * outerRadius * 0.95f
                val endY = centerY + sin(angle).toFloat() * outerRadius * 0.95f
                
                drawLine(
                    color = Color(0xFF0D0D0D),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
            
            // Silver rim
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFC0C0C0),
                        Color(0xFF808080),
                        Color(0xFFE8E8E8),
                        Color(0xFF606060)
                    ),
                    start = Offset(centerX - rimRadius, centerY - rimRadius),
                    end = Offset(centerX + rimRadius, centerY + rimRadius)
                ),
                radius = rimRadius,
                center = Offset(centerX, centerY)
            )
            
            // Rim spokes
            val spokeCount = 5
            for (i in 0 until spokeCount) {
                val angle = (i * 360f / spokeCount) * (Math.PI / 180f)
                val startX = centerX + cos(angle).toFloat() * (innerRadius + 8f)
                val startY = centerY + sin(angle).toFloat() * (innerRadius + 8f)
                val endX = centerX + cos(angle).toFloat() * (rimRadius - 8f)
                val endY = centerY + sin(angle).toFloat() * (rimRadius - 8f)
                
                // Spoke shadow
                drawLine(
                    color = Color(0xFF404040),
                    start = Offset(startX + 2f, startY + 2f),
                    end = Offset(endX + 2f, endY + 2f),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
                
                // Spoke
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFD0D0D0), Color(0xFF909090)),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY)
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
            }
            
            // Center hub
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor,
                        primaryColor.copy(alpha = 0.8f),
                        secondaryColor
                    ),
                    center = Offset(centerX, centerY),
                    radius = innerRadius
                ),
                radius = innerRadius,
                center = Offset(centerX, centerY)
            )
            
            // Center highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = innerRadius * 0.5f,
                center = Offset(centerX - innerRadius * 0.2f, centerY - innerRadius * 0.2f)
            )
        }
        
        // Rim ring outline
        drawCircle(
            color = Color(0xFF404040),
            radius = rimRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )
    }
}

/**
 * Animated floating tyre background elements
 */
@Composable
private fun AnimatedTyreBackground(
    rotation: Float,
    secondaryRotation: Float,
    pulseScale: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val secondaryColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
    
    Box(modifier = modifier.fillMaxSize()) {
        // Top-left floating tyre
        Canvas(
            modifier = Modifier
                .size(180.dp)
                .offset(x = (-40).dp, y = (-20).dp)
                .rotate(rotation)
                .scale(pulseScale * 0.9f)
                .alpha(0.4f)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2 - 8f
            
            drawCircle(
                color = primaryColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 24f)
            )
            
            drawCircle(
                color = primaryColor,
                radius = radius * 0.6f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 12f)
            )
        }
        
        // Top-right floating tyre
        Canvas(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = 100.dp)
                .rotate(secondaryRotation)
                .scale(pulseScale)
                .alpha(0.3f)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2 - 8f
            
            drawCircle(
                color = secondaryColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 16f)
            )
            
            drawCircle(
                color = secondaryColor,
                radius = radius * 0.5f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 8f)
            )
        }
        
        // Bottom-left floating tyre
        Canvas(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.BottomStart)
                .offset(x = 20.dp, y = (-150).dp)
                .rotate(rotation * 0.7f)
                .alpha(0.25f)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2 - 8f
            
            drawCircle(
                color = secondaryColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 12f)
            )
        }
        
        // Bottom-right larger floating tyre
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 40.dp)
                .rotate(secondaryRotation * 0.5f)
                .scale(pulseScale * 0.85f)
                .alpha(0.2f)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2 - 8f
            
            drawCircle(
                color = primaryColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 28f)
            )
            
            drawCircle(
                color = primaryColor,
                radius = radius * 0.65f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 14f)
            )
            
            drawCircle(
                color = primaryColor,
                radius = radius * 0.35f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 6f)
            )
        }
        
        // Center decorative particles/speed lines
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.1f)
        ) {
            val particleCount = 8
            for (i in 0 until particleCount) {
                val angle = ((rotation + i * 45f) % 360f) * (Math.PI / 180f)
                val startRadius = size.minDimension * 0.3f
                val endRadius = size.minDimension * 0.4f
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                val startX = centerX + cos(angle).toFloat() * startRadius
                val startY = centerY + sin(angle).toFloat() * startRadius
                val endX = centerX + cos(angle).toFloat() * endRadius
                val endY = centerY + sin(angle).toFloat() * endRadius
                
                drawLine(
                    color = primaryColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Rolling Tyre Animation - Continuously spinning tyre with premium design
 * This creates the "arriving at pit stop" effect
 */
@Composable
private fun RollingTyreAnimation(
    modifier: Modifier = Modifier
) {
    // Infinite transition for continuous rotation
    val infiniteTransition = rememberInfiniteTransition(label = "tyre_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.size(100.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val outerRadius = size.minDimension / 2 - 4f
        val strokeWidth = 12.dp.toPx()
        val rimRadius = outerRadius - strokeWidth
        val hubRadius = outerRadius / 3

        rotate(rotation, pivot = Offset(centerX, centerY)) {
            // 1. Draw Tyre Rubber (Black Outer Circle with gradient)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF212121),
                        Color(0xFF1A1A1A),
                        Color(0xFF0D0D0D)
                    ),
                    center = Offset(centerX, centerY),
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = Offset(centerX, centerY)
            )
            
            // Inner cutout for rim area
            drawCircle(
                color = Color.Transparent,
                radius = rimRadius,
                center = Offset(centerX, centerY)
            )

            // 2. Draw Treads (grooves in the tyre)
            for (i in 0 until 12) {
                val angle = (i * 30f) * (Math.PI / 180f)
                val startX = centerX + cos(angle).toFloat() * rimRadius
                val startY = centerY + sin(angle).toFloat() * rimRadius
                val endX = centerX + cos(angle).toFloat() * (outerRadius - 2f)
                val endY = centerY + sin(angle).toFloat() * (outerRadius - 2f)
                
                drawLine(
                    color = Color(0xFF0A0A0A),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw Rim (Metallic silver inner circle with gradient)
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE8E8E8),
                        Color(0xFFC0C0C0),
                        Color(0xFF909090),
                        Color(0xFFD0D0D0)
                    ),
                    start = Offset(centerX - rimRadius, centerY - rimRadius),
                    end = Offset(centerX + rimRadius, centerY + rimRadius)
                ),
                radius = rimRadius - 2f,
                center = Offset(centerX, centerY)
            )
            
            // 4. Rim spokes
            for (i in 0 until 5) {
                val angle = (i * 72f) * (Math.PI / 180f)
                val startX = centerX + cos(angle).toFloat() * (hubRadius + 4f)
                val startY = centerY + sin(angle).toFloat() * (hubRadius + 4f)
                val endX = centerX + cos(angle).toFloat() * (rimRadius - 6f)
                val endY = centerY + sin(angle).toFloat() * (rimRadius - 6f)
                
                // Spoke shadow
                drawLine(
                    color = Color(0xFF505050),
                    start = Offset(startX + 1f, startY + 1f),
                    end = Offset(endX + 1f, endY + 1f),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
                
                // Spoke highlight
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFE0E0E0), Color(0xFFA0A0A0)),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY)
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
            
            // 5. Center Hub (Purple branded center from the logo)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryViolet,
                        DeepViolet,
                        SecondaryPurple.copy(alpha = 0.8f)
                    ),
                    center = Offset(centerX, centerY),
                    radius = hubRadius
                ),
                radius = hubRadius,
                center = Offset(centerX, centerY)
            )
            
            // Hub highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = hubRadius * 0.4f,
                center = Offset(centerX - hubRadius * 0.2f, centerY - hubRadius * 0.2f)
            )
        }
        
        // Outer tyre ring outline
        drawCircle(
            color = Color(0xFF1A1A1A),
            radius = outerRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = strokeWidth)
        )
        
        // Rim outline
        drawCircle(
            color = Color(0xFF606060),
            radius = rimRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.5f)
        )
    }
}

/**
 * Mesh Gradient Background - Animated breathing gradient effect
 * Creates a premium, dynamic background
 */
@Composable
private fun MeshGradientBackground(
    meshOffset1: Float,
    meshOffset2: Float,
    breathingAlpha: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Base gradient layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGradient)
        )
        
        // Animated mesh blobs
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(60.dp)
        ) {
            val width = size.width
            val height = size.height
            
            // Top-left purple blob
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryViolet.copy(alpha = breathingAlpha * 0.4f),
                        SecondaryPurple.copy(alpha = breathingAlpha * 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(meshOffset1 * 2, meshOffset2 * 1.5f),
                    radius = width * 0.6f
                ),
                radius = width * 0.5f,
                center = Offset(meshOffset1 * 2, meshOffset2 * 1.5f)
            )
            
            // Bottom-right lavender blob
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LightLavender.copy(alpha = breathingAlpha * 0.6f),
                        SecondaryPurple.copy(alpha = breathingAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(width - meshOffset2 * 3, height - meshOffset1 * 2),
                    radius = width * 0.5f
                ),
                radius = width * 0.4f,
                center = Offset(width - meshOffset2 * 3, height - meshOffset1 * 2)
            )
            
            // Center subtle glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = breathingAlpha * 0.3f),
                        LightLavender.copy(alpha = breathingAlpha * 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(width / 2 + meshOffset1, height / 3 + meshOffset2),
                    radius = width * 0.4f
                ),
                radius = width * 0.35f,
                center = Offset(width / 2 + meshOffset1, height / 3 + meshOffset2)
            )
        }
    }
}
