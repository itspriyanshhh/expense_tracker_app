package com.example.pulseledger.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Concrete Dark Mode Constants (Image 1: Pure Black #000000 & Electric Lime)
val DarkBackground = Color(0xFF000000)         // Pure Pitch Black Base Background
val DarkCardSurface = Color(0xFF101510)        // Dark Olive Charcoal Bento Surface
val DarkCardSurfaceElevated = Color(0xFF161E16)// Elevated Bento Card
val DarkBorder = Color(0xFF1F281E)             // Subtle 1dp Olive Border
val DarkBorderFocused = Color(0xFF324830)      // Active / Focused Border
val DarkGlassSemiTransparent = Color(0xEB101510)// Dark glass layer

// Concrete Light Mode Constants (Image 2: Warm Linen / Bone & Deep Royal Forest Green)
val LightBackground = Color(0xFFF5F6F1)        // Warm Organic Linen / Bone Off-White
val LightCardSurface = Color(0xFFFFFFFF)       // Crisp Pure White elevated bento card
val LightCardSurfaceElevated = Color(0xFFECEFE6)// Warm Linen secondary elevated surface
val LightBorder = Color(0xFFDFE3D6)            // Soft Olive-Gray 1dp border
val LightBorderFocused = Color(0xFFC5CCC4)     // Active border
val LightGlassSemiTransparent = Color(0xF2FFFFFF)// 95% opacity white glass layer

// Special Hero Tokens
val ForestGreenHero = Color(0xFF133E2B)        // Image 2 Hero Card: Royal Forest Green
val ForestGreenHeroElevated = Color(0xFF1D4D37)// Hero Pill / Badge Background
val ForestGreenHeroSub = Color(0xFFA3D9B8)     // Soft Mint subtitle and pill text
val ElectricLimeAccent = Color(0xFFD4FF00)     // Image 1 Signature Electric Lime

// Semantic Accents (Dark) — Electric Lime as Primary Highlight, Crimson for Debit
val DarkEmeraldNeon = Color(0xFFD4FF00)        // Electric Lime (#D4FF00) signature accent
val DarkEmeraldGlow = Color(0x26D4FF00)        // 15% lime glow wash
val DarkCrimsonCoral = Color(0xFFFF4B6E)       // Expense, Debits, Critical Alerts
val DarkCrimsonGlow = Color(0x26FF4B6E)        // 15% crimson glow wash
val DarkGlowingAmber = Color(0xFFFFB800)       // Pending User Tap / Warnings
val DarkAmberGlow = Color(0x26FFB800)          // 15% amber glow wash

// Semantic Accents (Light) — Deep Forest Green & Crisp Emerald
val LightEmeraldNeon = Color(0xFF133E2B)       // Royal Forest Green primary accent
val LightEmeraldGlow = Color(0x20133E2B)       // 12% forest green glow wash
val LightCrimsonCoral = Color(0xFFE11D48)      // Deep crimson rose
val LightCrimsonGlow = Color(0x18E11D48)       // 10% glow wash
val LightGlowingAmber = Color(0xFFD97706)      // Deep amber
val LightAmberGlow = Color(0x18D97706)         // 10% glow wash

// Content / Typography Colors (Dark)
val DarkTextPrimary = Color(0xFFFFFFFF)        // Crisp pure white primary text
val DarkTextSecondary = Color(0xFF869584)      // Soft olive-sage subtitles & metadata
val DarkTextMuted = Color(0xFF5A6858)          // Captions, hints, disabled
val DarkTextDisabled = Color(0xFF384337)

// Content / Typography Colors (Light)
val LightTextPrimary = Color(0xFF111812)       // Deep Charcoal Green / Obsidian Black
val LightTextSecondary = Color(0xFF4E5D50)     // Refined Sage-Slate
val LightTextMuted = Color(0xFF7E8E80)         // Soft Linen-Gray
val LightTextDisabled = Color(0xFFC5CEC6)

// Category Accent Colors (Curated 5-tone green & monochrome fintech family)
val CatFoodDining = Color(0xFF85C99F)         // Soft Mint Green
val CatGroceries = Color(0xFF4ADE80)          // Vibrant Sprout Green
val CatTransitTravel = Color(0xFF6EE7B7)      // Cool Celadon Mint
val CatShopping = Color(0xFF34D399)           // Jade Emerald
val CatBillsUtilities = Color(0xFFA3E635)     // Lime Sprout
val CatEntertainment = Color(0xFF10B981)      // Emerald Neon
val CatMedical = Color(0xFF2DD4BF)            // Calm Teal Mint
val CatEducation = Color(0xFF86EFAC)          // Pale Mint
val CatInvestments = Color(0xFFD4FF00)        // Signature Electric Lime
val CatHomeRent = Color(0xFF65A30D)           // Olive Sprout
val CatPersonalCare = Color(0xFF5EEAD4)       // Soft Aqua Mint
val CatInsurance = Color(0xFF99F6E4)          // Celadon
val CatUncategorized = Color(0xFFFBBF24)      // Warm Amber (Pending User Review)

@Immutable
data class PulseCustomColors(
    val isDark: Boolean = true,
    val background: Color = DarkBackground,
    val cardSurface: Color = DarkCardSurface,
    val cardSurfaceElevated: Color = DarkCardSurfaceElevated,
    val border: Color = DarkBorder,
    val borderFocused: Color = DarkBorderFocused,
    val emeraldNeon: Color = DarkEmeraldNeon,
    val emeraldGlow: Color = DarkEmeraldGlow,
    val crimsonCoral: Color = DarkCrimsonCoral,
    val crimsonGlow: Color = DarkCrimsonGlow,
    val glowingAmber: Color = DarkGlowingAmber,
    val amberGlow: Color = DarkAmberGlow,
    val textPrimary: Color = DarkTextPrimary,
    val textSecondary: Color = DarkTextSecondary,
    val textMuted: Color = DarkTextMuted,
    val scrim: Color = Color(0xCC000000),

    // Hero Card Semantic Styling
    val heroCardSurface: Color = DarkCardSurface,
    val heroCardBorder: Color = DarkBorder,
    val heroCardTextPrimary: Color = DarkTextPrimary,
    val heroCardTextSecondary: Color = DarkTextSecondary,
    val heroCardPillBg: Color = DarkEmeraldGlow,
    val heroCardPillBorder: Color = DarkEmeraldNeon.copy(alpha = 0.4f),
    val heroCardPillText: Color = DarkEmeraldNeon,

    // Pill & Action Button Styling
    val pillPrimary: Color = ElectricLimeAccent,
    val pillOnPrimary: Color = Color(0xFF000000),
    val pillSecondary: Color = Color(0xFFFFFFFF),
    val pillOnSecondary: Color = Color(0xFF000000),

    // Chart Palette Tokens
    val chartTrack: Color = Color(0xFF202C1E),
    val chartSelected: Color = ElectricLimeAccent
)

val DarkPulseCustomColors = PulseCustomColors(
    isDark = true,
    background = DarkBackground,
    cardSurface = DarkCardSurface,
    cardSurfaceElevated = DarkCardSurfaceElevated,
    border = DarkBorder,
    borderFocused = DarkBorderFocused,
    emeraldNeon = DarkEmeraldNeon,
    emeraldGlow = DarkEmeraldGlow,
    crimsonCoral = DarkCrimsonCoral,
    crimsonGlow = DarkCrimsonGlow,
    glowingAmber = DarkGlowingAmber,
    amberGlow = DarkAmberGlow,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    scrim = Color(0xCC000000),

    // Hero in Dark Mode is DarkCardSurface with Lime highlights
    heroCardSurface = DarkCardSurface,
    heroCardBorder = DarkBorder,
    heroCardTextPrimary = DarkTextPrimary,
    heroCardTextSecondary = DarkTextSecondary,
    heroCardPillBg = DarkEmeraldGlow,
    heroCardPillBorder = DarkEmeraldNeon.copy(alpha = 0.4f),
    heroCardPillText = DarkEmeraldNeon,

    pillPrimary = ElectricLimeAccent,
    pillOnPrimary = Color(0xFF000000),
    pillSecondary = Color(0xFFFFFFFF),
    pillOnSecondary = Color(0xFF000000),

    chartTrack = Color(0xFF202C1E),
    chartSelected = ElectricLimeAccent
)

val LightPulseCustomColors = PulseCustomColors(
    isDark = false,
    background = LightBackground,
    cardSurface = LightCardSurface,
    cardSurfaceElevated = LightCardSurfaceElevated,
    border = LightBorder,
    borderFocused = LightBorderFocused,
    emeraldNeon = LightEmeraldNeon,
    emeraldGlow = LightEmeraldGlow,
    crimsonCoral = LightCrimsonCoral,
    crimsonGlow = LightCrimsonGlow,
    glowingAmber = LightGlowingAmber,
    amberGlow = LightAmberGlow,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    scrim = Color(0x66000000),

    // Hero in Light Mode is the signature Deep Royal Forest Green container (#133E2B)
    heroCardSurface = ForestGreenHero,
    heroCardBorder = Color(0xFF1E523A),
    heroCardTextPrimary = Color(0xFFFFFFFF),
    heroCardTextSecondary = ForestGreenHeroSub,
    heroCardPillBg = ForestGreenHeroElevated,
    heroCardPillBorder = Color(0xFF2A684C),
    heroCardPillText = ForestGreenHeroSub,

    pillPrimary = ForestGreenHero,
    pillOnPrimary = Color(0xFFFFFFFF),
    pillSecondary = Color(0xFFFFFFFF),
    pillOnSecondary = LightTextPrimary,

    chartTrack = Color(0xFFDCE2D5),
    chartSelected = ForestGreenHero
)

val LocalPulseColors = staticCompositionLocalOf { DarkPulseCustomColors }

// Dynamic Composable Color Accessors — all composables automatically resolve to current theme!
val ObsidianSlate: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.background

val GlassSlate: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.cardSurface

val GlassSlateElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.cardSurfaceElevated

val BorderSlate: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.border

val BorderSlateFocused: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.borderFocused

val GlassSlateSemiTransparent: Color
    @Composable
    @ReadOnlyComposable
    get() = if (PulseTheme.colors.isDark) DarkGlassSemiTransparent else LightGlassSemiTransparent

val EmeraldNeon: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.emeraldNeon

val EmeraldNeonGlow: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.emeraldGlow

val CrimsonCoral: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.crimsonCoral

val CrimsonCoralGlow: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.crimsonGlow

val GlowingAmber: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.glowingAmber

val GlowingAmberGlow: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.amberGlow

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.textSecondary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.textMuted

val TextDisabled: Color
    @Composable
    @ReadOnlyComposable
    get() = if (PulseTheme.colors.isDark) DarkTextDisabled else LightTextDisabled

// Hero Card Accessors
val HeroCardSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.heroCardSurface

val HeroCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.heroCardBorder

val HeroCardTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.heroCardTextPrimary

val HeroCardTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.heroCardTextSecondary

val HeroCardPillBg: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.heroCardPillBg

val HeroCardPillBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.heroCardPillBorder

val HeroCardPillText: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.heroCardPillText

// Pill & Action Button Accessors
val PillPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.pillPrimary

val PillOnPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.pillOnPrimary

val PillSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.pillSecondary

val PillOnSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.pillOnSecondary

// Signature Colors
val ElectricLime: Color
    @Composable
    @ReadOnlyComposable
    get() = ElectricLimeAccent

val DeepForestGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = ForestGreenHero

val ChartTrackColor: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.chartTrack

val ChartSelectedColor: Color
    @Composable
    @ReadOnlyComposable
    get() = PulseTheme.colors.chartSelected
