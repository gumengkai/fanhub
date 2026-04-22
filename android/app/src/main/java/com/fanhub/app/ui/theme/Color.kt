package com.fanhub.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── FanHub 品牌色系（与 Web 端 CSS 变量对齐）──────────────────────────
val Background        = Color(0xFF0A0A0F)   // --color-background
val BackgroundSurface = Color(0xFF14141A)   // --color-background-secondary
val BackgroundCard    = Color(0xFF1F1F2A)   // --color-background-tertiary
val PrimaryPink       = Color(0xFFFB7299)   // --color-primary
val SecondaryOrange   = Color(0xFFFF8F50)   // --color-secondary
val BorderColor       = Color(0xFF2A2A3A)   // --color-border
val TextPrimary       = Color(0xFFFFFFFF)
val TextSecondary     = Color(0xFFA0A0B0)   // --color-foreground-secondary
val TextTertiary      = Color(0xFF6C6C80)   // --color-foreground-tertiary
val ErrorRed          = Color(0xFFFF4D4F)
val GoldStar          = Color(0xFFFAAD14)   // 收藏星星颜色（金色）

// 常用透明变体
val PrimaryPink20     = Color(0x33FB7299)
val PrimaryPink10     = Color(0x1AFB7299)
val GlassSurface      = Color(0x1AFFFFFF)

// 渐变（品牌粉 → 橙）
val GradientBrand = Brush.linearGradient(listOf(PrimaryPink, SecondaryOrange))
