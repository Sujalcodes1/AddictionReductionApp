package com.example.addictionreductionapp.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.`data`.AppDataStore
import com.example.addictionreductionapp.ui.theme.DarkBackground
import com.example.addictionreductionapp.ui.theme.DarkCard
import com.example.addictionreductionapp.ui.theme.DarkSurface
import com.example.addictionreductionapp.ui.theme.RegainPurple
import com.example.addictionreductionapp.ui.theme.RegainTeal
import com.example.addictionreductionapp.ui.theme.TextGray
import com.example.addictionreductionapp.ui.theme.TextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

// ── Custom Glass Glow Shadow Modifier ──────────────────────────────
// Draws a beautiful, realistic glowing layout shadow outside the box
// bounds while keeping the area directly underneath the box completely
// clear and transparent, preventing the ugly black shadow over-draw bug.
fun Modifier.glassGlow(
    color: Color,
    borderRadius: Dp = 18.dp,
    glowRadius: Dp = 14.dp,
    glowAlpha: Float = 0.5f,
    offsetY: Dp = 0.dp
): Modifier = this.drawBehind {
    val density = this
    val glowColor = color.copy(alpha = glowAlpha)
    
    // Path matching the exact bounds and rounded corners of the box
    val innerPath = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                rect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(borderRadius.toPx())
            )
        )
    }
    
    // Clip out the inner box area using ClipOp.Difference so only the outer glow is drawn
    clipPath(innerPath, clipOp = ClipOp.Difference) {
        val paint = Paint().asFrameworkPaint().apply {
            this.color = glowColor.toArgb()
            setShadowLayer(
                glowRadius.toPx(),
                0f,
                offsetY.toPx(),
                glowColor.toArgb()
            )
        }
        
        drawIntoCanvas { canvas ->
            canvas.drawRoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                radiusX = borderRadius.toPx(),
                radiusY = borderRadius.toPx(),
                paint = androidx.compose.ui.graphics.Paint().apply {
                    asFrameworkPaint().set(paint)
                }
            )
        }
    }
}


// ──────────────────────────────────────────────────────────────────────
//  Deep-Space Glassmorphism Color Palette
// ──────────────────────────────────────────────────────────────────────
private val SpaceBgCenter   = Color(0xFF1A2F3A)
private val SpaceBgMid      = Color(0xFF0D1E28)
private val SpaceBgEdge     = Color(0xFF0A1520)
private val AmbientGlow     = Color(0x26148C8C)     // rgba(20,120,140,0.15)
private val GlassWhite004   = Color(0x0AFFFFFF)     // rgba(255,255,255,0.04)
private val GlassBorder008  = Color(0x14FFFFFF)     // rgba(255,255,255,0.08)
private val GlassBorder010  = Color(0x1AFFFFFF)     // rgba(255,255,255,0.10)
private val GlassInput005   = Color(0x0DFFFFFF)     // rgba(255,255,255,0.05)
private val GlassInputBorder = Color(0x1AFFFFFF)    // rgba(255,255,255,0.10)
private val PlaceholderWhite = Color(0x59FFFFFF)     // rgba(255,255,255,0.35)
private val IconWhite04     = Color(0x66FFFFFF)      // rgba(255,255,255,0.40)
private val DividerWhite015 = Color(0x26FFFFFF)      // rgba(255,255,255,0.15)
private val SubtitleWhite   = Color(0x80FFFFFF)      // rgba(255,255,255,0.50)
private val LogoBg007       = Color(0x12FFFFFF)      // rgba(255,255,255,0.07)
private val GradientTealA   = Color(0xFF14B8A6)
private val GradientTealB   = Color(0xFF06B6D4)
private val CtaGradientA    = Color(0xFF0D9488)
private val CtaGradientB    = Color(0xFF06B6D4)
private val FocusGlowTeal   = Color(0x2614B8A6)     // rgba(20,184,166,0.15)
private val FocusBorderTeal = Color(0x8014B8A6)      // rgba(20,184,166,0.50)

