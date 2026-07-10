package uk.co.wsjty.remote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val WsjtyColorScheme = darkColorScheme(
    primary = WsjtyAccent,
    onPrimary = WsjtyBackground,
    secondary = WsjtyAccentDim,
    background = WsjtyBackground,
    surface = WsjtySurface,
    onBackground = WsjtyTextPrimary,
    onSurface = WsjtyTextPrimary,
    error = WsjtyRed,
    outline = WsjtyBorder,
)

@Composable
fun WSJTYRemoteTheme(content: @Composable () -> Unit) {
    // Always dark — matches the desktop app's own theme, and this is a
    // shack-use tool, not something that needs to follow system light mode.
    MaterialTheme(
        colorScheme = WsjtyColorScheme,
        content = content,
    )
}
