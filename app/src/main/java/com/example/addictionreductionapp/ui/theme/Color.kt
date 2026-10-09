package com.example.addictionreductionapp.ui.theme

import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────────────
//  Black + Teal — App-wide spec
//  App Background #05070B | Card #10161E | Border #1E2936 | Accent #00D09E
//  Icon Circle #16202C | Track #1A2431 | Text White #FFFFFF | Muted #8A9BB0
//  MUST be used by ALL screens: Home, Focus, AI Coach, Settings, Login,
//  Register, Grant Access (Permissions), Analytics, Onboarding, Nav Bar.
//  Do NOT hardcode hex in screens — import from here.
// ──────────────────────────────────────────────────────────────────────

// Primary Accent — Bright Teal #00D09E
val RegainTeal = Color(0xFF00D09E) // primary buttons, nav pills, progress fills, badges, underlines
val RegainTealLight = Color(0xFF34E3B3) // #00D09E light variant
val RegainTealDark = Color(0xFF0A8A6E) // #00D09E dark variant
val RegainPurple = Color(0xFF00D09E) // aliased to teal — single accent system
val RegainPurpleLight = Color(0xFF34E3B3)
val RegainBlue = Color(0xFF00D09E) // aliased to teal
val RegainOrange = Color(0xFFFF9800)
val RegainAmber = Color(0xFFFFCA28)
// Onboarding aliases — all point to #00D09E
val ObAccentTeal = Color(0xFF00D09E)
val ObAccentBlue = Color(0xFF00D09E)
val ObAccentPurple = Color(0xFF00D09E)

// Background & Surface Colors — blackish spec
val DarkBackground = Color(0xFF05070B) // Near-black with blue hint #05070B
val DarkSurface = Color(0xFF090D14) // Dark charcoal alternative
val DarkCard = Color(0xFF10161E) // Neutral dark slate #10161E
val DarkCardLight = Color(0xFF1E2936) // Container outlines / borders — subtle blue-gray stroke
val DarkNavBar = Color(0xFF090D14) // Nav bar — same charcoal

// Text Colors
val TextWhite = Color(0xFFFFFFFF) // Crisp White #FFFFFF
val TextGray = Color(0xFF8A9BB0) // Secondary blue-gray
val TextGrayLight = Color(0xFFB0BEC5)

// Status Colors
val SuccessGreen = Color(0xFF00D09E) // Unified to teal accent (single-colour system)
val WarningYellow = Color(0xFFFFC107)
val ErrorRed = Color(0xFFEF5350)

// Achievement Colors
val GoldAccent = Color(0xFFFFD700)
val SilverAccent = Color(0xFFC0C0C0)
val BronzeAccent = Color(0xFFCD7F32)

// ──────────────────────────────────────────────────────────────────────
//  Black / Teal Glassmorphism — Shared Palette (Single Source)
//  Used by Login, Register, Permissions, Profile, Dashboard, Nav Bar, etc.
// ──────────────────────────────────────────────────────────────────────
val SpaceBgCenter = Color(0xFF05070B)          // Outer background #05070B
val SpaceBgMid = Color(0xFF0A0F16)
val SpaceBgEdge = Color(0xFF05070B)
val AmbientGlow = Color(0x2600D09E)            // rgba(47,124,255,0.15)
val RadialGlowCenter = Color(0x2600D09E)       // #00D09E 15% → transparent
val GlassWhite004 = Color(0x0AFFFFFF)          // rgba(255,255,255,0.04)
val GlassBorder008 = Color(0x14FFFFFF)         // rgba(255,255,255,0.08)
val GlassBorder010 = Color(0x1AFFFFFF)         // rgba(255,255,255,0.10)
val FocusCardBg = Color(0xD910161E)            // #10161E @85% (D9) — card surface
val FocusCardStroke = Color(0xFF1E2936)        // 1dp stroke — #1E2936 spec
val FocusInputBg = Color(0xFF16202C)           // Icon container background #16202C
val FocusInputBorder = Color(0xFF1E2936)       // icon circle border — same stroke
val GlassInput005 = Color(0x0DFFFFFF)
val GlassInputBorder = Color(0x1AFFFFFF)
val PlaceholderWhite = Color(0xFF7C8DA6)       // hint / icon inactive — muted blue-gray
val IconWhite04 = Color(0x66FFFFFF)            // rgba(255,255,255,0.40)
val CyanAccent = Color(0xFF00D09E)            // Primary accent #00D09E
val CyanGlow = Color(0xFF00D09E)              // glow — same teal
val DividerWhite015 = Color(0xFF1E2936)        // divider — #1E2936
val SubtitleWhite = Color(0xFF8A9BB0)         // Muted blue-gray — section subtitles
val LogoBg007 = Color(0x12FFFFFF)              // rgba(255,255,255,0.07)
val GradientTealA = Color(0xFF00D09E)
val GradientTealB = Color(0xFF0A8A6E)
val CtaGradientA = Color(0xFF00D09E)          // button solid #00D09E
val CtaGradientB = Color(0xFF0A8A6E)          // button gradient end
val FocusGlowTeal = Color(0x2600D09E)         // focus glow 15% #00D09E
val FocusBorderTeal = Color(0xFF00D09E)       // focused stroke
val ProgressTrack = Color(0xFF1A2431)         // Unfilled progress track #1A2431
val ValidationError = Color(0xFFFF6B6B)        // soft red
val AppLogoBgTop = Color(0xFF10161E)
val AppLogoBgBot = Color(0xFF05070B)
val AppLogoTeal = Color(0xFF00D09E)
