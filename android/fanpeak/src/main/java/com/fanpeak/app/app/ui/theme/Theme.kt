package com.fanpeak.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FanPeakColorScheme = darkColorScheme(
    primary            = PrimaryRed,
    onPrimary          = TextPrimary,
    secondary          = SecondaryGold,
    onSecondary        = TextPrimary,
    background         = Background,
    onBackground       = TextPrimary,
    surface            = BackgroundSurface,
    onSurface          = TextPrimary,
    surfaceVariant     = BackgroundCard,
    onSurfaceVariant   = TextSecondary,
    outline            = BorderColor,
    error              = ErrorRed,
)

@Composable
fun FanPeakTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FanPeakColorScheme,
        content = content
    )
}
