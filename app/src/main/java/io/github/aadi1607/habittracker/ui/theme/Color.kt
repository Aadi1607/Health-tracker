package io.github.aadi1607.habittracker.ui.theme

import androidx.compose.ui.graphics.Color

// Deep ink-blue backdrop with soft raised cards.
val Ink = Color(0xFF14161F)
val CardBlue = Color(0xFF1C1F2B)
val CardBlueHigh = Color(0xFF242838)
val OnInk = Color(0xFFE6E8F0)
val OnInkMuted = Color(0xFF9BA0B4)
val AccentBlue = Color(0xFF8AB4FF)
val DotEmpty = Color(0xFF2A2E3D)

// Preset palette for habit colors (packed ARGB, stored in the database).
val HabitPalette: List<Long> = listOf(
    0xFFFF6B6B, // coral
    0xFFFFB74D, // amber
    0xFF34D399, // emerald
    0xFF4FC3F7, // sky
    0xFFA78BFA, // violet
    0xFFF472B6, // pink
)
