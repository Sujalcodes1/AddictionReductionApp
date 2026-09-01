package com.example.addictionreductionapp.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Accent — unified to Image 1 shade #14B8A6 (deep teal, screenshot-matched)
val RegainTeal = Color(0xFF14B8A6) // image accent — login/register/grant/home/focus/status/ai coach/settings/nav
val RegainTealLight = Color(0xFF5EEAD4)
val RegainTealDark = Color(0xFF0F766E)
val RegainPurple = Color(0xFF14B8A6) // aliased to image teal (single shade system)
val RegainPurpleLight = Color(0xFF5EEAD4)
val RegainBlue = Color(0xFF14B8A6) // aliased to image teal
val RegainOrange = Color(0xFFFF9800)
val RegainAmber = Color(0xFFFFCA28)
// Onboarding aliases — all point to image shade #14B8A6
val ObAccentTeal = Color(0xFF14B8A6)
val ObAccentBlue = Color(0xFF14B8A6)
val ObAccentPurple = Color(0xFF14B8A6)

// Background & Surface Colors — Image 1 deep-space (teal-tinted near-black)
val DarkBackground = Color(0xFF020C0E)
val DarkSurface = Color(0xFF0A1E22)
val DarkCard = Color(0xFF11272D)
val DarkCardLight = Color(0xFF1C3339)
val DarkNavBar = Color(0xFF0A1E22)

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
//  Image 1 matched — deep teal near-black + #14B8A6 accent
//  Used by Login, Register, Permissions, Profile, Dashboard, etc.
//  DO NOT duplicate these values in screens — import from here.
// ──────────────────────────────────────────────────────────────────────
val SpaceBgCenter = Color(0xFF021418)          // Outer background image-matched #021418
val SpaceBgMid = Color(0xFF0C252C)
val SpaceBgEdge = Color(0xFF071A1E)
val AmbientGlow = Color(0x2614B8A6)            // rgba(20,184,166,0.15) image glow
val RadialGlowCenter = Color(0x2614B8A6)       // #14B8A6 15% → transparent
val GlassWhite004 = Color(0x0AFFFFFF)          // rgba(255,255,255,0.04)
val GlassBorder008 = Color(0x14FFFFFF)         // rgba(255,255,255,0.08)
val GlassBorder010 = Color(0x1AFFFFFF)         // rgba(255,255,255,0.10)
val FocusCardBg = Color(0xD911272D)            // #11272D @85% image card
val FocusCardStroke = Color(0xFF1E3D44)        // 1dp stroke image-matched
val FocusInputBg = Color(0xFF112A30)           // input / icon circle bg image
val FocusInputBorder = Color(0xFF1E3D44)       // input / icon circle border
val GlassInput005 = Color(0x0DFFFFFF)
val GlassInputBorder = Color(0x1AFFFFFF)
val PlaceholderWhite = Color(0xFF7A9AAA)       // hint / icon inactive
val IconWhite04 = Color(0x66FFFFFF)            // rgba(255,255,255,0.40)
val CyanAccent = Color(0xFF14B8A6)            // image accent #14B8A6
val CyanGlow = Color(0xFF2DD4BF)              // cyan glow image-matched
val DividerWhite015 = Color(0xFF1E3D44)        // divider image-matched
val SubtitleWhite = Color(0xFF7A9AAA)         // secondary text #7a9aaa
val LogoBg007 = Color(0x12FFFFFF)              // rgba(255,255,255,0.07)
val GradientTealA = Color(0xFF14B8A6)
val GradientTealB = Color(0xFF0F766E)
val CtaGradientA = Color(0xFF14B8A6)          // button solid image shade
val CtaGradientB = Color(0xFF0F766E)          // button gradient end
val FocusGlowTeal = Color(0x2614B8A6)         // focus glow 15% #14B8A6
val FocusBorderTeal = Color(0xFF14B8A6)       // focused stroke
val ValidationError = Color(0xFFFF6B6B)        // soft red
val AppLogoBgTop = Color(0xFF082022)
val AppLogoBgBot = Color(0xFF021010)
val AppLogoTeal = Color(0xFF14B8A6)