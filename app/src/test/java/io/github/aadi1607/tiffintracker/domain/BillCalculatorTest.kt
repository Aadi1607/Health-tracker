package io.github.aadi1607.tiffintracker.domain

import io.github.aadi1607.tiffintracker.data.db.EntryStatus
import io.github.aadi1607.tiffintracker.data.db.MealType
import io.github.aadi1607.tiffintracker.data.db.Payment
import io.github.aadi1607.tiffintracker.data.db.TiffinEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BillCalculatorTest {

    private val userId = 1L
    private val pricePaise = 6000L // ₹60

    private fun entry(
        date: LocalDate,
        meal: MealType = MealType.LUNCH,
        quantity: Int = 1,
        status: EntryStatus = EntryStatus.TAKEN,
        user: Long = userId,
    ) = TiffinEntry(
        userId = user,
        epochDay = date.toEpochDay(),
        mealType = meal,
        quantity = quantity,
        status = status,
    )

    private val julyCycle = BillingCycle.cycleFor(LocalDate.of(2026, 7, 10), startDay = 5)

    @Test
    fun `tiffin count sums quantities of taken entries only`() {
        val entries = listOf(
            entry(LocalDate.of(2026, 7, 10)),
            entry(LocalDate.of(2026, 7, 10), MealType.DINNER),
            entry(LocalDate.of(2026, 7, 11), MealType.EXTRA, quantity = 2),
            entry(LocalDate.of(2026, 7, 12), status = EntryStatus.SKIPPED),
        )
        assertEquals(4, BillCalculator.tiffinCount(entries))
    }

    @Test
    fun `bill is count times price`() {
        val entries = listOf(
            entry(LocalDate.of(2026, 7, 10)),
            entry(LocalDate.of(2026, 7, 10), MealType.DINNER),
        )
        assertEquals(12000L, BillCalculator.billPaise(entries, pricePaise))
    }

    @Test
    fun `user bill only counts entries inside the cycle`() {
        val entries = listOf(
            entry(LocalDate.of(2026, 7, 4)), // previous cycle
            entry(LocalDate.of(2026, 7, 5)), // first day of cycle
            entry(LocalDate.of(2026, 8, 4)), // last day of cycle
            entry(LocalDate.of(2026, 8, 5)), // next cycle
        )
        val bill = BillCalculator.userBill(userId, pricePaise, julyCycle, entries, emptyList())
        assertEquals(2, bill.tiffinCount)
        assertEquals(12000L, bill.billPaise)
    }

    @Test
    fun `entries of other users are ignored`() {
        val entries = listOf(
            entry(LocalDate.of(2026, 7, 10)),
            entry(LocalDate.of(2026, 7, 10), user = 2L),
        )
        val bill = BillCalculator.userBill(userId, pricePaise, julyCycle, entries, emptyList())
        assertEquals(1, bill.tiffinCount)
    }

    @Test
    fun `carry forward is unpaid balance from earlier cycles`() {
        val entries = listOf(
            entry(LocalDate.of(2026, 6, 10)), // previous cycle: bill 60
            entry(LocalDate.of(2026, 7, 10)), // this cycle: bill 60
        )
        val payments = listOf(
            Payment(
                userId = userId,
                cycleStartEpochDay = julyCycle.previous().startEpochDay,
                amountPaise = 2000,
                epochDay = LocalDate.of(2026, 7, 6).toEpochDay(),
            )
        )
        val bill = BillCalculator.userBill(userId, pricePaise, julyCycle, entries, payments)
        assertEquals(4000L, bill.carryForwardPaise) // 6000 billed - 2000 paid
        assertEquals(6000L, bill.billPaise)
        assertEquals(10000L, bill.totalDuePaise)
    }

    @Test
    fun `status is pending when nothing paid`() {
        val entries = listOf(entry(LocalDate.of(2026, 7, 10)))
        val bill = BillCalculator.userBill(userId, pricePaise, julyCycle, entries, emptyList())
        assertEquals(BillStatus.PENDING, bill.status)
    }

    @Test
    fun `status is partial when some paid`() {
        val entries = listOf(entry(LocalDate.of(2026, 7, 10)))
        val payments = listOf(
            Payment(
                userId = userId,
                cycleStartEpochDay = julyCycle.startEpochDay,
                amountPaise = 3000,
                epochDay = LocalDate.of(2026, 7, 11).toEpochDay(),
            )
        )
        val bill = BillCalculator.userBill(userId, pricePaise, julyCycle, entries, payments)
        assertEquals(BillStatus.PARTIAL, bill.status)
        assertEquals(3000L, bill.balancePaise)
    }

    @Test
    fun `status is paid when fully paid`() {
        val entries = listOf(entry(LocalDate.of(2026, 7, 10)))
        val payments = listOf(
            Payment(
                userId = userId,
                cycleStartEpochDay = julyCycle.startEpochDay,
                amountPaise = 6000,
                epochDay = LocalDate.of(2026, 7, 11).toEpochDay(),
            )
        )
        val bill = BillCalculator.userBill(userId, pricePaise, julyCycle, entries, payments)
        assertEquals(BillStatus.PAID, bill.status)
        assertEquals(0L, bill.totalDuePaise)
    }

    @Test
    fun `empty cycle with no bill counts as paid`() {
        val bill = BillCalculator.userBill(userId, pricePaise, julyCycle, emptyList(), emptyList())
        assertEquals(BillStatus.PAID, bill.status)
        assertEquals(0L, bill.totalDuePaise)
    }

    @Test
    fun `format paise handles whole and fractional rupees`() {
        assertEquals("₹60", BillCalculator.formatPaise(6000))
        assertEquals("₹62.50", BillCalculator.formatPaise(6250))
        assertEquals("₹1,240", BillCalculator.formatPaise(124000))
        assertEquals("-₹15", BillCalculator.formatPaise(-1500))
    }
}
