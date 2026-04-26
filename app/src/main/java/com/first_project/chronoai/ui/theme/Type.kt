package com.first_project.chronoai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.R

// 1. PROVIDER SETUP
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// 2. TYPEFACE SYSTEM

// Display Font (Google Pixel style Comic Font - Comic Neue)
val ComicFont = FontFamily(
    Font(googleFont = GoogleFont("Comic Neue"), fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = GoogleFont("Comic Neue"), fontProvider = provider, weight = FontWeight.ExtraBold)
)

// UI Font (Sans-serif, clean for body and labels)
val SyneFont = FontFamily(
    Font(googleFont = GoogleFont("Syne"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Syne"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Syne"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Syne"), fontProvider = provider, weight = FontWeight.Bold)
)

val JetBrainsMonoFont = FontFamily(
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider, weight = FontWeight.Medium)
)

// 3. EXPRESSIVE SCALE (Vynta v1.0 Scale)
val VyntaTypography = Typography(
    // Used for Greetings ("Good Morning", etc.) using the Comic Font
    displayLarge = TextStyle(
        fontFamily = ComicFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = ComicFont,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    
    // UI Elements using Syne for a modern look
    headlineMedium = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    
    // Fallbacks
    displaySmall = TextStyle(
        fontFamily = ComicFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)

// MONO TIME (Strictly for numerical data, time-blocks, and durations)
val MonoTime = TextStyle(
    fontFamily = JetBrainsMonoFont,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp
)
