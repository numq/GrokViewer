package io.github.numq.grokviewer.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkGrey = Color(0xFF1A1A1A)

private val SurfaceGrey = Color(0xFF242424)

private val BorderGrey = Color(0xFF383838)

private val TextPrimary = Color(0xFFE2E2E2)

private val TextSecondary = Color(0xFFA0A0A0)

private val GrokOrange = Color(0xFFFF5C00)

private val LightColors = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color.Black,
    background = Color(0xFFF9F9F9),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFFD1D1D1)
)

private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = Color.White,
    secondary = GrokOrange,
    onSecondary = Color.Black,
    background = DarkGrey,
    onBackground = TextPrimary,
    surface = DarkGrey,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGrey,
    onSurfaceVariant = TextSecondary,
    outline = BorderGrey,
    error = Color(0xFFCF6679)
)

@Composable
fun Theme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}