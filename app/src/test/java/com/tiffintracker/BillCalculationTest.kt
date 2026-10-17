package com.tiffintracker

import com.tiffintracker.data.db.MealEntryDao
import com.tiffintracker.data.db.MealStatus
import com.tiffintracker.data.db.MealType
import com.tiffintracker.data.db.PaymentDao
import com.tiffintracker.data.db.Provider
import com.tiffintracker.data.db.ProviderDao
import com.tiffintracker.data.repository.TiffinRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BillCalculationTest {

    private lateinit var providerDao: ProviderDao
    private lateinit var mealEntryDao: MealEntryDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var repository: TiffinRepository

    private val testProvider = Provider(
        id = 1L,
        name = "Test Kitchen",
        phone = "9876543210",
        breakfastPrice = 50.0,
        lunchPrice = 80.0,
        dinnerPrice = 70.0,
        offersBreakfast = true,
        offersLunch = true,
        offersDinner = true,
        billingCycleStartDay = 1
    )

    @BeforeEach
    fun setup() {
        providerDao = mock()
        mealEntryDao = mock()
        paymentDao = mock()
        repository = TiffinRepository(providerDao, mealEntryDao, paymentDao)
    }

    @Nested
    @DisplayName("Bill calculation")
    inner class CalculateBill {

        @Test
        @DisplayName("calculates total for all meal types with correct quantities")
        fun fullMonthAllMeals() = runTest {
            whenever(providerDao.getProviderByIdOnce(1L)).thenReturn(testProvider)
            whenever(
                mealEntryDao.getTotalQuantityForProviderInRange(
                    eq(1L), any(), any(), eq(MealType.BREAKFAST.name)
                )
            ).thenReturn(25)
            whenever(
                mealEntryDao.getTotalQuantityForProviderInRange(
                    eq(1L), any(), any(), eq(MealType.LUNCH.name)
                )
            ).thenReturn(28)
            whenever(
                mealEntryDao.getTotalQuantityForProviderInRange(
                    eq(1L), any(), any(), eq(MealType.DINNER.name)
                )
            ).thenReturn(20)
            whenever(
                paymentDao.getTotalPaidForProviderInRange(eq(1L), any(), any())
            ).thenReturn(2000.0)

            val bill = repository.calculateBill(1L, "2025-10-01", "2025-10-31")

            assertEquals(25, bill.breakfastCount)
            assertEquals(28, bill.lunchCount)
            assertEquals(20, bill.dinnerCount)
            assertEquals(25 * 50.0, bill.breakfastAmount)
            assertEquals(28 * 80.0, bill.lunchAmount)
            assertEquals(20 * 70.0, bill.dinnerAmount)
            assertEquals(1250.0 + 2240.0 + 1400.0, bill.totalAmount)
            assertEquals(2000.0, bill.totalPaid)
            assertEquals(4890.0 - 2000.0, bill.balanceDue)
        }

        @Test
        @DisplayName("handles zero meals correctly")
        fun noMeals() = runTest {
            whenever(providerDao.getProviderByIdOnce(1L)).thenReturn(testProvider)
            whenever(
                mealEntryDao.getTotalQuantityForProviderInRange(
                    eq(1L), any(), any(), any()
                )
            ).thenReturn(null)
            whenever(
                paymentDao.getTotalPaidForProviderInRange(eq(1L), any(), any())
            ).thenReturn(null)

            val bill = repository.calculateBill(1L, "2025-10-01", "2025-10-31")

            assertEquals(0, bill.breakfastCount)
            assertEquals(0, bill.lunchCount)
            assertEquals(0, bill.dinnerCount)
            assertEquals(0.0, bill.totalAmount)
            assertEquals(0.0, bill.totalPaid)
            assertEquals(0.0, bill.balanceDue)
        }

        @Test
        @DisplayName("returns empty bill for unknown provider")
        fun unknownProvider() = runTest {
            whenever(providerDao.getProviderByIdOnce(99L)).thenReturn(null)

            val bill = repository.calculateBill(99L, "2025-10-01", "2025-10-31")

            assertEquals(0.0, bill.totalAmount)
            assertEquals(0.0, bill.balanceDue)
            assertEquals("", bill.providerName)
        }

        @Test
        @DisplayName("shows advance when overpaid")
        fun overpayment() = runTest {
            whenever(providerDao.getProviderByIdOnce(1L)).thenReturn(testProvider)
            whenever(
                mealEntryDao.getTotalQuantityForProviderInRange(
                    eq(1L), any(), any(), eq(MealType.BREAKFAST.name)
                )
            ).thenReturn(0)
            whenever(
                mealEntryDao.getTotalQuantityForProviderInRange(
                    eq(1L), any(), any(), eq(MealType.LUNCH.name)
                )
            ).thenReturn(10)
            whenever(
                mealEntryDao.getTotalQuantityForProviderInRange(
                    eq(1L), any(), any(), eq(MealType.DINNER.name)
                )
            ).thenReturn(0)
            whenever(
                paymentDao.getTotalPaidForProviderInRange(eq(1L), any(), any())
            ).thenReturn(1000.0)

            val bill = repository.calculateBill(1L, "2025-10-01", "2025-10-31")

            assertEquals(800.0, bill.totalAmount) // 10 * 80
            assertEquals(1000.0, bill.totalPaid)
            assertEquals(-200.0, bill.balanceDue) // negative = advance
        }

        @Test
        @DisplayName("handles extra quantities (2 tiffins per meal)")
        fun extraQuantity() = runTest {
            whenever(providerDao.getProviderByIdOnce(1L)).thenReturn(testProvider)
            // 15 entries with qty 2 each = getTotalQuantity returns 30
            whenever(
                mealEntryDao.getTotalQuantityForProviderInRange(
                    eq(1L), any(), any(), eq(MealType.LUNCH.name)
                )
            ).thenReturn(30)
            whenever(
                mealEntryDao.getTotalQuantityForProviderInRange(
                    eq(1L), any(), any(), eq(MealType.BREAKFAST.name)
                )
            ).thenReturn(0)
            whenever(
                mealEntryDao.getTotalQuantityForProviderInRange(
                    eq(1L), any(), any(), eq(MealType.DINNER.name)
                )
            ).thenReturn(0)
            whenever(
                paymentDao.getTotalPaidForProviderInRange(eq(1L), any(), any())
            ).thenReturn(0.0)

            val bill = repository.calculateBill(1L, "2025-10-01", "2025-10-31")

            assertEquals(30, bill.lunchCount)
            assertEquals(30 * 80.0, bill.lunchAmount)
            assertEquals(2400.0, bill.totalAmount)
        }

        @Test
        @DisplayName("only-lunch provider ignores breakfast and dinner prices")
        fun lunchOnlyProvider() = runTest {
            val lunchOnly = testProvider.copy(
                offersBreakfast = false,
                offersDinner = false
            )
            whenever(providerDao.getProviderByIdOnce(1L)).thenReturn(lunchOnly)
            // Even if there are breakfast entries somehow, the DAO returns null for them
            whenever(
                mealEntryDao.getTotalQuantityForProviderInRange(
                    eq(1L), any(), any(), eq(MealType.BREAKFAST.name)
                )
            ).thenReturn(null)
            whenever(
                mealEntryDao.getTotalQuantityForProviderInRange(
                    eq(1L), any(), any(), eq(MealType.LUNCH.name)
                )
            ).thenReturn(20)
            whenever(
                mealEntryDao.getTotalQuantityForProviderInRange(
                    eq(1L), any(), any(), eq(MealType.DINNER.name)
                )
            ).thenReturn(null)
            whenever(
                paymentDao.getTotalPaidForProviderInRange(eq(1L), any(), any())
            ).thenReturn(500.0)

            val bill = repository.calculateBill(1L, "2025-10-01", "2025-10-31")

            assertEquals(0, bill.breakfastCount)
            assertEquals(20, bill.lunchCount)
            assertEquals(0, bill.dinnerCount)
            assertEquals(0.0, bill.breakfastAmount)
            assertEquals(1600.0, bill.lunchAmount)
            assertEquals(0.0, bill.dinnerAmount)
            assertEquals(1600.0, bill.totalAmount)
            assertEquals(1100.0, bill.balanceDue)
        }
    }

    @Nested
    @DisplayName("Date utilities")
    inner class DateUtilities {

        @Test
        @DisplayName("formatCurrency formats Indian Rupees correctly")
        fun formatCurrency() {
            val result = com.tiffintracker.util.formatCurrency(1234.50)
            assert(result.contains("1,234.50") || result.contains("1234.50"))
            assert(result.contains("₹"))
        }

        @Test
        @DisplayName("getMonthRange returns first and last day of month")
        fun monthRange() {
            val (start, end) = com.tiffintracker.util.getMonthRange(
                java.time.YearMonth.of(2025, 10)
            )
            assertEquals("2025-10-01", start)
            assertEquals("2025-10-31", end)
        }

        @Test
        @DisplayName("getMonthRange handles February in leap year")
        fun leapYear() {
            val (start, end) = com.tiffintracker.util.getMonthRange(
                java.time.YearMonth.of(2024, 2)
            )
            assertEquals("2024-02-01", start)
            assertEquals("2024-02-29", end)
        }

        @Test
        @DisplayName("getBillingCycleRange with mid-month start")
        fun billingCycleMidMonth() {
            val (start, end) = com.tiffintracker.util.getBillingCycleRange(
                java.time.YearMonth.of(2025, 10), 15
            )
            assertEquals("2025-10-15", start)
            assertEquals("2025-11-14", end)
        }
    }
}
