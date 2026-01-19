package org.example.project

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.auth.AuthResult
import org.example.project.auth.BiometricAuthManager
import org.example.project.auth.BiometricAuthResult
import org.example.project.auth.BiometricCapability
import org.example.project.auth.GoogleAuthManager
import org.example.project.auth.GoogleAuthResult
import org.example.project.auth.SupabaseAuthService
import org.example.project.data.UserDataRepository
import kotlin.math.cos
import kotlin.math.sin

// Brand Colors
private val TyrePurple = Color(0xFF6A4FA3)
private val TyrePurpleLight = Color(0xFFE8E0F0)
private val TyrePurpleDark = Color(0xFF4A3573)
private val TyrePurpleContainer = Color(0xFFF3EEFA)

/**
 * Biometric authentication state
 */
sealed class BiometricState {
    object Idle : BiometricState()
    object Checking : BiometricState()
    object Authenticating : BiometricState()
    object Success : BiometricState()
    data class Failed(val message: String) : BiometricState()
    object NotAvailable : BiometricState()
    object NotEnrolled : BiometricState()
}

/**
 * Android-specific Login Screen with Biometric + Google Credential Manager integration
 * 
 * Authentication Flow:
 * 1. On launch, check if biometric is enabled for user
 * 2. If enabled, show biometric prompt immediately
 * 3. On biometric success, auto sign-in
 * 4. If biometric fails/cancelled, show normal login form
 * 5. After successful login, offer to enable biometric for future
 * 
 * Supports unified biometric credentials (face, fingerprint, iris) as per Android 2026 standards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidLoginScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit,
    onSkipToSetup: () -> Unit = onLoginSuccess,
    onForgotPassword: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    
    var loginState by remember { mutableStateOf<EnhancedLoginState>(EnhancedLoginState.Idle) }
    var biometricState by remember { mutableStateOf<BiometricState>(BiometricState.Idle) }
    var authMode by remember { mutableStateOf(AuthMode.SignIn) }
    var hasQuickLoginEnabled by remember { mutableStateOf(false) }
    var autoSignInAttempted by remember { mutableStateOf(false) }
    var biometricAttempted by remember { mutableStateOf(false) }
    
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
    
    // Create managers
    val googleAuthManager = remember { GoogleAuthManager(context) }
    val biometricAuthManager = remember { BiometricAuthManager(context) }
    
    // Check biometric capability on launch
    val biometricCapability by biometricAuthManager.biometricState.collectAsState()
    
    // Biometric authentication function
    fun attemptBiometricAuth() {
        if (activity == null) {
            biometricState = BiometricState.NotAvailable
            return
        }
        
        biometricState = BiometricState.Authenticating
        
        biometricAuthManager.authenticate(
            activity = activity,
            title = "Welcome Back",
            subtitle = "Sign in to TyreGuard",
            description = "Use your biometric credential for quick access"
        ) { result ->
            when (result) {
                is BiometricAuthResult.Success -> {
                    biometricState = BiometricState.Success
                    hasQuickLoginEnabled = true
                    loginState = EnhancedLoginState.Success
                    
                    // Restore user session from stored data
                    val storedUserId = biometricAuthManager.getStoredUserId()
                    if (storedUserId != null) {
                        // User is authenticated via biometric
                        coroutineScope.launch {
                            delay(300) // Brief delay for visual feedback
                            onLoginSuccess()
                        }
                    }
                }
                is BiometricAuthResult.Cancelled -> {
                    biometricState = BiometricState.Idle
                    // User cancelled, show normal login form
                }
                is BiometricAuthResult.NotAvailable -> {
                    biometricState = BiometricState.NotAvailable
                }
                is BiometricAuthResult.NotEnrolled -> {
                    biometricState = BiometricState.NotEnrolled
                }
                is BiometricAuthResult.Failed -> {
                    biometricState = BiometricState.Failed("Biometric not recognized. Please try again.")
                }
                is BiometricAuthResult.Error -> {
                    biometricState = BiometricState.Failed(result.message)
                }
                else -> {
                    biometricState = BiometricState.Idle
                }
            }
        }
    }
    
    // Attempt biometric authentication on first launch if enabled
    LaunchedEffect(Unit) {
        if (!biometricAttempted) {
            biometricAttempted = true
            
            // Check if biometric sign-in is available
            if (biometricAuthManager.canAttemptBiometricSignIn()) {
                hasQuickLoginEnabled = true
                delay(300) // Brief delay to let UI render
                attemptBiometricAuth()
            } else if (!autoSignInAttempted && activity != null) {
                // Fall back to Google auto sign-in
                autoSignInAttempted = true
                loginState = EnhancedLoginState.Loading
                
                val result = googleAuthManager.attemptAutoSignIn(activity as Activity)
                when (result) {
                    is GoogleAuthResult.Success -> {
                        loginState = EnhancedLoginState.Success
                        hasQuickLoginEnabled = true
                        delay(500)
                        onLoginSuccess()
                    }
                    else -> {
                        loginState = EnhancedLoginState.Idle
                    }
                }
            }
        }
    }
    
    // Animated background gradient with TyrePurple
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animatedColor by infiniteTransition.animateColor(
        initialValue = TyrePurple.copy(alpha = 0.08f),
        targetValue = TyrePurpleLight.copy(alpha = 0.15f),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientColor"
    )
    
    // Enhanced animations for background elements
    val floatAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val secondaryRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "secondaryRotation"
    )
    
    // Handle biometric sheet
    val showBiometricSheet = loginState is EnhancedLoginState.BiometricSetup
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Animated background with gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            animatedColor,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        
        // Animated floating tyre elements in background
        AndroidAnimatedTyreBackground(
            rotation = floatAnimation,
            secondaryRotation = secondaryRotation,
            pulseScale = pulseAnimation
        )
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top section - Logo and welcome text
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // Animated 3D-style App Logo
                AndroidAnimatedLogoTyre(
                    rotation = floatAnimation,
                    modifier = Modifier.size(100.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "TyreGuard",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TyrePurple
                )
                
                Text(
                    text = "Your personal pit crew",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray.copy(alpha = 0.8f)
                )
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
                // Auth Mode Toggle Tabs
                AuthModeToggleAndroid(
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
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (authMode == AuthMode.SignIn) "Signing in..." else "Creating account...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // Display name field (Sign Up only)
                    AnimatedVisibility(
                        visible = authMode == AuthMode.SignUp,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name") },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            emailError = null
                        },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null)
                        },
                        isError = emailError != null,
                        supportingText = emailError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            passwordError = null
                        },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null)
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
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Confirm password field (Sign Up only)
                    AnimatedVisibility(
                        visible = authMode == AuthMode.SignUp,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Lock, contentDescription = null)
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Main action button (Email/Password sign in)
                    Button(
                        onClick = {
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
                                            // Initialize user data repository
                                            UserDataRepository.initialize(
                                                userId = result.user.id,
                                                email = result.user.email,
                                                displayName = result.user.displayName ?: "",
                                                authProvider = "email"
                                            )
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
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TyrePurple,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text(
                            text = if (authMode == AuthMode.SignIn) "Sign In" else "Create Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Forgot Password link (Sign In only)
                    AnimatedVisibility(
                        visible = authMode == AuthMode.SignIn,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(onClick = onForgotPassword) {
                                Text(
                                    text = "Forgot Password?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TyrePurple,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Divider with "OR"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  OR  ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Google Sign-In Button (uses Credential Manager)
                    OutlinedButton(
                        onClick = {
                            // Find the activity context
                            val activityContext = context as? Activity 
                                ?: (context as? android.content.ContextWrapper)?.baseContext as? Activity
                            
                            if (activityContext != null) {
                                coroutineScope.launch {
                                    loginState = EnhancedLoginState.Loading
                                    generalError = null
                                    
                                    val result = googleAuthManager.signIn(activityContext)
                                    when (result) {
                                        is GoogleAuthResult.Success -> {
                                            loginState = EnhancedLoginState.BiometricSetup
                                        }
                                        is GoogleAuthResult.Cancelled -> {
                                            loginState = EnhancedLoginState.Idle
                                        }
                                        is GoogleAuthResult.Error -> {
                                            loginState = EnhancedLoginState.Error(result.message)
                                            generalError = result.message
                                        }
                                        is GoogleAuthResult.NoSavedCredentials -> {
                                            loginState = EnhancedLoginState.Idle
                                            generalError = "No Google accounts found. Please sign in with email."
                                        }
                                    }
                                }
                            } else {
                                generalError = "Unable to launch Google Sign-In. Please try again."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.DarkGray
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color.LightGray
                        )
                    ) {
                        // Google "G" logo with brand colors
                        GoogleGLogo(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Bottom section - biometric indicator and quick access
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Show biometric button if available
                if (hasQuickLoginEnabled && biometricCapability == BiometricCapability.AVAILABLE) {
                    FilledTonalButton(
                        onClick = { attemptBiometricAuth() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = TyrePurpleContainer,
                            contentColor = TyrePurple
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Biometric", fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    text = if (hasQuickLoginEnabled) "🔒" else "🔓",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (hasQuickLoginEnabled)
                        TyrePurple
                    else
                        Color.Gray.copy(alpha = 0.4f)
                )
                if (hasQuickLoginEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Quick Login enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = TyrePurple
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
                        text = "Choose your preferred biometric method for instant sign-in",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Biometric options cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Fingerprint option
                        BiometricOptionCard(
                            icon = Icons.Filled.Fingerprint,
                            title = "Fingerprint",
                            description = "Use your fingerprint",
                            isAvailable = biometricCapability == BiometricCapability.AVAILABLE,
                            onClick = {
                                if (activity != null) {
                                    registerBiometric(
                                        activity = activity,
                                        biometricAuthManager = biometricAuthManager,
                                        onSuccess = {
                                            hasQuickLoginEnabled = true
                                            loginState = EnhancedLoginState.Success
                                            onLoginSuccess()
                                        },
                                        onSkip = {
                                            loginState = EnhancedLoginState.Success
                                            onLoginSuccess()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Face option
                        BiometricOptionCard(
                            icon = Icons.Filled.Face,
                            title = "Face",
                            description = "Use face unlock",
                            isAvailable = biometricCapability == BiometricCapability.AVAILABLE,
                            onClick = {
                                if (activity != null) {
                                    registerBiometric(
                                        activity = activity,
                                        biometricAuthManager = biometricAuthManager,
                                        onSuccess = {
                                            hasQuickLoginEnabled = true
                                            loginState = EnhancedLoginState.Success
                                            onLoginSuccess()
                                        },
                                        onSkip = {
                                            loginState = EnhancedLoginState.Success
                                            onLoginSuccess()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Info text
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = TyrePurpleContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = TyrePurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your device will use whichever biometric you have enrolled in settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = TyrePurpleDark
                            )
                        }
                    }
                    
                    // Show biometric capability info
                    when (biometricCapability) {
                        BiometricCapability.AVAILABLE -> {
                            Text(
                                text = "✅ Biometric authentication available",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF34A853)
                            )
                        }
                        BiometricCapability.NOT_ENROLLED -> {
                            Text(
                                text = "⚠️ No biometric enrolled. Set up in device settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFA000)
                            )
                        }
                        else -> {
                            Text(
                                text = "ℹ️ Biometric not available on this device",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Enable Both button
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        onClick = {
                            if (biometricCapability == BiometricCapability.AVAILABLE && activity != null) {
                                registerBiometric(
                                    activity = activity,
                                    biometricAuthManager = biometricAuthManager,
                                    onSuccess = {
                                        hasQuickLoginEnabled = true
                                        loginState = EnhancedLoginState.Success
                                        onLoginSuccess()
                                    },
                                    onSkip = {
                                        loginState = EnhancedLoginState.Success
                                        onLoginSuccess()
                                    }
                                )
                            } else {
                                loginState = EnhancedLoginState.Success
                                onLoginSuccess()
                            }
                        },
                        enabled = biometricCapability == BiometricCapability.AVAILABLE,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TyrePurple,
                            contentColor = Color.White,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enable All Available",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    TextButton(
                        onClick = {
                            loginState = EnhancedLoginState.Success
                            onLoginSuccess()
                        }
                    ) {
                        Text("Skip for now", color = TyrePurple)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        
        // Biometric authenticating overlay
        AnimatedVisibility(
            visible = biometricState is BiometricState.Authenticating,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Animated fingerprint icon
                    val pulseAnim = rememberInfiniteTransition(label = "bioPulse")
                    val scale by pulseAnim.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bioScale"
                    )
                    
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = "Authenticating",
                        modifier = Modifier
                            .size(80.dp)
                            .scale(scale),
                        tint = TyrePurple
                    )
                    
                    Text(
                        text = "Authenticating...",
                        style = MaterialTheme.typography.titleMedium,
                        color = TyrePurple
                    )
                    
                    Text(
                        text = "Use your biometric to sign in",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * Get description of available biometric type
 */
