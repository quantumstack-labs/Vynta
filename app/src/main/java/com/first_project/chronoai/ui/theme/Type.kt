package com.first_project.chronoai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GFont
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

// 1. PROVIDER SETUP (Optional: Kept for JetBrains Mono if not bundled)
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// 2. TYPEFACE SYSTEM

// Display Font (Modern Expressive Font - Bricolage Grotesque)
// Using bundled variable font
val BricolageFont = FontFamily(
    Font(R.font.bricolage_grotesque, weight = FontWeight.Normal),
    Font(R.font.bricolage_grotesque, weight = FontWeight.Medium),
    Font(R.font.bricolage_grotesque, weight = FontWeight.SemiBold),
    Font(R.font.bricolage_grotesque, weight = FontWeight.Bold),
    Font(R.font.bricolage_grotesque, weight = FontWeight.ExtraBold)
)

// UI Font (Sans-serif, clean for body and labels)
// Using bundled variable font
val SyneFont = FontFamily(
    Font(R.font.syne, weight = FontWeight.Normal),
    Font(R.font.syne, weight = FontWeight.Medium),
    Font(R.font.syne, weight = FontWeight.SemiBold),
    Font(R.font.syne, weight = FontWeight.Bold)
)

val JetBrainsMonoFont = FontFamily(
    GFont(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider, weight = FontWeight.Medium)
)

// 3. EXPRESSIVE SCALE (Vynta v2.0 Expressive Scale)
val VyntaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = BricolageFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = (-2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = BricolageFont,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1).sp
    ),
    displaySmall = TextStyle(
        fontFamily = BricolageFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = BricolageFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = BricolageFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BricolageFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.5.sp
    )
)

// MONO TIME (Strictly for numerical data, time-blocks, and durations)
val MonoTime = TextStyle(
    fontFamily = JetBrainsMonoFont,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

@Preview(showBackground = true)
@Composable
fun TypographyPreview() {
    VyntaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Good Morning", style = VyntaTypography.displayLarge)
                Spacer(Modifier.height(8.dp))
                Text("Your Flow", style = VyntaTypography.displayMedium)
                Spacer(Modifier.height(16.dp))
                Text("Headline Large", style = VyntaTypography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Text("Title Large - Bricolage", style = VyntaTypography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Title Medium - Syne", style = VyntaTypography.titleMedium)
                Spacer(Modifier.height(16.dp))
                Text("This is body large text using Syne font. It is clean and modern.", style = VyntaTypography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text("LABEL LARGE", style = VyntaTypography.labelLarge)
                Spacer(Modifier.height(16.dp))
                Text("12:45", style = MonoTime)
            }
        }
    }
}
