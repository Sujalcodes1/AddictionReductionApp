package com.example.addictionreductionapp.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.addictionreductionapp.R
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.addictionreductionapp.ui.theme.RegainTeal
import com.example.addictionreductionapp.ui.theme.RegainTeal
import com.example.addictionreductionapp.ui.theme.RegainTeal
import com.example.addictionreductionapp.ui.theme.DarkBackground
import com.example.addictionreductionapp.ui.theme.DarkCard
import com.example.addictionreductionapp.ui.theme.DarkCardLight
import com.example.addictionreductionapp.ui.theme.RegainTeal
import com.example.addictionreductionapp.ui.theme.TextGray
import com.example.addictionreductionapp.ui.theme.DarkCardLight
import com.example.addictionreductionapp.ui.theme.RegainTeal
import com.example.addictionreductionapp.ui.theme.DarkCard
import com.example.addictionreductionapp.ui.theme.DarkCardLight
import com.example.addictionreductionapp.ui.theme.DarkCard
import com.example.addictionreductionapp.ui.theme.DarkCardLight
import com.example.addictionreductionapp.ui.theme.TextGray
import com.example.addictionreductionapp.ui.theme.TextGray
import com.example.addictionreductionapp.ui.theme.DarkBackground
import com.example.addictionreductionapp.ui.theme.DarkBackground
import com.example.addictionreductionapp.ui.theme.TextGray
import com.example.addictionreductionapp.ui.theme.ValidationError
import com.example.addictionreductionapp.ui.theme.glassGlow
import com.example.addictionreductionapp.utils.AuthResult
import com.example.addictionreductionapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// ── Email format validator ─────────────────────────────────────────────────────
private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

