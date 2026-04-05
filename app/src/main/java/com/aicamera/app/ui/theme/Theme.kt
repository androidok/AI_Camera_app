package com.aicamera.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * ============================================
 * AI 相机应用 - 主题系统
 * ============================================
 *
 * 支持三种主题风格：
 * 1. PROFESSIONAL - 专业摄影风格（橙色/琥珀色）
 * 2. TECH - 科技蓝风格
 * 3. FRESH - 明亮清新风格
 */

enum class ThemeType {
    PROFESSIONAL, // 专业摄影风格
    TECH,       // 科技蓝风格
    FRESH       // 明亮清新风格
}

/**
 * 获取当前颜色方案
 */
fun getColorScheme(themeType: ThemeType = ThemeType.PROFESSIONAL): AppColorScheme {
    return when (themeType) {
        ThemeType.PROFESSIONAL -> ProfessionalColorScheme
        ThemeType.TECH -> TechColorScheme
        ThemeType.FRESH -> FreshColorScheme
    }
}

/**
 * 应用颜色方案数据类
 */
data class AppColorScheme(
    val primary: androidx.compose.ui.graphics.Color,
    val onPrimary: androidx.compose.ui.graphics.Color,
    val primaryContainer: androidx.compose.ui.graphics.Color,
    val onPrimaryContainer: androidx.compose.ui.graphics.Color,
    val secondary: androidx.compose.ui.graphics.Color,
    val onSecondary: androidx.compose.ui.graphics.Color,
    val secondaryContainer: androidx.compose.ui.graphics.Color,
    val onSecondaryContainer: androidx.compose.ui.graphics.Color,
    val tertiary: androidx.compose.ui.graphics.Color,
    val onTertiary: androidx.compose.ui.graphics.Color,
    val tertiaryContainer: androidx.compose.ui.graphics.Color,
    val onTertiaryContainer: androidx.compose.ui.graphics.Color,
    val background: androidx.compose.ui.graphics.Color,
    val onBackground: androidx.compose.ui.graphics.Color,
    val surface: androidx.compose.ui.graphics.Color,
    val onSurface: androidx.compose.ui.graphics.Color,
    val surfaceVariant: androidx.compose.ui.graphics.Color,
    val onSurfaceVariant: androidx.compose.ui.graphics.Color,
    val error: androidx.compose.ui.graphics.Color,
    val onError: androidx.compose.ui.graphics.Color,
    val errorContainer: androidx.compose.ui.graphics.Color,
    val onErrorContainer: androidx.compose.ui.graphics.Color,
    val outline: androidx.compose.ui.graphics.Color,
    val outlineVariant: androidx.compose.ui.graphics.Color,
    val scrim: androidx.compose.ui.graphics.Color,
    val inverseSurface: androidx.compose.ui.graphics.Color,
    val inverseOnSurface: androidx.compose.ui.graphics.Color,
    val inversePrimary: androidx.compose.ui.graphics.Color,
    val surfaceDim: androidx.compose.ui.graphics.Color,
    val surfaceBright: androidx.compose.ui.graphics.Color,
    val surfaceContainerLowest: androidx.compose.ui.graphics.Color,
    val surfaceContainerLow: androidx.compose.ui.graphics.Color,
    val surfaceContainer: androidx.compose.ui.graphics.Color,
    val surfaceContainerHigh: androidx.compose.ui.graphics.Color,
    val surfaceContainerHighest: androidx.compose.ui.graphics.Color,
)

/**
 * 专业摄影风格颜色方案 - 橙色/琥珀色主题
 */
