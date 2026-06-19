package com.fantok.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Fantok TikTok 风格色系 ──────────────────────────
val Background        = Color(0xFF000000)   // 纯黑背景
val BackgroundSurface = Color(0xFF161823)   // 抖音深色背景
val BackgroundCard    = Color(0xFF232533)   // 卡片背景
val PrimaryPink       = Color(0xFFFE2C55)   // TikTok 红
val SecondaryCyan     = Color(0xFF25F4EE)   // TikTok 青
val SecondaryOrange   = Color(0xFFFF8F50)   // 橙色（备用）
val BorderColor       = Color(0xFF2A2A3A)   // 边框色
val TextPrimary       = Color(0xFFFFFFFF)   // 白色文字
val TextSecondary     = Color(0xFF8A8B91)   // 灰色文字
val TextTertiary      = Color(0xFF6C6C80)   // 更深灰色
val ErrorRed          = Color(0xFFFF4D4F)   // 错误红
val GoldStar          = Color(0xFFFFD700)   // 金色星星

// 常用透明变体
val PrimaryPink20     = Color(0x33FE2C55)
val PrimaryPink10     = Color(0x1AFE2C55)
val GlassSurface      = Color(0x1AFFFFFF)

// 渐变（TikTok 红 → 青）
val GradientBrand = Brush.linearGradient(listOf(PrimaryPink, SecondaryCyan))
