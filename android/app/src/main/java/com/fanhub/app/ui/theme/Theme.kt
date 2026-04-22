package com.fanhub.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FanHubColorScheme = darkColorScheme(
    primary            = PrimaryPink,
    onPrimary          = TextPrimary,
    secondary          = SecondaryOrange,
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
fun FanHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FanHubColorScheme,
        content = content
    )
}
