package com.messageatlas.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF6950A1), onPrimary = Color.White,
    primaryContainer = Color(0xFFECE5F7), onPrimaryContainer = Color(0xFF362350),
    secondary = Color(0xFF59694D), secondaryContainer = Color(0xFFE7ECDC), onSecondaryContainer = Color(0xFF25331D),
    tertiary = Color(0xFF956344), tertiaryContainer = Color(0xFFF8E6D9),
    background = Color(0xFFF8F7F4), surface = Color(0xFFFFFEFC),
    surfaceVariant = Color(0xFFF0EEEA), onSurface = Color(0xFF28252D), onSurfaceVariant = Color(0xFF706B76),
    outline = Color(0xFFA39CA9), outlineVariant = Color(0xFFE7E3EA),
    error = Color(0xFFAC394A), errorContainer = Color(0xFFFBE9EB), onErrorContainer = Color(0xFF7A2230)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFCDB8F1), onPrimary = Color(0xFF35204F),
    primaryContainer = Color(0xFF3A304B), onPrimaryContainer = Color(0xFFEADCFB),
    secondary = Color(0xFFB7CCA1), secondaryContainer = Color(0xFF303D27), onSecondaryContainer = Color(0xFFDDECCC),
    tertiary = Color(0xFFE7BB98), tertiaryContainer = Color(0xFF4C3629),
    background = Color(0xFF17151B), surface = Color(0xFF211E26),
    surfaceVariant = Color(0xFF2B2731), onSurface = Color(0xFFF1ECF4), onSurfaceVariant = Color(0xFFB7AFBF),
    outline = Color(0xFF807687), outlineVariant = Color(0xFF38323F),
    error = Color(0xFFFFADBA), errorContainer = Color(0xFF4D2830), onErrorContainer = Color(0xFFFFD9DF)
)
private val AtlasTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)

internal object DesignTokens {
    val Ink = Color(0xFF352A48)
    val Lavender = Color(0xFFDACEEC)
    @Composable fun urgentColor() = MaterialTheme.colorScheme.error
    @Composable fun todoColor() = MaterialTheme.colorScheme.tertiary
    @Composable fun importantColor() = MaterialTheme.colorScheme.primary
    @Composable fun normalColor() = MaterialTheme.colorScheme.secondary
    @Composable fun mutedColor() = MaterialTheme.colorScheme.onSurfaceVariant
    private val palette = listOf(Color(0xFF8571AD), Color(0xFF6D8C9E), Color(0xFF7D9469), Color(0xFFAD8870), Color(0xFFAB7F94))
    fun appColor(appName: String, packageName: String): Color = when {
        packageName == "com.tencent.mm" -> Color(0xFF668F72)
        "mail" in packageName -> Color(0xFF7D78AC)
        "ding" in packageName || "lark" in packageName -> Color(0xFF6086A6)
        else -> palette[Math.floorMod((packageName.ifBlank { appName }).hashCode(), palette.size)]
    }
}

@Composable
fun MessageAtlasTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = AtlasTypography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)), content = content)
}
