package io.github.aadi1607.tiffintracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreaksTest {

    private val today = LocalDate.of(2026, 7, 16)

    private fun days(vararg dates: LocalDate): Set<Long> =
        dates.map { it.toEpochDay() }.toSet()

    @Test
    fun `empty history has zero streak`() {
        assertEquals(0, Streaks.currentStreak(emptySet(), today))
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        val taken = days(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, Streaks.currentStreak(taken, today))
    }

    @Test
    fun `unlogged today does not break the streak`() {
        val taken = days(today.minusDays(1), today.minusDays(2))
        assertEquals(2, Streaks.currentStreak(taken, today))
    }

    @Test
    fun `gap breaks the streak`() {
        val taken = days(today, today.minusDays(2), today.minusDays(3))
        assertEquals(1, Streaks.currentStreak(taken, today))
    }

    @Test
    fun `gap two days ago means zero when nothing recent`() {
        val taken = days(today.minusDays(3), today.minusDays(4))
        assertEquals(0, Streaks.currentStreak(taken, today))
    }
}