// ──────────────────────────────────────────────────────────────────────
//  LOGIN SCREEN — Deep-Space Glassmorphism
// ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Per-field validation error messages (null = no error shown yet)
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val isLoading = authState is com.example.addictionreductionapp.utils.AuthState.Loading
    val snackbarHostState = remember { SnackbarHostState() }

    // Focus tracking for glow effect
    val emailInteractionSource = remember { MutableInteractionSource() }
    val passwordInteractionSource = remember { MutableInteractionSource() }
    val isEmailFocused by emailInteractionSource.collectIsFocusedAsState()
    val isPasswordFocused by passwordInteractionSource.collectIsFocusedAsState()

    val loginRadial = remember {
        Brush.radialGradient(
            colors = listOf(DarkBackground, Color.Transparent),
            center = Offset(0.5f * 1000f, 0.45f * 1000f),
            radius = 800f
        )
    }
    val loginLogoBrush = remember { Brush.verticalGradient(listOf(RegainTeal, RegainTeal)) }
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Layer 1: Outer background #010d0f + radial cyan glow at center (0.5, 0.45) — remembered ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .background(loginRadial)
        )

        // ── Layer 2: Glassmorphism Card — spec 28dp, #0d2a30 85%, stroke #1a4a52, padding 32dp ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(DarkCard)
                    .border(1.dp, DarkCardLight, RoundedCornerShape(28.dp))
                    .padding(horizontal = 32.dp, vertical = 32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── App Badge — 72x72dp, #0a1f25 16dp, LOGO PRESERVED (do not replace with star) ──
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .glassGlow(
                                color = RegainTeal,
                                borderRadius = 16.dp,
                                glowRadius = 14.dp,
                                glowAlpha = 0.5f
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(loginLogoBrush)
                            .border(1.dp, RegainTeal.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Shield logo — explicitly preserved per requirement
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // ── Two-Tone Heading — "Welcome" white + "Back" cyan #00d4aa, 28sp bold, centered ──
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            ) {
                                append("Welcome ")
                            }
                            withStyle(
                                SpanStyle(
                                    color = RegainTeal, // spec #00d4aa solid (not gradient) per plan
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            ) {
                                append("Back")
                            }
                        },
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Subtitle — spec #7a9aaa 14sp 1.4x centered ──
                    Text(
                        "Continue your journey into\nthe deep space of productivity.",
                        color = TextGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // ── Email Input — spec 56dp, #0f2d35, 14dp, border #1e4a54 → #00d4aa, icon cyan 22dp ──
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailError != null) emailError = null
                        },
                        placeholder = {
                            Text(
                                "Work Email",
                                color = TextGray,
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Email",
                                tint = if (emailError != null) ValidationError else RegainTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        isError = emailError != null,
                        interactionSource = emailInteractionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (emailError != null) ValidationError else RegainTeal,
                            unfocusedBorderColor = if (emailError != null) ValidationError.copy(alpha = 0.6f) else DarkCardLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard,
                            cursorColor = RegainTeal,
                            focusedLeadingIconColor = if (emailError != null) ValidationError else RegainTeal,
                            unfocusedLeadingIconColor = if (emailError != null) ValidationError else RegainTeal,
                            errorBorderColor = ValidationError,
                            errorLeadingIconColor = ValidationError,
                            errorTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .then(
                                if (isEmailFocused && emailError == null) {
                                    Modifier.glassGlow(
                                        color = RegainTeal,
                                        borderRadius = 14.dp,
                                        glowRadius = 8.dp,
                                        glowAlpha = 0.4f
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    // Inline email error
                    if (emailError != null) {
                        Text(
                            text = emailError!!,
                            color = ValidationError,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Password Input — same bg as email, lock cyan + eye toggle #7a9aaa, 56dp ──
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (passwordError != null) passwordError = null
                        },
                        placeholder = {
                            Text(
                                "Password",
                                color = TextGray,
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = if (passwordError != null) ValidationError else RegainTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = image,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color(0xFF7A9AAA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        isError = passwordError != null,
                        interactionSource = passwordInteractionSource,
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (passwordError != null) ValidationError else RegainTeal,
                            unfocusedBorderColor = if (passwordError != null) ValidationError.copy(alpha = 0.6f) else DarkCardLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard,
                            cursorColor = RegainTeal,
                            focusedLeadingIconColor = if (passwordError != null) ValidationError else RegainTeal,
                            unfocusedLeadingIconColor = if (passwordError != null) ValidationError else RegainTeal,
                            focusedTrailingIconColor = RegainTeal,
                            unfocusedTrailingIconColor = Color(0xFF7A9AAA),
                            errorBorderColor = ValidationError,
                            errorLeadingIconColor = ValidationError,
                            errorTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .then(
                                if (isPasswordFocused && passwordError == null) {
                                    Modifier.glassGlow(
                                        color = RegainTeal,
                                        borderRadius = 14.dp,
                                        glowRadius = 8.dp,
                                        glowAlpha = 0.4f
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    // Inline password error
                    if (passwordError != null) {
                        Text(
                            text = passwordError!!,
                            color = ValidationError,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // ── CTA — spec pill 56dp, gradient #00a888→#00d4aa, 28dp radius, 8dp shadow cyan 40% ──
                    Button(
                        onClick = {
                            // ── Step 1: Local validation — NEVER call Supabase if invalid ──
                            var valid = true

                            if (email.isBlank()) {
                                emailError = "Email is required"
                                valid = false
                            } else if (!isValidEmail(email)) {
                                emailError = "Enter a valid email address"
                                valid = false
                            }

                            if (password.isBlank()) {
                                passwordError = "Password is required"
                                valid = false
                            } else if (password.length < 6) {
                                passwordError = "Password must contain at least 6 characters"
                                valid = false
                            }

                            if (!valid) return@Button

                            // ── Step 2: Call real Supabase auth ──────────────────────────
                            authViewModel.login(email, password) { result ->
                                when (result) {
                                    is AuthResult.Success -> {
                                        onLoginSuccess()
                                    }
                                    is AuthResult.EmailConfirmationRequired -> {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Please verify your email address before logging in."
                                            )
                                        }
                                    }
                                    is AuthResult.Failure -> {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(28.dp),
                                clip = false,
                                ambientColor = RegainTeal.copy(alpha = 0.25f),
                                spotColor = RegainTeal.copy(alpha = 0.25f)
                            ),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        val ctaBrush = remember(isLoading) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    if (isLoading) RegainTeal.copy(alpha = 0.6f) else RegainTeal,
                                    if (isLoading) RegainTeal.copy(alpha = 0.6f) else RegainTeal
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ctaBrush, RoundedCornerShape(28.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Signing in...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        letterSpacing = 0.3.sp
                                    )
                                }
                            } else {
                                Text(
                                    "Sign In to Focus Shield →",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Star Divider — spec: 1dp #1a4a52 lines + cyan ✦ 16sp pad 12dp ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = DarkCardLight
                        )
                        Text(
                            "✦",
                            color = RegainTeal,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = DarkCardLight
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Footer — spec: #7a9aaa 14sp + #00d4aa bold underline clickable ──
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Don't have an account?  ",
                            color = TextGray,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = RegainTeal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("Create one")
                                }
                            },
                            maxLines = 1,
                            modifier = Modifier.clickable { onNavigateToRegister() }
                        )
                    }
                }
            }
        }

        // ── Snackbar ──────────────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ──────────────────────────────────────────────────────────────────────
//  REGISTER SCREEN — Unified Deep-Space Glassmorphism (Teal Theme)
// ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Per-field validation error messages
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var termsError by remember { mutableStateOf<String?>(null) }

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val isLoading = authState is com.example.addictionreductionapp.utils.AuthState.Loading
    val snackbarHostState = remember { SnackbarHostState() }

    // Focus tracking for glow effect
    val nameInteractionSource = remember { MutableInteractionSource() }
    val emailInteractionSource = remember { MutableInteractionSource() }
    val passwordInteractionSource = remember { MutableInteractionSource() }
    val isNameFocused by nameInteractionSource.collectIsFocusedAsState()
    val isEmailFocused by emailInteractionSource.collectIsFocusedAsState()
    val isPasswordFocused by passwordInteractionSource.collectIsFocusedAsState()

    val registerRadial = remember {
        Brush.radialGradient(
            colors = listOf(DarkBackground, Color.Transparent),
            center = Offset(0.5f * 1000f, 0.45f * 1000f),
            radius = 800f
        )
    }
    val registerLogoBrush = remember { Brush.verticalGradient(listOf(RegainTeal, RegainTeal)) }
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Same outer + radial glow as Login — ensures identical shade — remembered ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .background(registerRadial)
        )

        // ── Card — identical to Login: 28dp, #0d2a30 85%, stroke #1a4a52, padding 32dp ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 60.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(DarkCard)
                    .border(1.dp, DarkCardLight, RoundedCornerShape(28.dp))
                    .padding(horizontal = 32.dp, vertical = 32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo — identical to Login: 72dp 16dp, shield preserved
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .glassGlow(
                                color = RegainTeal,
                                borderRadius = 16.dp,
                                glowRadius = 14.dp,
                                glowAlpha = 0.5f
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(registerLogoBrush)
                            .border(1.dp, RegainTeal.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Heading — same two-tone style as Login, cyan solid #00d4aa
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            ) {
                                append("Join the ")
                            }
                            withStyle(
                                SpanStyle(
                                    color = RegainTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            ) {
                                append("Future")
                            }
                        },
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Supercharge your productivity with AI.",
                        color = TextGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // ── Name Field — identical shade to Login inputs: #0f2d35, 56dp, cyan 22dp ──
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (nameError != null) nameError = null
                        },
                        placeholder = {
                            Text("Full Name", color = TextGray, fontSize = 15.sp)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Name",
                                tint = if (nameError != null) ValidationError else RegainTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        isError = nameError != null,
                        interactionSource = nameInteractionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (nameError != null) ValidationError else RegainTeal,
                            unfocusedBorderColor = if (nameError != null) ValidationError.copy(alpha = 0.6f) else DarkCardLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard,
                            cursorColor = RegainTeal,
                            focusedLeadingIconColor = if (nameError != null) ValidationError else RegainTeal,
                            unfocusedLeadingIconColor = if (nameError != null) ValidationError else RegainTeal,
                            errorBorderColor = ValidationError,
                            errorLeadingIconColor = ValidationError,
                            errorTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .then(
                                if (isNameFocused && nameError == null) {
                                    Modifier.glassGlow(
                                        color = RegainTeal,
                                        borderRadius = 14.dp,
                                        glowRadius = 8.dp,
                                        glowAlpha = 0.4f
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (nameError != null) {
                        Text(
                            text = nameError!!,
                            color = ValidationError,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Email Field — identical to Login email ──
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailError != null) emailError = null
                        },
                        placeholder = {
                            Text("Email Address", color = TextGray, fontSize = 15.sp)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Email",
                                tint = if (emailError != null) ValidationError else RegainTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        isError = emailError != null,
                        interactionSource = emailInteractionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (emailError != null) ValidationError else RegainTeal,
                            unfocusedBorderColor = if (emailError != null) ValidationError.copy(alpha = 0.6f) else DarkCardLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard,
                            cursorColor = RegainTeal,
                            focusedLeadingIconColor = if (emailError != null) ValidationError else RegainTeal,
                            unfocusedLeadingIconColor = if (emailError != null) ValidationError else RegainTeal,
                            errorBorderColor = ValidationError,
                            errorLeadingIconColor = ValidationError,
                            errorTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .then(
                                if (isEmailFocused && emailError == null) {
                                    Modifier.glassGlow(
                                        color = RegainTeal,
                                        borderRadius = 14.dp,
                                        glowRadius = 8.dp,
                                        glowAlpha = 0.4f
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    if (emailError != null) {
                        Text(
                            text = emailError!!,
                            color = ValidationError,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Password Field — identical to Login password, eye #7A9AAA ──
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (passwordError != null) passwordError = null
                        },
                        placeholder = {
                            Text("Password", color = TextGray, fontSize = 15.sp)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = if (passwordError != null) ValidationError else RegainTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = image,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color(0xFF7A9AAA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        isError = passwordError != null,
                        interactionSource = passwordInteractionSource,
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (passwordError != null) ValidationError else RegainTeal,
                            unfocusedBorderColor = if (passwordError != null) ValidationError.copy(alpha = 0.6f) else DarkCardLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard,
                            cursorColor = RegainTeal,
                            focusedLeadingIconColor = if (passwordError != null) ValidationError else RegainTeal,
                            unfocusedLeadingIconColor = if (passwordError != null) ValidationError else RegainTeal,
                            focusedTrailingIconColor = RegainTeal,
                            unfocusedTrailingIconColor = Color(0xFF7A9AAA),
                            errorBorderColor = ValidationError,
                            errorLeadingIconColor = ValidationError,
                            errorTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .then(
                                if (isPasswordFocused && passwordError == null) {
                                    Modifier.glassGlow(
                                        color = RegainTeal,
                                        borderRadius = 14.dp,
                                        glowRadius = 8.dp,
                                        glowAlpha = 0.4f
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (passwordError != null) {
                        Text(
                            text = passwordError!!,
                            color = ValidationError,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    // ── Terms Checkbox ────────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = {
                                termsAccepted = it
                                if (it && termsError != null) termsError = null
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = RegainTeal,
                                uncheckedColor = if (termsError != null) ValidationError else TextGray
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = buildAnnotatedString {
                                append("I agree to the ")
                                withStyle(SpanStyle(color = RegainTeal, fontWeight = FontWeight.Bold)) {
                                    append("Terms of Service")
                                }
                            },
                            color = if (termsError != null) ValidationError else TextGray,
                            fontSize = 13.sp
                        )
                    }
                    if (termsError != null) {
                        Text(
                            text = termsError!!,
                            color = ValidationError,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, bottom = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── CTA — identical to Login: 56dp 28dp pill, #00a888→#00d4aa ──
                    Button(
                        onClick = {
                            // ── Step 1: Local validation — NEVER call Supabase if invalid ──
                            var valid = true

                            if (name.isBlank()) {
                                nameError = "Name is required"
                                valid = false
                            }

                            if (email.isBlank()) {
                                emailError = "Email is required"
                                valid = false
                            } else if (!isValidEmail(email)) {
                                emailError = "Enter a valid email address"
                                valid = false
                            }

                            if (password.isBlank()) {
                                passwordError = "Password is required"
                                valid = false
                            } else if (password.length < 6) {
                                passwordError = "Password must contain at least 6 characters"
                                valid = false
                            }

                            if (!termsAccepted) {
                                termsError = "You must accept the Terms of Service"
                                valid = false
                            }

                            if (!valid) return@Button

                            // ── Step 2: Call real Supabase auth ──────────────────────────
                            authViewModel.register(name, email, password) { result ->
                                when (result) {
                                    is AuthResult.Success -> {
                                        onRegisterSuccess()
                                    }
                                    is AuthResult.EmailConfirmationRequired -> {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Verification email sent. Please check your inbox before logging in."
                                            )
                                            onNavigateToLogin()
                                        }
                                    }
                                    is AuthResult.Failure -> {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(28.dp),
                                clip = false,
                                ambientColor = RegainTeal.copy(alpha = 0.25f),
                                spotColor = RegainTeal.copy(alpha = 0.25f)
                            ),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        val ctaBrush2 = remember(isLoading) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    if (isLoading) RegainTeal.copy(alpha = 0.6f) else RegainTeal,
                                    if (isLoading) RegainTeal.copy(alpha = 0.6f) else RegainTeal
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ctaBrush2, RoundedCornerShape(28.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Creating Account...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        letterSpacing = 0.3.sp
                                    )
                                }
                            } else {
                                Text(
                                    "Join Now →",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Star Divider — identical to Login
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = DarkCardLight
                        )
                        Text(
                            "✦",
                            color = RegainTeal,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = DarkCardLight
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Footer — identical shade to Login
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Already have an account?  ",
                            color = TextGray,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = RegainTeal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("Login")
                                }
                            },
                            maxLines = 1,
                            modifier = Modifier.clickable { onNavigateToLogin() }
                        )
                    }
                }
            }
        }

        // ── Snackbar ──────────────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun SparkleIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, 0f)
            quadraticTo(cx, cy, w, cy)
            quadraticTo(cx, cy, cx, h)
            quadraticTo(cx, cy, 0f, cy)
            quadraticTo(cx, cy, cx, 0f)
            close()
        }
        drawPath(path, color)
    }
}
