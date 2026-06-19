package com.fanpeak.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── FanPeak 红色主题色系 ──────────────────────────
val Background        = Color(0xFF1A1A1A)   // 深灰背景（比纯黑柔和）
val BackgroundSurface = Color(0xFF2D2D2D)   // 卡片背景
val BackgroundCard    = Color(0xFF3D3D3D)   // 更深卡片背景
val PrimaryRed        = Color(0xFFE53935)   // 主红色
val PrimaryRedDark    = Color(0xFFB71C1C)   // 深红色
val PrimaryRedLight   = Color(0xFFEF5350)   // 浅红色
val SecondaryGold     = Color(0xFFFFB300)   // 金色（备用）
val SecondaryOrange   = Color(0xFFFF8F50)   // 橙色（备用）
val SecondaryCyan     = Color(0xFF25F4EE)   // 青色（备用）
val BorderColor       = Color(0xFF404040)   // 边框色
val TextPrimary       = Color(0xFFFFFFFF)   // 白色文字
val TextSecondary     = Color(0xFFBDBDBD)   // 灰色文字
val TextTertiary      = Color(0xFF9E9E9E)   // 更深灰色
val ErrorRed          = Color(0xFFFF1744)   // 错误红
val GoldStar          = Color(0xFFFFD700)   // 金色星星

// 常用透明变体
val PrimaryRed20      = Color(0x33E53935)
val PrimaryRed10      = Color(0x1AE53935)
val GlassSurface      = Color(0x1AFFFFFF)

// 渐变（红色主题）
val GradientBrand = Brush.linearGradient(listOf(PrimaryRed, PrimaryRedLight))
