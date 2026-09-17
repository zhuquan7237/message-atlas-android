package com.messageatlas.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// 现代高端 Indigo & Slate 调色板（浅色）
private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFEF4444),
    errorContainer = Color(0xFFFEE2E2),
    onError = Color.White,
    onErrorContainer = Color(0xFF991B1B)
)

// 深石板暗色，同色相降低亮度，夜间阅读舒适
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DA6FF),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF5FC5F1),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0A5B84),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF003C25),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = Color(0xFF0B1220),
    surface = Color(0xFF121A2A),
    surfaceVariant = Color(0xFF1E293B),
    onSurface = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF3E4C63),
    outlineVariant = Color(0xFF2B3A50),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D),
    onError = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFECACA)
)

private val Type = Typography(
    headlineLarge = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
)

// 视觉渐变与设计常量（明暗自适应部分提供 @Composable 取值函数）
object DesignTokens {
    val HeroGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF3730A3),
            Color(0xFF4F46E5),
            Color(0xFF6366F1)
        )
    )

    val UrgentGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFF3366),
            Color(0xFFFF6584)
        )
    )

    @Composable fun cardBorder(): Color = if (isSystemInDarkTheme()) Color(0xFF2C3648) else Color(0xFFE2E8F0)
    @Composable fun urgentColor(): Color = if (isSystemInDarkTheme()) Color(0xFFFF8199) else Color(0xFFE11D48)
    @Composable fun urgentContainer(): Color = if (isSystemInDarkTheme()) Color(0xFF421B27) else Color(0xFFFEE2E2)
    @Composable fun todoColor(): Color = if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFD97706)
    @Composable fun importantColor(): Color = if (isSystemInDarkTheme()) Color(0xFF8FB2FF) else Color(0xFF2563EB)
    @Composable fun normalColor(): Color = if (isSystemInDarkTheme()) Color(0xFF45D6B8) else Color(0xFF0D9488)
    @Composable fun mutedColor(): Color = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B)
    @Composable fun starColor(): Color = if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFF59E0B)
    @Composable fun onlineGreen(): Color = if (isSystemInDarkTheme()) Color(0xFF4ADE80) else Color(0xFF10B981)
    @Composable fun idleGrey(): Color = if (isSystemInDarkTheme()) Color(0xFF64748B) else Color(0xFF94A3B8)

    // 应用彩色头像分配策略（知名应用品牌色 + 莫兰迪哈希色）
    fun appColor(appName: String, packageName: String): Color {
        val lowerName = appName.lowercase()
        val lowerPkg = packageName.lowercase()
        return when {
            "微信" in lowerName || "wechat" in lowerPkg -> Color(0xFF07C160)
            "钉钉" in lowerName || "dingtalk" in lowerPkg -> Color(0xFF0089FF)
            "飞书" in lowerName || "lark" in lowerPkg -> Color(0xFF00D6B9)
            "qq" in lowerName || "tencent.mobileqq" in lowerPkg -> Color(0xFF12B7F5)
            "支付宝" in lowerName || "alipay" in lowerPkg -> Color(0xFF1677FF)
            "短信" in lowerName || "mms" in lowerPkg || "sms" in lowerPkg -> Color(0xFF3B82F6)
            "电话" in lowerName || "dialer" in lowerPkg -> Color(0xFF10B981)
            "邮件" in lowerName || "mail" in lowerPkg -> Color(0xFF8B5CF6)
            "淘宝" in lowerName || "taobao" in lowerPkg -> Color(0xFFFF5000)
            "京东" in lowerName || "jingdong" in lowerPkg -> Color(0xFFE1251B)
            "美团" in lowerName || "meituan" in lowerPkg -> Color(0xFFFFC300)
            else -> {
                val palette = listOf(
                    Color(0xFF6366F1), Color(0xFF0EA5E9), Color(0xFF10B981),
                    Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899),
                    Color(0xFF14B8A6), Color(0xFFF97316)
                )
                palette[abs(appName.hashCode()) % palette.size]
            }
        }
    }
}

@Composable
fun MessageAtlasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Type,
        content = content
    )
}