private fun getBiometricTypeDescription(): String {
    return "Face, Fingerprint or PIN available"
}

/**
 * Animated Pill-Shaped Auth Mode Toggle with smooth sliding indicator
 */
@Composable
private fun AuthModeToggleAndroid(
    currentMode: AuthMode,
    onModeChange: (AuthMode) -> Unit
) {
    val isSignIn = currentMode == AuthMode.SignIn
    
    // Animated offset for the sliding pill indicator
    val animatedOffset by animateFloatAsState(
        targetValue = if (isSignIn) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "toggleOffset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(TyrePurpleContainer)
            .padding(4.dp)
    ) {
        // Sliding pill indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .offset(x = (animatedOffset * (LocalContext.current.resources.displayMetrics.widthPixels / LocalContext.current.resources.displayMetrics.density - 56).dp.value * 0.5f).dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(TyrePurple, TyrePurpleDark)
                    )
                )
        )
        
        // Toggle buttons row
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sign In option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onModeChange(AuthMode.SignIn) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSignIn) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = animateColorAsState(
                        targetValue = if (isSignIn) Color.White else Color.Gray,
                        animationSpec = tween(300),
                        label = "signInTextColor"
                    ).value
                )
            }
            
            // Sign Up option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onModeChange(AuthMode.SignUp) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (!isSignIn) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = animateColorAsState(
                        targetValue = if (!isSignIn) Color.White else Color.Gray,
                        animationSpec = tween(300),
                        label = "signUpTextColor"
                    ).value
                )
            }
        }
    }
}