val ProfessionalColorScheme = AppColorScheme(
    primary = ProfessionalColors.Primary,
    onPrimary = Color(0xFF000000),
    primaryContainer = ProfessionalColors.SurfaceElevated,
    onPrimaryContainer = ProfessionalColors.Primary,
    secondary = ProfessionalColors.AccentBlue,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = ProfessionalColors.Surface,
    onSecondaryContainer = ProfessionalColors.AccentBlue,
    tertiary = ProfessionalColors.AccentGreen,
    onTertiary = Color(0xFF000000),
    tertiaryContainer = ProfessionalColors.Surface,
    onTertiaryContainer = ProfessionalColors.AccentGreen,
    background = ProfessionalColors.Background,
    onBackground = ProfessionalColors.TextPrimary,
    surface = ProfessionalColors.Surface,
    onSurface = ProfessionalColors.TextPrimary,
    surfaceVariant = ProfessionalColors.SurfaceElevated,
    onSurfaceVariant = ProfessionalColors.TextSecondary,
    error = ProfessionalColors.AccentRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = ProfessionalColors.Surface,
    onErrorContainer = ProfessionalColors.AccentRed,
    outline = ProfessionalColors.Border,
    outlineVariant = ProfessionalColors.TextTertiary,
    scrim = ProfessionalColors.Overlay,
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = ProfessionalColors.PrimaryLight,
    surfaceDim = ProfessionalColors.Background,
    surfaceBright = ProfessionalColors.SurfaceElevated,
    surfaceContainerLowest = ProfessionalColors.Background,
    surfaceContainerLow = ProfessionalColors.Surface,
    surfaceContainer = ProfessionalColors.SurfaceElevated,
    surfaceContainerHigh = ProfessionalColors.SurfaceElevated,
    surfaceContainerHighest = Color(0xFF3A3A3C),
)

/**
 * 科技蓝风格颜色方案 - 霓虹青蓝主题
 */
val TechColorScheme = AppColorScheme(
    primary = TechColors.Primary,
    onPrimary = Color(0xFF000000),
    primaryContainer = TechColors.SurfaceElevated,
    onPrimaryContainer = TechColors.Primary,
    secondary = TechColors.AccentPurple,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = TechColors.Surface,
    onSecondaryContainer = TechColors.AccentPurple,
    tertiary = TechColors.AccentCyan,
    onTertiary = Color(0xFF000000),
    tertiaryContainer = TechColors.Surface,
    onTertiaryContainer = TechColors.AccentCyan,
    background = TechColors.Background,
    onBackground = TechColors.TextPrimary,
    surface = TechColors.Surface,
    onSurface = TechColors.TextPrimary,
    surfaceVariant = TechColors.SurfaceElevated,
    onSurfaceVariant = TechColors.TextSecondary,
    error = TechColors.AccentRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = TechColors.Surface,
    onErrorContainer = TechColors.AccentRed,
    outline = TechColors.Border,
    outlineVariant = TechColors.TextTertiary,
    scrim = TechColors.Overlay,
    inverseSurface = Color(0xFFE6F7FF),
    inverseOnSurface = Color(0xFF050B14),
    inversePrimary = TechColors.PrimaryLight,
    surfaceDim = TechColors.Background,
    surfaceBright = TechColors.SurfaceElevated,
    surfaceContainerLowest = TechColors.Background,
    surfaceContainerLow = TechColors.Surface,
    surfaceContainer = TechColors.SurfaceElevated,
    surfaceContainerHigh = TechColors.SurfaceElevated,
    surfaceContainerHighest = Color(0xFF243447),
)

/**
 * 明亮清新风格颜色方案 - 薄荷绿主题
 */
