package com.messageatlas.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Colors = lightColorScheme(
    primary = Color(0xFF52665A), onPrimary = Color.White, primaryContainer = Color(0xFFDDE7E0),
    secondary = Color(0xFF736C5F), background = Color(0xFFF7F8F5), surface = Color(0xFFFDFDF9),
    surfaceVariant = Color(0xFFECEEE9), onSurface = Color(0xFF1B1D1B), outline = Color(0xFF777B77),
    error = Color(0xFF9B4E4E)
)
private val Type = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
)

@Composable
fun MessageAtlasTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Type, content = content)
}
