package com.aicamera.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ============================================
 * AI 相机应用 - 主题配色方案
 * ============================================
 *
 * 包含三种主题风格：
 * 1. PROFESSIONAL - 专业摄影风格（橙色/琥珀色）
 * 2. TECH - 科技蓝风格
 * 3. FRESH - 明亮清新风格
 */

enum class AppTheme {
    PROFESSIONAL,   // 专业摄影风格
    TECH,           // 科技蓝风格
    FRESH           // 明亮清新风格
}

// ============================================
// 主题1: 专业摄影风格 (PROFESSIONAL)
// 特点: 橙色/琥珀色强调 + 深炭灰背景
// 适合: 专业摄影师，强调质感和专业度
// ============================================
object ProfessionalColors {
    // 主强调色 - 温暖的琥珀橙
    val Primary = Color(0xFFFF9500)           // 琥珀橙
    val PrimaryLight = Color(0xFFFFB347)      // 浅橙
    val PrimaryDark = Color(0xFFE68600)       // 深橙

    // 背景色 - 深炭灰色系
    val Background = Color(0xFF0D0D0D)        // 纯黑背景
    val Surface = Color(0xFF1C1C1E)           // 深炭灰表面
    val SurfaceElevated = Color(0xFF2C2C2E)   // 提升表面

    // 文字颜色
    val TextPrimary = Color(0xFFFFFFFF)       // 纯白文字
    val TextSecondary = Color(0xFFB0B0B0)     // 次要文字
    val TextTertiary = Color(0xFF8E8E93)      // 第三级文字

    // 功能色
    val AccentBlue = Color(0xFF5AC8FA)        // 信息/云端提示
    val AccentGreen = Color(0xFF34C759)       // 成功状态
    val AccentRed = Color(0xFFFF3B30)         // 错误/删除
    val AccentYellow = Color(0xFFFFCC00)      // 警告

    // 遮罩和边框
    val Overlay = Color(0x80000000)           // 50%黑色遮罩
    val Border = Color(0x33FFFFFF)            // 20%白色边框
    val GridLine = Color(0x80FF9500)          // 半透明橙色网格线
}

// ============================================
// 主题2: 科技蓝风格 (TECH)
// 特点: 霓虹蓝/青色强调 + 深蓝/黑色背景
// 适合: 科技爱好者，未来感设计
// ============================================
object TechColors {
    // 主强调色 - 霓虹蓝
    val Primary = Color(0xFF00D4AA)           // 霓虹青蓝
    val PrimaryLight = Color(0xFF5DFFE1)      // 浅青
    val PrimaryDark = Color(0xFF00A884)       // 深青

    // 背景色 - 深蓝黑色系
    val Background = Color(0xFF050B14)        // 深海蓝黑
    val Surface = Color(0xFF0F1720)           // 深蓝灰表面
    val SurfaceElevated = Color(0xFF1A2330)   // 提升表面

    // 文字颜色
    val TextPrimary = Color(0xFFE6F7FF)       // 冰蓝白文字
    val TextSecondary = Color(0xFF8BA3B8)     // 次要文字
    val TextTertiary = Color(0xFF5A7080)      // 第三级文字

    // 功能色
    val AccentPurple = Color(0xFF8B5CF6)      // 紫色 - 云端AI
    val AccentCyan = Color(0xFF06B6D4)        // 青色
    val AccentRed = Color(0xFFF43F5E)         // 玫瑰红错误
    val AccentYellow = Color(0xFFFBBF24)      // 琥珀警告

    // 遮罩和边框
    val Overlay = Color(0x90000000)           // 深色遮罩
    val Border = Color(0x4000D4AA)            // 青色发光边框
    val GridLine = Color(0x6000D4AA)          // 半透明青色网格线

    // 特殊效果色
    val Glow = Color(0x4000D4AA)              // 发光效果
    val Neon = Color(0xFF00FFCC)              // 霓虹高亮
}

// ============================================
// 主题3: 明亮清新风格 (FRESH)
// 特点: 薄荷绿/青色强调 + 白色背景
// 适合: 日常用户，清新自然风格
// ============================================
object FreshColors {
    // 主强调色 - 薄荷绿
    val Primary = Color(0xFF10B981)           // 薄荷绿
    val PrimaryLight = Color(0xFF6EE7B7)      // 浅薄荷
    val PrimaryDark = Color(0xFF059669)       // 深薄荷

    // 背景色 - 明亮色系
    val Background = Color(0xFFFFFFFF)        // 纯白背景
    val Surface = Color(0xFFF8FAFC)           // 浅灰表面
    val SurfaceElevated = Color(0xFFF1F5F9)   // 提升表面

    // 文字颜色
    val TextPrimary = Color(0xFF0F172A)       // 深 slate 文字
    val TextSecondary = Color(0xFF64748B)     // 次要文字
    val TextTertiary = Color(0xFF94A3B8)      // 第三级文字

    // 功能色
    val AccentBlue = Color(0xFF3B82F6)        // 天蓝
    val AccentPurple = Color(0xFF8B5CF6)      // 紫色
    val AccentRed = Color(0xFFEF4444)         // 红色错误
    val AccentYellow = Color(0xFFF59E0B)      // 琥珀警告
    val AccentOrange = Color(0xFFF97316)      // 活力橙

