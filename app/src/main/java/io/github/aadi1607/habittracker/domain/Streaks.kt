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

    /** Monday of the ISO week containing [date]. */
    fun weekStart(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value - 1).toLong())

    /** Total logs per ISO week (keyed by that week's Monday). */
    fun weeklySums(countsByDate: Map<LocalDate, Int>): Map<LocalDate, Int> =
        countsByDate.entries
            .groupBy { weekStart(it.key) }
            .mapValues { (_, entries) -> entries.sumOf { it.value } }

    /**
     * Consecutive weeks (ending at the current one) whose total logs reached
     * [weeklyTarget]. Like the daily streak, a still-in-progress current week
     * doesn't break the run — it just doesn't count until the target is met.
     */
    fun currentWeeklyStreak(
        countsByDate: Map<LocalDate, Int>,
        weeklyTarget: Int,
        today: LocalDate,
    ): Int {
        val sums = weeklySums(countsByDate)
        var week = weekStart(today)
        var streak = 0
        if ((sums[week] ?: 0) >= weeklyTarget) streak++
        week = week.minusWeeks(1)
        while ((sums[week] ?: 0) >= weeklyTarget) {
            streak++
            week = week.minusWeeks(1)
        }
        return streak
    }

    /** Longest run of consecutive weeks whose total logs reached [weeklyTarget]. */
    fun bestWeeklyStreak(countsByDate: Map<LocalDate, Int>, weeklyTarget: Int): Int {
        val met = weeklySums(countsByDate).filterValues { it >= weeklyTarget }.keys
        if (met.isEmpty()) return 0
        val sorted = met.toSortedSet().toList()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1].plusWeeks(1)) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    /** Fraction of whole weeks since [createdOn] whose total logs met [weeklyTarget]. */
    fun weeklyCompletionRate(
        countsByDate: Map<LocalDate, Int>,
        weeklyTarget: Int,
        createdOn: LocalDate,
        today: LocalDate,
    ): Float {
        val start = weekStart(if (createdOn.isAfter(today)) today else createdOn)
        val weeks = (ChronoUnit.WEEKS.between(start, weekStart(today)) + 1).coerceAtLeast(1)
        val met = weeklySums(countsByDate).count { (week, sum) ->
            !week.isBefore(start) && sum >= weeklyTarget
        }
        return (met.toFloat() / weeks).coerceIn(0f, 1f)
    }

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