val FreshColorScheme = AppColorScheme(
    primary = FreshColors.Primary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = FreshColors.PrimaryLight.copy(alpha = 0.3f),
    onPrimaryContainer = FreshColors.PrimaryDark,
    secondary = FreshColors.AccentBlue,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = FreshColors.SurfaceElevated,
    onSecondaryContainer = FreshColors.AccentBlue,
    tertiary = FreshColors.AccentOrange,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = FreshColors.SurfaceElevated,
    onTertiaryContainer = FreshColors.AccentOrange,
    background = FreshColors.Background,
    onBackground = FreshColors.TextPrimary,
    surface = FreshColors.Surface,
    onSurface = FreshColors.TextPrimary,
    surfaceVariant = FreshColors.SurfaceElevated,
    onSurfaceVariant = FreshColors.TextSecondary,
    error = FreshColors.AccentRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = FreshColors.SurfaceElevated,
    onErrorContainer = FreshColors.AccentRed,
    outline = FreshColors.Border,
    outlineVariant = FreshColors.TextTertiary,
    scrim = FreshColors.Overlay,
    inverseSurface = FreshColors.TextPrimary,
    inverseOnSurface = FreshColors.Background,
    inversePrimary = FreshColors.PrimaryDark,
    surfaceDim = Color(0xFFE2E8F0),
    surfaceBright = FreshColors.Background,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = FreshColors.Surface,
    surfaceContainer = FreshColors.SurfaceElevated,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1),
)

/**
 * 应用主题
 *
 * @param themeType 主题类型
 * @param content 内容 composable
 */
@Composable
fun AISmartCameraTheme(
    themeType: ThemeType = ThemeType.PROFESSIONAL,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(themeType)

    // 设置状态栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            // 根据背景亮度设置状态栏图标颜色
            val isLightBackground = themeType == ThemeType.FRESH
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightBackground
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLightBackground
        }
    }

    // 应用颜色方案
    ThemeColors.setTheme(
        when (themeType) {
            ThemeType.PROFESSIONAL -> AppTheme.PROFESSIONAL
            ThemeType.TECH -> AppTheme.TECH
            ThemeType.FRESH -> AppTheme.FRESH
        }
    )

    MaterialTheme(
        colorScheme = when (themeType) {
            ThemeType.FRESH -> lightColorScheme(
                primary = colorScheme.primary,
                onPrimary = colorScheme.onPrimary,
                primaryContainer = colorScheme.primaryContainer,
                onPrimaryContainer = colorScheme.onPrimaryContainer,
                secondary = colorScheme.secondary,
                onSecondary = colorScheme.onSecondary,
                secondaryContainer = colorScheme.secondaryContainer,
                onSecondaryContainer = colorScheme.onSecondaryContainer,
                background = colorScheme.background,
                onBackground = colorScheme.onBackground,
                surface = colorScheme.surface,
                onSurface = colorScheme.onSurface,
                error = colorScheme.error,
                onError = colorScheme.onError,
                outline = colorScheme.outline,
            )
            else -> darkColorScheme(
                primary = colorScheme.primary,
                onPrimary = colorScheme.onPrimary,
                primaryContainer = colorScheme.primaryContainer,
                onPrimaryContainer = colorScheme.onPrimaryContainer,
                secondary = colorScheme.secondary,
                onSecondary = colorScheme.onSecondary,
                secondaryContainer = colorScheme.secondaryContainer,
                onSecondaryContainer = colorScheme.onSecondaryContainer,
                background = colorScheme.background,
                onBackground = colorScheme.onBackground,
                surface = colorScheme.surface,
                onSurface = colorScheme.onSurface,
                error = colorScheme.error,
                onError = colorScheme.onError,
                outline = colorScheme.outline,
            )
        },
        typography = Typography,
        content = content
    )
}

/**
 * 获取主题名称
 */
fun ThemeType.displayName(): String = when (this) {
    ThemeType.PROFESSIONAL -> "专业摄影"
    ThemeType.TECH -> "科技蓝"
    ThemeType.FRESH -> "明亮清新"
}

/**
 * 获取主题描述
 */
fun ThemeType.description(): String = when (this) {
    ThemeType.PROFESSIONAL -> "橙色强调配色，专业质感"
    ThemeType.TECH -> "霓虹青蓝配色，科技感十足"
    ThemeType.FRESH -> "薄荷绿配色，清新自然"
}
