package io.github.aadi1607.tiffintracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.aadi1607.tiffintracker.data.ThemeMode

/** Preset color tags assignable to users. */
object UserColors {
    val palette: List<Long> = listOf(
        0xFF1E88E5, // blue
        0xFFE53935, // red
        0xFF43A047, // green
        0xFF8E24AA, // purple
        0xFFF4511E, // deep orange
        0xFF00897B, // teal
        0xFF6D4C41, // brown
        0xFF3949AB, // indigo
    )
}

// Status colors shared by calendar dots, chips, and legends.
val StatusTaken = Color(0xFF2E7D32)
val StatusSkipped = Color(0xFF9E9E9E)
val StatusPending = Color(0xFFEF6C00)

private val LightColors = lightColorScheme(
    primary = Color(0xFFBF4D00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF3E1500),
    secondary = Color(0xFF755746),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBC8),
    onSecondaryContainer = Color(0xFF2B160A),
    tertiary = Color(0xFF616032),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A15),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF4DED3),
    onSurfaceVariant = Color(0xFF52443C),
    outline = Color(0xFF85736A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68E),
    onPrimary = Color(0xFF552000),
    primaryContainer = Color(0xFF793100),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFE5BEA9),
    onSecondary = Color(0xFF422B1C),
    secondaryContainer = Color(0xFF5B4130),
    onSecondaryContainer = Color(0xFFFFDBC8),
    tertiary = Color(0xFFCBC990),
    background = Color(0xFF1A120D),
    onBackground = Color(0xFFF0DED5),
    surface = Color(0xFF1A120D),
    onSurface = Color(0xFFF0DED5),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C2B8),
    outline = Color(0xFF9F8D83),
)

@Composable
fun TiffinTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
