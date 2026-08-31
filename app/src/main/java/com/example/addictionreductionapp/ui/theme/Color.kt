package com.example.addictionreductionapp.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Accent — unified to current app shade #00BFA5 (lag-safe, single teal)
val RegainTeal = Color(0xFF00BFA5) // cyan_accent — all onboarding pages
val RegainTealLight = Color(0xFF64FFDA)
val RegainTealDark = Color(0xFF00897B)
val RegainPurple = Color(0xFF00BFA5) // aliased to teal per new design (no per-page blue/purple)
val RegainPurpleLight = Color(0xFFCE93D8)
val RegainBlue = Color(0xFF00BFA5) // aliased to teal
val RegainOrange = Color(0xFFFF9800)
val RegainAmber = Color(0xFFFFCA28)
// Onboarding aliases — all point to current app shade #00BFA5
val ObAccentTeal = Color(0xFF00BFA5)
val ObAccentBlue = Color(0xFF00BFA5)
val ObAccentPurple = Color(0xFF00BFA5)

// Background & Surface Colors
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF0A0E14)
val DarkCard = Color(0xFF0F171E)
val DarkCardLight = Color(0xFF1B262F)
val DarkNavBar = Color(0xFF0A0F16)

// Text Colors
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFF8E99A4)
val TextGrayLight = Color(0xFFB0BEC5)

// Status Colors
val SuccessGreen = Color(0xFF4CAF50)
val WarningYellow = Color(0xFFFFC107)
val ErrorRed = Color(0xFFEF5350)

// Achievement Colors
val GoldAccent = Color(0xFFFFD700)
val SilverAccent = Color(0xFFC0C0C0)
val BronzeAccent = Color(0xFFCD7F32)

// ──────────────────────────────────────────────────────────────────────
//  Deep-Space Glassmorphism — Shared Palette (Single Source of Truth)
//  Used by Login, Register, Permissions, Profile, Dashboard, etc.
//  DO NOT duplicate these values in screens — import from here.
// ──────────────────────────────────────────────────────────────────────
val SpaceBgCenter = Color(0xFF010D0F)          // Outer background #010d0f
val SpaceBgMid = Color(0xFF0D1E28)
val SpaceBgEdge = Color(0xFF0A1520)
val AmbientGlow = Color(0x26148C8C)            // rgba(20,120,140,0.15)
val RadialGlowCenter = Color(0x2600D4AA)       // #00d4aa 15% → transparent
val GlassWhite004 = Color(0x0AFFFFFF)          // rgba(255,255,255,0.04)
val GlassBorder008 = Color(0x14FFFFFF)         // rgba(255,255,255,0.08)
val GlassBorder010 = Color(0x1AFFFFFF)         // rgba(255,255,255,0.10)
val FocusCardBg = Color(0xD90D2A30)            // #0d2a30 @85% (D9)
val FocusCardStroke = Color(0xFF1A4A52)        // 1dp stroke
val FocusInputBg = Color(0xFF0F2D35)           // input / icon circle bg
val FocusInputBorder = Color(0xFF1E4A54)       // input / icon circle border
val GlassInput005 = Color(0x0DFFFFFF)
val GlassInputBorder = Color(0x1AFFFFFF)
val PlaceholderWhite = Color(0xFF7A9AAA)       // hint / icon inactive
val IconWhite04 = Color(0x66FFFFFF)            // rgba(255,255,255,0.40)
val CyanAccent = Color(0xFF00BFA5)            // cyan accent #00BFA5 (current app shade)
val CyanGlow = Color(0xFF00FFCC)              // cyan glow #00ffcc
val DividerWhite015 = Color(0xFF1A4A52)        // divider #1a4a52
val SubtitleWhite = Color(0xFF7A9AAA)         // secondary text #7a9aaa
val LogoBg007 = Color(0x12FFFFFF)              // rgba(255,255,255,0.07)
val GradientTealA = Color(0xFF00BFA5)
val GradientTealB = Color(0xFF00BFA5)
val CtaGradientA = Color(0xFF00BFA5)          // button solid #00BFA5
val CtaGradientB = Color(0xFF00BFA5)          // button solid #00BFA5
val FocusGlowTeal = Color(0x2600BFA5)         // focus glow 15% #00BFA5
val FocusBorderTeal = Color(0xFF00BFA5)       // focused stroke
val ValidationError = Color(0xFFFF6B6B)        // soft red
val AppLogoBgTop = Color(0xFF08181A)
val AppLogoBgBot = Color(0xFF020C0E)
val AppLogoTeal = Color(0xFF37E6DA)