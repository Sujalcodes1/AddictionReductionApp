package com.example.addictionreductionapp.ui.theme

import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────────────
//  Dark Emerald / Mint Teal — Exact Home Screen Spec
//  App Background #070B0E | Card #111C24 | Border #1A2B36 | Accent #00D09E
//  Icon Circle #172A33 | Track #182833 | Text White #FFFFFF | Muted #569A93
//  MUST be used by ALL screens: Home, Focus, AI Coach, Settings, Login,
//  Register, Grant Access (Permissions), Analytics, Onboarding, Nav Bar.
//  Do NOT hardcode hex in screens — import from here.
// ──────────────────────────────────────────────────────────────────────

// Primary Accent — Bright Emerald / Mint Green #00D09E (spec #00D09E / #10B981)
val RegainTeal = Color(0xFF00D09E) // primary buttons, nav pills, progress fills, badges, underlines
val RegainTealLight = Color(0xFF34E3B3) // #00D09E light variant
val RegainTealDark = Color(0xFF0A8A6E) // #00D09E dark variant
val RegainPurple = Color(0xFF00D09E) // aliased to emerald — single accent system
val RegainPurpleLight = Color(0xFF34E3B3)
val RegainBlue = Color(0xFF00D09E) // aliased to emerald
val RegainOrange = Color(0xFFFF9800)
val RegainAmber = Color(0xFFFFCA28)
// Onboarding aliases — all point to #00D09E
val ObAccentTeal = Color(0xFF00D09E)
val ObAccentBlue = Color(0xFF00D09E)
val ObAccentPurple = Color(0xFF00D09E)

// Background & Surface Colors — spec exact
val DarkBackground = Color(0xFF070B0E) // Deep Pitch Black #070B0E (spec #070B0E / #0A0F14)
val DarkSurface = Color(0xFF0A0F14) // Dark Charcoal alternative
val DarkCard = Color(0xFF111C24) // Dark Teal-Tinted Slate #111C24 (spec #111C24 / #131F28)
val DarkCardLight = Color(0xFF1A2B36) // Container Outlines / Borders — Subtle Dark Teal Stroke #1A2B36
val DarkNavBar = Color(0xFF0A0F14) // Nav bar — same charcoal

// Text Colors — spec exact
val TextWhite = Color(0xFFFFFFFF) // Crisp White #FFFFFF
val TextGray = Color(0xFF8E99A4) // Secondary gray (fallback)
val TextGrayLight = Color(0xFFB0BEC5)

// Status Colors
val SuccessGreen = Color(0xFF00D09E) // Exact Emerald Mint Teal
val WarningYellow = Color(0xFFFFC107)
val ErrorRed = Color(0xFFEF5350)

// Achievement Colors
val GoldAccent = Color(0xFFFFD700)
val SilverAccent = Color(0xFFC0C0C0)
val BronzeAccent = Color(0xFFCD7F32)

// ──────────────────────────────────────────────────────────────────────
//  Deep-Space / Dark Emerald Glassmorphism — Shared Palette (Single Source)
//  Used by Login, Register, Permissions, Profile, Dashboard, Nav Bar, etc.
// ──────────────────────────────────────────────────────────────────────
val SpaceBgCenter = Color(0xFF070B0E)          // Outer background #070B0E
val SpaceBgMid = Color(0xFF0A0F14)
val SpaceBgEdge = Color(0xFF070B0E)
val AmbientGlow = Color(0x2600D09E)            // rgba(0,208,158,0.15)
val RadialGlowCenter = Color(0x2600D09E)       // #00D09E 15% → transparent
val GlassWhite004 = Color(0x0AFFFFFF)          // rgba(255,255,255,0.04)
val GlassBorder008 = Color(0x14FFFFFF)         // rgba(255,255,255,0.08)
val GlassBorder010 = Color(0x1AFFFFFF)         // rgba(255,255,255,0.10)
val FocusCardBg = Color(0xD9111C24)            // #111C24 @85% (D9) — card surface
val FocusCardStroke = Color(0xFF1A2B36)        // 1dp stroke — #1A2B36 spec
val FocusInputBg = Color(0xFF172A33)           // Icon Container Background #172A33 (spec #172A33 / #14252E)
val FocusInputBorder = Color(0xFF1A2B36)       // icon circle border — same stroke
val GlassInput005 = Color(0x0DFFFFFF)
val GlassInputBorder = Color(0x1AFFFFFF)
val PlaceholderWhite = Color(0xFF569A93)       // hint / icon inactive — Muted Teal #569A93
val IconWhite04 = Color(0x66FFFFFF)            // rgba(255,255,255,0.40)
val CyanAccent = Color(0xFF00D09E)            // Primary accent #00D09E
val CyanGlow = Color(0xFF00D09E)              // glow — same emerald
val DividerWhite015 = Color(0xFF1A2B36)        // divider — #1A2B36
val SubtitleWhite = Color(0xFF569A93)         // Muted Teal #569A93 — section subtitles
val LogoBg007 = Color(0x12FFFFFF)              // rgba(255,255,255,0.07)
val GradientTealA = Color(0xFF00D09E)
val GradientTealB = Color(0xFF0A8A6E)
val CtaGradientA = Color(0xFF00D09E)          // button solid #00D09E
val CtaGradientB = Color(0xFF0A8A6E)          // button gradient end
val FocusGlowTeal = Color(0x2600D09E)         // focus glow 15% #00D09E
val FocusBorderTeal = Color(0xFF00D09E)       // focused stroke
val ProgressTrack = Color(0xFF182833)         // Unfilled Progress Track #182833 spec
val ValidationError = Color(0xFFFF6B6B)        // soft red
val AppLogoBgTop = Color(0xFF111C24)
val AppLogoBgBot = Color(0xFF070B0E)
val AppLogoTeal = Color(0xFF00D09E)
