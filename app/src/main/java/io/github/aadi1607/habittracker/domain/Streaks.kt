package io.github.aadi1607.habittracker.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pure date math for streaks and history. Kept free of Android types so it is
 * trivially unit-testable on the JVM.
 */
object Streaks {

    /**
     * Number of consecutive completed days ending today or yesterday.
     *
     * A pending (not yet completed) today does not break the streak: the run is
     * counted from yesterday backwards. Only once midnight passes with the day
     * still incomplete does the streak reset.
     */
    fun currentStreak(completed: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in completed) today else today.minusDays(1)
        var streak = 0
        while (day in completed) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    /** Longest run of consecutive completed days in the whole history. */
    fun bestStreak(completed: Collection<LocalDate>): Int {
        if (completed.isEmpty()) return 0
        val sorted = completed.toSortedSet().toList()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1].plusDays(1)) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    /** The last [n] days ending at [today], oldest first. */
    fun lastDays(today: LocalDate, n: Int): List<LocalDate> =
        List(n) { today.minusDays((n - 1 - it).toLong()) }

    /**
     * Completion rate as a fraction in 0..1: completed days divided by days the
     * habit has existed (from [createdOn] through [today], inclusive).
     */
    fun completionRate(completed: Set<LocalDate>, createdOn: LocalDate, today: LocalDate): Float {
        val start = if (createdOn.isAfter(today)) today else createdOn
        val days = ChronoUnit.DAYS.between(start, today) + 1
        val done = completed.count { !it.isBefore(start) && !it.isAfter(today) }
        return (done.toFloat() / days).coerceIn(0f, 1f)
    }
}
