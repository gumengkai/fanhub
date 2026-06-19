package com.fantok.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FantokColorScheme = darkColorScheme(
    primary            = PrimaryPink,
    onPrimary          = TextPrimary,
    secondary          = SecondaryCyan,
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
fun FantokTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FantokColorScheme,
        content = content
    )
}