@Composable
private fun AuthModeTabAndroid(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) TyrePurple else Color.Transparent,
        onClick = onClick
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            color = if (isSelected) Color.White else Color.Gray,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Animated 3D-style tyre for the logo with rotation (Android version)
 * Uses TyrePurple brand color for the center hub
 */
@Composable
private fun AndroidAnimatedLogoTyre(
    rotation: Float,
    modifier: Modifier = Modifier
) {
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
            
            // Center hub with TyrePurple brand color
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TyrePurple,
                        TyrePurple.copy(alpha = 0.8f),
                        TyrePurpleDark
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
 * Animated floating tyre background elements (Android version)
 * Uses TyrePurple brand colors for visual consistency
 */
@Composable
private fun AndroidAnimatedTyreBackground(
    rotation: Float,
    secondaryRotation: Float,
    pulseScale: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = TyrePurple.copy(alpha = 0.1f)
    val secondaryColor = TyrePurpleLight.copy(alpha = 0.08f)
    
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
    }
}

/**
 * Google "G" Logo drawn with Canvas using brand colors
 */
@Composable
private fun GoogleGLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val size = size.minDimension
        val strokeWidth = size * 0.18f
        val radius = (size - strokeWidth) / 2
        val center = Offset(size / 2, size / 2)
        
        // Google brand colors
        val googleBlue = Color(0xFF4285F4)
        val googleGreen = Color(0xFF34A853)
        val googleYellow = Color(0xFFFBBC05)
        val googleRed = Color(0xFFEA4335)
        
        // Draw the colored arcs (the "G" shape)
        // Blue arc (right side)
        drawArc(
            color = googleBlue,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = androidx.compose.ui.geometry.Size(size - strokeWidth, size - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        
        // Green arc (bottom)
        drawArc(
            color = googleGreen,
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = androidx.compose.ui.geometry.Size(size - strokeWidth, size - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        
        // Yellow arc (left bottom)
        drawArc(
            color = googleYellow,
            startAngle = 135f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = androidx.compose.ui.geometry.Size(size - strokeWidth, size - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        
        // Red arc (top)
        drawArc(
            color = googleRed,
            startAngle = 195f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = androidx.compose.ui.geometry.Size(size - strokeWidth, size - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        
        // Draw the horizontal bar of the "G"
        drawLine(
            color = googleBlue,
            start = Offset(center.x, center.y),
            end = Offset(size - strokeWidth / 2, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt
        )
    }
}

/**
 * Biometric option card for selecting fingerprint or face registration
 */
@Composable
private fun BiometricOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(
                enabled = isAvailable,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) TyrePurpleContainer else Color.Gray.copy(alpha = 0.1f),
            contentColor = if (isAvailable) TyrePurpleDark else Color.Gray
        ),
        border = if (isAvailable) BorderStroke(1.dp, TyrePurple.copy(alpha = 0.3f)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(40.dp),
                tint = if (isAvailable) TyrePurple else Color.Gray
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = if (isAvailable) TyrePurpleDark.copy(alpha = 0.7f) else Color.Gray
            )
        }
    }
}

/**
 * Helper function to register biometric authentication
 */
private fun registerBiometric(
    activity: FragmentActivity,
    biometricAuthManager: BiometricAuthManager,
    onSuccess: () -> Unit,
    onSkip: () -> Unit
) {
    biometricAuthManager.authenticate(
        activity = activity,
        title = "Register Biometric",
        subtitle = "Verify your identity to enable quick login",
        description = "Use fingerprint or face to sign in instantly next time"
    ) { result ->
        when (result) {
            is BiometricAuthResult.Success -> {
                // Get user ID from current session
                val userId = SupabaseAuthService.currentUser.value?.id 
                    ?: UserDataRepository.userProfile.value?.name
                    ?: "user_${System.currentTimeMillis()}"
                
                biometricAuthManager.enableBiometric(userId)
                onSuccess()
            }
            else -> {
                // Biometric verification failed, but still allow login
                onSkip()
            }
        }
    }
}
