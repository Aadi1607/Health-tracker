package io.github.aadi1607.habittracker.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val InkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Ink,
    secondary = AccentBlue,
    onSecondary = Ink,
    background = Ink,
    onBackground = OnInk,
    surface = Ink,
    onSurface = OnInk,
    surfaceVariant = CardBlueHigh,
    onSurfaceVariant = OnInkMuted,
    surfaceContainerLowest = CardBlue,
    surfaceContainerLow = CardBlue,
    surfaceContainer = CardBlue,
    surfaceContainerHigh = CardBlueHigh,
    surfaceContainerHighest = CardBlueHigh,
    outline = OnInkMuted,
)

@Composable
fun HabitTrackerTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        InkColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
