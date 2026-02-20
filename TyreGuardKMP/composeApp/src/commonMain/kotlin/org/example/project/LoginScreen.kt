package org.example.project

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

// ============ DARK AUTOMOTIVE THEME ============
private val PrimaryViolet = Color(0xFF7C3AED)
private val SecondaryPurple = Color(0xFFBB86FC)
private val DeepViolet = Color(0xFF5B21B6)
private val DarkViolet = Color(0xFF4C1D95)
private val LightLavender = Color(0xFFF3E5F5)
private val SoftWhite = Color(0xFFFAFAFA)

// Dark background layers
private val DarkBg        = Color(0xFF0D0D1A)
private val DarkSurface   = Color(0xFF161625)
private val DarkCard      = Color(0xFF1E1E30)
private val DarkField     = Color(0xFF252538)
private val FieldBorder   = Color(0xFF2E2E45)
private val TextOnDark    = Color(0xFFECECFF)
private val SubTextOnDark = Color(0xFF8888AA)

// Brand gradient (stays purple)
private val BrandGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))
)

// Legacy compat only
private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(DarkBg, DarkSurface)
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
    onLoginSuccess: () -> Unit,
    onForgotPassword: () -> Unit = {}
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
        modifier = modifier.fillMaxSize().background(DarkBg)
    ) {
        // Dark animated background blobs
        DarkMeshBackground(breathingAlpha = breathingAlpha)
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Logo & Brand ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(56.dp))
                AnimatedVisibility(
                    visible = startAnimation,
                    enter = slideInVertically(initialOffsetY = { -40 }, animationSpec = tween(800)) + fadeIn(tween(800))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RollingTyreAnimation(modifier = Modifier.size(88.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "TyreGuard",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextOnDark,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (authMode == AuthMode.SignIn) "Welcome back" else "Start your journey",
                            fontSize = 15.sp,
                            color = SubTextOnDark
                        )
                    }
                }
            }

            // ── Auth Form ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 32.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dark pill toggle
                DarkAuthToggle(
                    currentMode = authMode,
                    onModeChange = {
                        authMode = it
                        generalError = null
                        emailError = null
                        passwordError = null
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Error banner
                AnimatedVisibility(
                    visible = generalError != null || loginState is EnhancedLoginState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val errorMsg = generalError ?: (loginState as? EnhancedLoginState.Error)?.message ?: ""
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A1215)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(errorMsg, color = Color(0xFFFF6B6B), fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Loading
                if (loginState is EnhancedLoginState.Loading) {
                    CircularProgressIndicator(color = PrimaryViolet, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (authMode == AuthMode.SignIn) "Signing in..." else "Creating account...",
                        color = SubTextOnDark, fontSize = 14.sp
                    )
                } else {
                    // Display Name (Sign Up only)
                    AnimatedVisibility(
                        visible = authMode == AuthMode.SignUp,
                        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                    ) {
                        Column {
                            Text("Full Name", color = SubTextOnDark, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                            DarkTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                placeholder = "Full Name",
                                leadingIcon = Icons.Filled.Person,
                                imeAction = ImeAction.Next
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    // Email
                    Text("Email", color = SubTextOnDark, fontSize = 13.sp, modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp))
                    DarkTextField(
                        value = email,
                        onValueChange = { email = it; emailError = null },
                        placeholder = "Email address",
                        leadingIcon = Icons.Filled.Email,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        isError = emailError != null,
                        errorText = emailError
                    )

                    Spacer(Modifier.height(16.dp))

                    // Password
                    Text("Password", color = SubTextOnDark, fontSize = 13.sp, modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp))
                    DarkTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = null },
                        placeholder = "Enter Password",
                        leadingIcon = Icons.Filled.Lock,
                        keyboardType = KeyboardType.Password,
                        imeAction = if (authMode == AuthMode.SignUp) ImeAction.Next else ImeAction.Done,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        isError = passwordError != null,
                        errorText = passwordError
                    )

                    // Confirm password (Sign Up only)
                    AnimatedVisibility(
                        visible = authMode == AuthMode.SignUp,
                        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                    ) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Text("Confirm Password", color = SubTextOnDark, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                            DarkTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                placeholder = "Confirm Password",
                                leadingIcon = Icons.Filled.Lock,
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                                isPassword = true,
                                passwordVisible = passwordVisible,
                                onTogglePassword = { passwordVisible = !passwordVisible }
                            )
                        }
                    }

                    // Forgot Password (Sign In only)
                    AnimatedVisibility(
                        visible = authMode == AuthMode.SignIn,
                        enter = fadeIn(tween(300)),
                        exit = fadeOut(tween(200))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(onClick = onForgotPassword) {
                                Text("Forgot Password?", color = PrimaryViolet, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Main CTA button (gradient)
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
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(BrandGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (authMode == AuthMode.SignIn) "Log In" else "Register",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // OR divider
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = FieldBorder)
                        Text("  OR  ", color = SubTextOnDark, fontSize = 13.sp)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = FieldBorder)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Google Sign In Button (dark outlined)
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                loginState = EnhancedLoginState.Loading
                                val result = SupabaseAuthService.signInWithGoogle()
                                when (result) {
                                    is AuthResult.Success -> loginState = EnhancedLoginState.BiometricSetup
                                    else -> loginState = EnhancedLoginState.Error("Google sign in failed")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, FieldBorder),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkCard)
                    ) {
                        Text(
                            text = "G",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text("Continue with Google", color = TextOnDark, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign up / Sign in switch link
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = if (authMode == AuthMode.SignIn) "Haven't any account? " else "Already have an account? ",
                            color = SubTextOnDark, fontSize = 14.sp
                        )
                        Text(
                            text = if (authMode == AuthMode.SignIn) "Sign up" else "Sign in",
                            color = PrimaryViolet, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                authMode = if (authMode == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn
                                generalError = null; emailError = null; passwordError = null
                            }
                        )
                    }
                }
            }

            // Quick login indicator
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hasQuickLoginEnabled) {
                    Icon(Icons.Filled.Fingerprint, null, tint = PrimaryViolet, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Quick Login enabled", color = PrimaryViolet, fontSize = 12.sp)
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
                sheetState = bottomSheetState,
                containerColor = DarkCard,
                dragHandle = { Box(Modifier.padding(vertical = 12.dp)) {
                    Box(Modifier.width(40.dp).height(4.dp).background(FieldBorder, RoundedCornerShape(50)).align(Alignment.Center))
                }}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Fingerprint, null, tint = PrimaryViolet, modifier = Modifier.size(56.dp))
                    
                    Text(
                        text = "Enable Quick Login?",
                        color = TextOnDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Use biometrics to log in instantly next time. Fast, secure, and convenient.",
                        color = SubTextOnDark,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
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
 * Dark Pill-Style Auth Toggle — Drivio-inspired
 */
@Composable
private fun DarkAuthToggle(
    currentMode: AuthMode,
    onModeChange: (AuthMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(50))
            .border(1.dp, FieldBorder, RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        Row {
            DarkTabButton(
                text = "Sign In",
                isSelected = currentMode == AuthMode.SignIn,
                onClick = { onModeChange(AuthMode.SignIn) },
                modifier = Modifier.weight(1f)
            )
            DarkTabButton(
                text = "Sign Up",
                isSelected = currentMode == AuthMode.SignUp,
                onClick = { onModeChange(AuthMode.SignUp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DarkTabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor by animateColorAsState(if (isSelected) PrimaryViolet else Color.Transparent, tween(250), label = "tab")
    val textColor by animateColorAsState(if (isSelected) Color.White else SubTextOnDark, tween(250), label = "tabTxt")
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor, contentColor = textColor),
        elevation = null,
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 15.sp)
    }
}

/**
 * Dark Filled Text Field — Drivio-style
 */
@Composable
private fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = SubTextOnDark.copy(alpha = 0.6f)) },
            leadingIcon = {
                Icon(leadingIcon, contentDescription = null, tint = if (isError) Color(0xFFFF6B6B) else SubTextOnDark, modifier = Modifier.size(20.dp))
            },
            trailingIcon = if (isPassword && onTogglePassword != null) {{
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = SubTextOnDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }} else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            isError = isError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = DarkField,
                focusedContainerColor = DarkField,
                errorContainerColor = Color(0xFF2A1010),
                unfocusedBorderColor = FieldBorder,
                focusedBorderColor = PrimaryViolet,
                errorBorderColor = Color(0xFFFF6B6B),
                unfocusedTextColor = TextOnDark,
                focusedTextColor = TextOnDark,
                errorTextColor = TextOnDark,
                cursorColor = PrimaryViolet
            )
        )
        if (isError && errorText != null) {
            Text(errorText, color = Color(0xFFFF6B6B), fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
    }
}

