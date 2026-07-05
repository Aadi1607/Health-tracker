package io.github.aadi1607.habittracker.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreaksTest {

    private val today: LocalDate = LocalDate.of(2026, 7, 5)

    private fun days(vararg offsets: Long): Set<LocalDate> =
        offsets.map { today.minusDays(it) }.toSet()

    @Test
    fun `empty history has no streak`() {
        assertEquals(0, Streaks.currentStreak(emptySet(), today))
    }

    @Test
    fun `streak counts today when completed`() {
        assertEquals(3, Streaks.currentStreak(days(0, 1, 2), today))
    }

    @Test
    fun `pending today does not break streak`() {
        // Completed yesterday and the day before, nothing yet today.
        assertEquals(2, Streaks.currentStreak(days(1, 2), today))
    }

    @Test
    fun `streak resets after a missed day`() {
        // Gap two days ago: only yesterday counts.
        assertEquals(1, Streaks.currentStreak(days(1, 3, 4), today))
    }

    @Test
    fun `streak broken when neither today nor yesterday completed`() {
        assertEquals(0, Streaks.currentStreak(days(2, 3), today))
    }

    @Test
    fun `completing today extends yesterday's run`() {
        val completed = days(0, 1, 2, 3, 4)
        assertEquals(5, Streaks.currentStreak(completed, today))
    }

    @Test
    fun `midnight rollover breaks a pending streak`() {
        val completed = days(1, 2) // streak of 2 while today is pending
        assertEquals(2, Streaks.currentStreak(completed, today))
        // At the next midnight "today" moves forward; the missed day breaks the run.
        assertEquals(0, Streaks.currentStreak(completed, today.plusDays(1)))
    }

    @Test
    fun `best streak finds longest run in history`() {
        val completed = days(0, 1, 2, 5, 6, 7, 8, 12)
        assertEquals(4, Streaks.bestStreak(completed))
    }

    @Test
    fun `best streak of empty history is zero`() {
        assertEquals(0, Streaks.bestStreak(emptyList()))
    }

    @Test
    fun `best streak handles duplicates`() {
        val withDuplicates = listOf(today, today, today.minusDays(1))
        assertEquals(2, Streaks.bestStreak(withDuplicates))
    }

    @Test
    fun `lastDays returns oldest first ending today`() {
        val week = Streaks.lastDays(today, 7)
        assertEquals(7, week.size)
        assertEquals(today.minusDays(6), week.first())
        assertEquals(today, week.last())
    }

    @Test
    fun `completion rate over habit lifetime`() {
        val createdOn = today.minusDays(9) // 10 days of existence
        val completed = days(0, 1, 2, 3) // 4 completed
        assertEquals(0.4f, Streaks.completionRate(completed, createdOn, today), 0.0001f)
    }

    @Test
    fun `completion rate ignores days before creation`() {
        val createdOn = today.minusDays(1) // 2 days of existence
        val completed = days(0, 1, 5, 6) // old completions outside window
        assertEquals(1f, Streaks.completionRate(completed, createdOn, today), 0.0001f)
    }

    @Test
    fun `completion rate handles creation today`() {
        assertEquals(0f, Streaks.completionRate(emptySet(), today, today), 0.0001f)
        assertEquals(1f, Streaks.completionRate(days(0), today, today), 0.0001f)
    }
}
