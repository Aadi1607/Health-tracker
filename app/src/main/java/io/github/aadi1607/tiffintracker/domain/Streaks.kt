package io.github.aadi1607.tiffintracker.domain

import java.time.LocalDate

object Streaks {
    /**
     * Consecutive days (ending today, or yesterday if today isn't logged yet)
     * on which at least one tiffin was taken. A skipped or unlogged day in
     * between breaks the streak.
     */
    fun currentStreak(takenEpochDays: Set<Long>, today: LocalDate): Int {
        var day = today.toEpochDay()
        if (day !in takenEpochDays) day-- // today may simply not be logged yet
        var streak = 0
        while (day in takenEpochDays) {
            streak++
            day--
        }
        return streak
    }
}