/**
 * Legacy compat (kept so existing calls don't break)
 */
@Composable
private fun PillAuthModeToggle(currentMode: AuthMode, onModeChange: (AuthMode) -> Unit) {
    DarkAuthToggle(currentMode, onModeChange)
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
 * Dark Mesh Background — deep space glow effect
 */
@Composable
private fun DarkMeshBackground(breathingAlpha: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize().blur(80.dp)) {
        val w = size.width
        val h = size.height
        // Top-left purple glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PrimaryViolet.copy(alpha = breathingAlpha * 0.35f), Color.Transparent),
                center = Offset(w * 0.15f, h * 0.15f), radius = w * 0.55f
            ),
            radius = w * 0.5f, center = Offset(w * 0.15f, h * 0.15f)
        )
        // Bottom-right deep violet glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(DeepViolet.copy(alpha = breathingAlpha * 0.4f), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.85f), radius = w * 0.5f
            ),
            radius = w * 0.45f, center = Offset(w * 0.85f, h * 0.85f)
        )
    }
}

/**
 * Legacy alias kept so old callers don't break
 */
@Composable
private fun MeshGradientBackground(meshOffset1: Float, meshOffset2: Float, breathingAlpha: Float, modifier: Modifier = Modifier) {
    DarkMeshBackground(breathingAlpha, modifier)
}