    // 遮罩和边框
    val Overlay = Color(0x40000000)           // 25%黑色遮罩
    val Border = Color(0xFFE2E8F0)            // 浅灰边框
    val GridLine = Color(0x4010B981)          // 半透明绿色网格线

    // 卡片和分隔线
    val Divider = Color(0xFFE2E8F0)           // 分隔线
    val Card = Color(0xFFFFFFFF)              // 卡片背景
}

// ============================================
// 统一颜色获取函数
// 根据当前主题返回对应颜色
// ============================================
object ThemeColors {
    private var currentTheme: AppTheme = AppTheme.PROFESSIONAL

    fun setTheme(theme: AppTheme) {
        currentTheme = theme
    }

    fun getTheme(): AppTheme = currentTheme

    // 主强调色
    val Primary: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.Primary
            AppTheme.TECH -> TechColors.Primary
            AppTheme.FRESH -> FreshColors.Primary
        }

    val PrimaryLight: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.PrimaryLight
            AppTheme.TECH -> TechColors.PrimaryLight
            AppTheme.FRESH -> FreshColors.PrimaryLight
        }

    val PrimaryDark: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.PrimaryDark
            AppTheme.TECH -> TechColors.PrimaryDark
            AppTheme.FRESH -> FreshColors.PrimaryDark
        }

    // 背景色
    val Background: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.Background
            AppTheme.TECH -> TechColors.Background
            AppTheme.FRESH -> FreshColors.Background
        }

    val Surface: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.Surface
            AppTheme.TECH -> TechColors.Surface
            AppTheme.FRESH -> FreshColors.Surface
        }

    val SurfaceElevated: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.SurfaceElevated
            AppTheme.TECH -> TechColors.SurfaceElevated
            AppTheme.FRESH -> FreshColors.SurfaceElevated
        }

    // 文字颜色
    val TextPrimary: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.TextPrimary
            AppTheme.TECH -> TechColors.TextPrimary
            AppTheme.FRESH -> FreshColors.TextPrimary
        }

    val TextSecondary: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.TextSecondary
            AppTheme.TECH -> TechColors.TextSecondary
            AppTheme.FRESH -> FreshColors.TextSecondary
        }

    val TextTertiary: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.TextTertiary
            AppTheme.TECH -> TechColors.TextTertiary
            AppTheme.FRESH -> FreshColors.TextTertiary
        }

    // 功能色
    val AccentBlue: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.AccentBlue
            AppTheme.TECH -> TechColors.AccentCyan
            AppTheme.FRESH -> FreshColors.AccentBlue
        }

    val AccentPurple: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.AccentBlue
            AppTheme.TECH -> TechColors.AccentPurple
            AppTheme.FRESH -> FreshColors.AccentPurple
        }

    val AccentGreen: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.AccentGreen
            AppTheme.TECH -> TechColors.Primary
            AppTheme.FRESH -> FreshColors.Primary
        }

    val AccentRed: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.AccentRed
            AppTheme.TECH -> TechColors.AccentRed
            AppTheme.FRESH -> FreshColors.AccentRed
        }

    val AccentYellow: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.AccentYellow
            AppTheme.TECH -> TechColors.AccentYellow
            AppTheme.FRESH -> FreshColors.AccentYellow
        }

    // 遮罩和边框
    val Overlay: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.Overlay
            AppTheme.TECH -> TechColors.Overlay
            AppTheme.FRESH -> FreshColors.Overlay
        }

    val Border: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.Border
            AppTheme.TECH -> TechColors.Border
            AppTheme.FRESH -> FreshColors.Border
        }

    val GridLine: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.GridLine
            AppTheme.TECH -> TechColors.GridLine
            AppTheme.FRESH -> FreshColors.GridLine
        }

    // 特殊
    val Glow: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.Overlay
            AppTheme.TECH -> TechColors.Glow
            AppTheme.FRESH -> FreshColors.Overlay
        }

    val Neon: Color
        get() = when (currentTheme) {
            AppTheme.PROFESSIONAL -> ProfessionalColors.Primary
            AppTheme.TECH -> TechColors.Neon
            AppTheme.FRESH -> FreshColors.Primary
        }
}

// ============================================
// 向后兼容的旧颜色定义
// 使用 Professional 主题作为默认
// ============================================

// 主绿色（保持向后兼容）
val PrimaryGreen = Color(0xFFFF9500)           // 更新为琥珀橙

// 深色主题颜色
val DarkBackground = Color(0xFF0D0D0D)         // 深黑背景
val SurfaceDark = Color(0xFF1C1C1E)            // 深灰表面
val SurfaceLight = Color(0xFFF8FAFC)           // 浅灰表面（兼容）
val OverlayDark = Color(0x80000000)            // 深色遮罩
val OverlayDark80 = Color(0xCC000000)          // 80%深色遮罩

// 文字颜色
val TextWhite = Color(0xFFFFFFFF)              // 白色文字
val TextGray = Color(0xFFB0B0B0)               // 灰色文字
val TextDark = Color(0xFF0F172A)               // 深色文字
val TextLightGray = Color(0xFF8E8E93)          // 浅灰文字

// 滑块颜色
val SliderInactive = Color(0xFF3A3A3C)         // 滑块未激活轨道

// AI 提示气泡颜色（兼容）
val AiTipCloud = Color(0xFF8B5CF6)             // 云端提示 - 紫色
val AiTipLocal = Color(0xFFFF9500)             // 本地提示 - 琥珀橙
