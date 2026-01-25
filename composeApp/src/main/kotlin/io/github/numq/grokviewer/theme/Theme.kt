package io.github.numq.grokviewer.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GrokBlack = Color(0xFF000000)

private val GrokDarkGrey = Color(0xFF121212)

private val GrokWhite = Color(0xFFFFFFFF)

private val GrokAccent = Color(0xFFE5E5E5)

private val GrokHighlight = Color(0xFFFF5C00)

private val LightColors = lightColorScheme(
    primary = GrokBlack,
    onPrimary = GrokWhite,
    secondary = GrokDarkGrey,
    onSecondary = GrokWhite,
    background = GrokWhite,
    onBackground = GrokBlack,
    surface = Color(0xFFF5F5F5),
    onSurface = GrokBlack,
    outline = Color(0xFF8E8E8E)
)

private val DarkColors = darkColorScheme(
    primary = GrokWhite,
    onPrimary = GrokBlack,
    secondary = GrokAccent,
    onSecondary = GrokBlack,
    background = GrokBlack,
    onBackground = GrokWhite,
    surface = GrokDarkGrey,
    onSurface = GrokWhite,
    outline = Color(0xFF333333),
    tertiary = GrokHighlight
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