package io.github.aadi1607.tiffintracker.domain

import java.time.LocalDate

/**
 * A billing cycle that starts on a configurable day of the month (1..28)
 * and runs until the day before the next cycle starts. Restricting the
 * start day to 28 means every month has the start day, so no month-length
 * clamping is ever needed and cycle boundaries are unambiguous.
 */
data class BillingCycle(val start: LocalDate, val end: LocalDate) {
    val startEpochDay: Long get() = start.toEpochDay()
    val endEpochDay: Long get() = end.toEpochDay()

    fun previous(): BillingCycle = of(start.minusMonths(1))
    fun next(): BillingCycle = of(start.plusMonths(1))

    operator fun contains(date: LocalDate): Boolean = date in start..end

    private fun of(newStart: LocalDate) =
        BillingCycle(newStart, newStart.plusMonths(1).minusDays(1))

    companion object {
        const val MIN_START_DAY = 1
        const val MAX_START_DAY = 28

        /** The cycle containing [date] for a cycle that begins on [startDay] each month. */
        fun cycleFor(date: LocalDate, startDay: Int): BillingCycle {
            val day = startDay.coerceIn(MIN_START_DAY, MAX_START_DAY)
            val candidate = date.withDayOfMonth(day)
            val start = if (date >= candidate) candidate else candidate.minusMonths(1)
            return BillingCycle(start, start.plusMonths(1).minusDays(1))
        }
    }
}