// ──────────────────────────────────────────────────────────────────────
//  LOGIN SCREEN — Deep-Space Glassmorphism
// ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Focus tracking for glow effect
    val emailInteractionSource = remember { MutableInteractionSource() }
    val passwordInteractionSource = remember { MutableInteractionSource() }
    val isEmailFocused by emailInteractionSource.collectIsFocusedAsState()
    val isPasswordFocused by passwordInteractionSource.collectIsFocusedAsState()

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Layer 1: Deep-Space Radial Gradient Background ──────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Main radial gradient background
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to SpaceBgCenter,
                        0.4f to SpaceBgMid,
                        1.0f to SpaceBgEdge
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.4f),
                    radius = size.maxDimension * 0.8f
                )
            )

            // Blue-teal ambient glow circle behind the card
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AmbientGlow,
                        Color.Transparent
                    ),
                    radius = size.width * 0.7f
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.5f, size.height * 0.42f)
            )

            // Subtle secondary upper glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x0D14B8A6),
                        Color.Transparent
                    ),
                    radius = size.width * 0.5f
                ),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.3f, size.height * 0.15f)
            )
        }

        // ── Layer 2: Glassmorphism Card ─────────────────────────────
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
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassWhite004)
                    .border(1.dp, GlassBorder008, RoundedCornerShape(24.dp))
                    .padding(horizontal = 28.dp, vertical = 36.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Logo Sparkle Icon ────────────────────────────
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .glassGlow(
                                color = GradientTealA,
                                borderRadius = 18.dp,
                                glowRadius = 14.dp,
                                glowAlpha = 0.5f
                            )
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x2BFFFFFF), Color(0x0FFFFFFF))
                                )
                            )
                            .border(1.dp, GlassBorder010, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        SparkleIcon(
                            color = GradientTealA,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Split Gradient Heading ───────────────────────
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
                                    brush = Brush.linearGradient(
                                        listOf(GradientTealA, GradientTealB)
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            ) {
                                append("Back")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── Subtitle ─────────────────────────────────────
                    Text(
                        "Continue your journey into the deep\nspace of productivity.",
                        color = SubtitleWhite,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // ── Email Input Field ─────────────────────────────
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = {
                            Text(
                                "Work Email",
                                color = PlaceholderWhite,
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Email",
                                tint = IconWhite04,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        interactionSource = emailInteractionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FocusBorderTeal,
                            unfocusedBorderColor = GlassInputBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = GlassInput005,
                            unfocusedContainerColor = GlassInput005,
                            cursorColor = GradientTealA,
                            focusedLeadingIconColor = GradientTealA,
                            unfocusedLeadingIconColor = IconWhite04
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isEmailFocused) {
                                    Modifier.glassGlow(
                                        color = GradientTealA,
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Password Input Field ─────────────────────────
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = {
                            Text(
                                "Password",
                                color = PlaceholderWhite,
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = IconWhite04,
                                modifier = Modifier.size(20.dp)
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
                                    tint = IconWhite04,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        interactionSource = passwordInteractionSource,
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FocusBorderTeal,
                            unfocusedBorderColor = GlassInputBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = GlassInput005,
                            unfocusedContainerColor = GlassInput005,
                            cursorColor = GradientTealA,
                            focusedLeadingIconColor = GradientTealA,
                            unfocusedLeadingIconColor = IconWhite04,
                            focusedTrailingIconColor = GradientTealA,
                            unfocusedTrailingIconColor = IconWhite04
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isPasswordFocused) {
                                    Modifier.glassGlow(
                                        color = GradientTealA,
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

                    Spacer(modifier = Modifier.height(32.dp))

                    // ── CTA Gradient Pill Button ─────────────────────
                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                isLoading = true
                                scope.launch {
                                    delay(900) // Simulate network call
                                    AppDataStore.isLoggedIn.value = true
                                    AppDataStore.userName.value =
                                        email.substringBefore("@")
                                            .replaceFirstChar { it.uppercase() }
                                    AppDataStore.saveToPrefs(context)
                                    isLoading = false
                                    onLoginSuccess()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(50.dp),
                                clip = false,
                                ambientColor = GradientTealA.copy(alpha = 0.5f),
                                spotColor = GradientTealB.copy(alpha = 0.5f)
                            ),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(CtaGradientA, CtaGradientB),
                                        start = Offset.Zero,
                                        end = Offset.Infinite
                                    ),
                                    RoundedCornerShape(50.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isLoading) "Signing in..." else "Sign In to Focus Shield →",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // ── Sparkle Divider ───────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 0.5.dp,
                            color = DividerWhite015
                        )
                        Spacer(Modifier.width(8.dp))
                        SparkleIcon(
                            color = DividerWhite015,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 0.5.dp,
                            color = DividerWhite015
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Footer Link (non-wrapping) ───────────────────
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Don't have an account?  ",
                            color = SubtitleWhite,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            "Create one here",
                            color = GradientTealA,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.clickable { onNavigateToRegister() }
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  REGISTER SCREEN — Unified Deep-Space Glassmorphism (Teal Theme)
// ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
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
    var isLoading by remember { mutableStateOf(false) }

    // Focus tracking for glow effect
    val nameInteractionSource = remember { MutableInteractionSource() }
    val emailInteractionSource = remember { MutableInteractionSource() }
    val passwordInteractionSource = remember { MutableInteractionSource() }
    val isNameFocused by nameInteractionSource.collectIsFocusedAsState()
    val isEmailFocused by emailInteractionSource.collectIsFocusedAsState()
    val isPasswordFocused by passwordInteractionSource.collectIsFocusedAsState()

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Background ──────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to SpaceBgCenter,
                        0.4f to SpaceBgMid,
                        1.0f to SpaceBgEdge
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.4f),
                    radius = size.maxDimension * 0.8f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AmbientGlow,
                        Color.Transparent
                    ),
                    radius = size.width * 0.7f
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.5f, size.height * 0.42f)
            )
        }

        // ── Card ─────────────────────────────────────────────────────
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
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassWhite004)
                    .border(1.dp, GlassBorder008, RoundedCornerShape(24.dp))
                    .padding(horizontal = 28.dp, vertical = 36.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .glassGlow(
                                color = GradientTealA,
                                borderRadius = 18.dp,
                                glowRadius = 14.dp,
                                glowAlpha = 0.5f
                            )
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x2BFFFFFF), Color(0x0FFFFFFF))
                                )
                            )
                            .border(1.dp, GlassBorder010, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        SparkleIcon(
                            color = GradientTealA,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Heading
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
                                    brush = Brush.linearGradient(
                                        listOf(GradientTealA, GradientTealB)
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            ) {
                                append("Future")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "Supercharge your productivity with AI.",
                        color = SubtitleWhite,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = {
                            Text("Full Name", color = PlaceholderWhite, fontSize = 15.sp)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Name",
                                tint = IconWhite04,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        interactionSource = nameInteractionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FocusBorderTeal,
                            unfocusedBorderColor = GlassInputBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = GlassInput005,
                            unfocusedContainerColor = GlassInput005,
                            cursorColor = GradientTealA,
                            focusedLeadingIconColor = GradientTealA,
                            unfocusedLeadingIconColor = IconWhite04
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isNameFocused) {
                                    Modifier.glassGlow(
                                        color = GradientTealA,
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = {
                            Text("Email Address", color = PlaceholderWhite, fontSize = 15.sp)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Email",
                                tint = IconWhite04,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        interactionSource = emailInteractionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FocusBorderTeal,
                            unfocusedBorderColor = GlassInputBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = GlassInput005,
                            unfocusedContainerColor = GlassInput005,
                            cursorColor = GradientTealA,
                            focusedLeadingIconColor = GradientTealA,
                            unfocusedLeadingIconColor = IconWhite04
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isEmailFocused) {
                                    Modifier.glassGlow(
                                        color = GradientTealA,
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = {
                            Text("Password", color = PlaceholderWhite, fontSize = 15.sp)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = IconWhite04,
                                modifier = Modifier.size(20.dp)
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
                                    tint = IconWhite04,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        interactionSource = passwordInteractionSource,
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FocusBorderTeal,
                            unfocusedBorderColor = GlassInputBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = GlassInput005,
                            unfocusedContainerColor = GlassInput005,
                            cursorColor = GradientTealA,
                            focusedLeadingIconColor = GradientTealA,
                            unfocusedLeadingIconColor = IconWhite04,
                            focusedTrailingIconColor = GradientTealA,
                            unfocusedTrailingIconColor = IconWhite04
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isPasswordFocused) {
                                    Modifier.glassGlow(
                                        color = GradientTealA,
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

                    // Terms Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { termsAccepted = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GradientTealA,
                                uncheckedColor = IconWhite04
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = buildAnnotatedString {
                                append("I agree to the ")
                                withStyle(SpanStyle(color = GradientTealA, fontWeight = FontWeight.Bold)) {
                                    append("Terms of Service")
                                }
                            },
                            color = SubtitleWhite,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // CTA Button - Elegant Frosted Glass Button with Glowing Outline
                    Button(
                        onClick = {
                            if ((name.isNotBlank() && email.isNotBlank() && password.length >= 8 && termsAccepted)) {
                                isLoading = true
                                scope.launch {
                                    delay(900)
                                    AppDataStore.isLoggedIn.value = true
                                    AppDataStore.userName.value = name
                                    AppDataStore.saveToPrefs(context)
                                    isLoading = false
                                    onRegisterSuccess()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .glassGlow(
                                color = GradientTealA,
                                borderRadius = 50.dp,
                                glowRadius = 12.dp,
                                glowAlpha = 0.5f
                            ),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(CtaGradientA, CtaGradientB),
                                        start = Offset.Zero,
                                        end = Offset.Infinite
                                    ),
                                    RoundedCornerShape(50.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isLoading) "Creating account..." else "Join Now →",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Footer
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Already have an account?  ",
                            color = SubtitleWhite,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            "Login here",
                            color = GradientTealA,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.clickable { onNavigateToLogin() }
                        )
                    }
                }
            }
        }
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
