package io.github.aadi1607.tiffintracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BillingCycleTest {

    @Test
    fun `calendar month cycle when start day is 1`() {
        val cycle = BillingCycle.cycleFor(LocalDate.of(2026, 7, 16), startDay = 1)
        assertEquals(LocalDate.of(2026, 7, 1), cycle.start)
        assertEquals(LocalDate.of(2026, 7, 31), cycle.end)
    }

    @Test
    fun `date on the start day begins a new cycle`() {
        val cycle = BillingCycle.cycleFor(LocalDate.of(2026, 7, 5), startDay = 5)
        assertEquals(LocalDate.of(2026, 7, 5), cycle.start)
        assertEquals(LocalDate.of(2026, 8, 4), cycle.end)
    }

    @Test
    fun `date before the start day belongs to the previous month's cycle`() {
        val cycle = BillingCycle.cycleFor(LocalDate.of(2026, 7, 4), startDay = 5)
        assertEquals(LocalDate.of(2026, 6, 5), cycle.start)
        assertEquals(LocalDate.of(2026, 7, 4), cycle.end)
    }

    @Test
    fun `cycle spanning february handles short month`() {
        val cycle = BillingCycle.cycleFor(LocalDate.of(2026, 2, 10), startDay = 28)
        assertEquals(LocalDate.of(2026, 1, 28), cycle.start)
        assertEquals(LocalDate.of(2026, 2, 27), cycle.end)
    }

    @Test
    fun `cycle starting in february of a leap year`() {
        val cycle = BillingCycle.cycleFor(LocalDate.of(2028, 2, 28), startDay = 28)
        assertEquals(LocalDate.of(2028, 2, 28), cycle.start)
        assertEquals(LocalDate.of(2028, 3, 27), cycle.end)
    }

    @Test
    fun `start day out of range is clamped to 28`() {
        val cycle = BillingCycle.cycleFor(LocalDate.of(2026, 3, 30), startDay = 31)
        assertEquals(LocalDate.of(2026, 3, 28), cycle.start)
    }

    @Test
    fun `previous and next cycles are adjacent without gaps`() {
        val cycle = BillingCycle.cycleFor(LocalDate.of(2026, 7, 16), startDay = 5)
        assertEquals(cycle.start.minusDays(1), cycle.previous().end)
        assertEquals(cycle.end.plusDays(1), cycle.next().start)
    }

    @Test
    fun `every date maps into exactly one cycle across a year boundary`() {
        var date = LocalDate.of(2025, 12, 1)
        while (date <= LocalDate.of(2026, 2, 1)) {
            val cycle = BillingCycle.cycleFor(date, startDay = 5)
            assert(date in cycle) { "$date not inside its own cycle $cycle" }
            date = date.plusDays(1)
        }
    }
}
