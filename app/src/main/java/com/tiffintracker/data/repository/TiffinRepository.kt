package com.tiffintracker.data.repository

import com.tiffintracker.data.db.MealEntry
import com.tiffintracker.data.db.MealEntryDao
import com.tiffintracker.data.db.MealStatus
import com.tiffintracker.data.db.MealType
import com.tiffintracker.data.db.Payment
import com.tiffintracker.data.db.PaymentDao
import com.tiffintracker.data.db.Provider
import com.tiffintracker.data.db.ProviderDao
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class BillSummary(
    val providerId: Long,
    val providerName: String,
    val startDate: String,
    val endDate: String,
    val breakfastCount: Int,
    val lunchCount: Int,
    val dinnerCount: Int,
    val breakfastAmount: Double,
    val lunchAmount: Double,
    val dinnerAmount: Double,
    val totalAmount: Double,
    val totalPaid: Double,
    val balanceDue: Double
)

@Singleton
class TiffinRepository @Inject constructor(
    private val providerDao: ProviderDao,
    private val mealEntryDao: MealEntryDao,
    private val paymentDao: PaymentDao
) {
    // --- Provider operations ---

    fun getAllProviders(): Flow<List<Provider>> = providerDao.getAllProviders()

    fun getActiveProviders(): Flow<List<Provider>> = providerDao.getActiveProviders()

    fun getProviderById(id: Long): Flow<Provider?> = providerDao.getProviderById(id)

    suspend fun getProviderByIdOnce(id: Long): Provider? = providerDao.getProviderByIdOnce(id)

    suspend fun insertProvider(provider: Provider): Long = providerDao.insert(provider)

    suspend fun updateProvider(provider: Provider) = providerDao.update(provider)

    suspend fun deleteProvider(provider: Provider) = providerDao.delete(provider)

    suspend fun deleteProviderById(id: Long) = providerDao.deleteById(id)

    // --- MealEntry operations ---

    fun getEntriesForDate(date: String): Flow<List<MealEntry>> =
        mealEntryDao.getEntriesForDate(date)

    fun getEntriesForProviderInRange(
        providerId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<MealEntry>> =
        mealEntryDao.getEntriesForProviderInRange(providerId, startDate, endDate)

    fun getEntriesInRange(startDate: String, endDate: String): Flow<List<MealEntry>> =
        mealEntryDao.getEntriesInRange(startDate, endDate)

    suspend fun upsertMealEntry(entry: MealEntry): Long = mealEntryDao.upsert(entry)

    suspend fun insertEntry(entry: MealEntry): Long = mealEntryDao.insert(entry)

    suspend fun updateEntry(entry: MealEntry) = mealEntryDao.update(entry)

    suspend fun getEntryForProviderDateMeal(
        providerId: Long,
        date: String,
        mealType: String
    ): MealEntry? = mealEntryDao.getEntryForProviderDateMeal(providerId, date, mealType)

    suspend fun deleteMealEntry(entry: MealEntry) = mealEntryDao.delete(entry)

    fun getMonthlyStats(startDate: String, endDate: String) =
        mealEntryDao.getMonthlyStats(startDate, endDate)

    // --- Payment operations ---

    fun getPaymentsForProvider(providerId: Long): Flow<List<Payment>> =
        paymentDao.getPaymentsForProvider(providerId)

    fun getPaymentsForProviderInRange(
        providerId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<Payment>> =
        paymentDao.getPaymentsForProviderInRange(providerId, startDate, endDate)

    fun getAllPaymentsInRange(startDate: String, endDate: String): Flow<List<Payment>> =
        paymentDao.getAllPaymentsInRange(startDate, endDate)

    suspend fun insertPayment(payment: Payment): Long = paymentDao.insert(payment)

    suspend fun updatePayment(payment: Payment) = paymentDao.update(payment)

    suspend fun deletePayment(payment: Payment) = paymentDao.delete(payment)

    // --- Billing ---

    suspend fun calculateBill(
        providerId: Long,
        startDate: String,
        endDate: String
    ): BillSummary {
        val provider = providerDao.getProviderByIdOnce(providerId)
            ?: return BillSummary(
                providerId, "", startDate, endDate,
                0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
            )

        val breakfastQty = mealEntryDao.getTotalQuantityForProviderInRange(
            providerId, startDate, endDate, MealType.BREAKFAST.name
        ) ?: 0
        val lunchQty = mealEntryDao.getTotalQuantityForProviderInRange(
            providerId, startDate, endDate, MealType.LUNCH.name
        ) ?: 0
        val dinnerQty = mealEntryDao.getTotalQuantityForProviderInRange(
            providerId, startDate, endDate, MealType.DINNER.name
        ) ?: 0

        val breakfastAmt = breakfastQty * provider.breakfastPrice
        val lunchAmt = lunchQty * provider.lunchPrice
        val dinnerAmt = dinnerQty * provider.dinnerPrice
        val totalAmt = breakfastAmt + lunchAmt + dinnerAmt

        val totalPaid = paymentDao.getTotalPaidForProviderInRange(
            providerId, startDate, endDate
        ) ?: 0.0

        return BillSummary(
            providerId = providerId,
            providerName = provider.name,
            startDate = startDate,
            endDate = endDate,
            breakfastCount = breakfastQty,
            lunchCount = lunchQty,
            dinnerCount = dinnerQty,
            breakfastAmount = breakfastAmt,
            lunchAmount = lunchAmt,
            dinnerAmount = dinnerAmt,
            totalAmount = totalAmt,
            totalPaid = totalPaid,
            balanceDue = totalAmt - totalPaid
        )
    }

    // --- Bulk actions ---

    suspend fun markHoliday(date: String, providerIds: List<Long>) {
        for (pid in providerIds) {
            val provider = providerDao.getProviderByIdOnce(pid) ?: continue
            val mealTypes = buildList {
                if (provider.offersBreakfast) add(MealType.BREAKFAST)
                if (provider.offersLunch) add(MealType.LUNCH)
                if (provider.offersDinner) add(MealType.DINNER)
            }
            for (mt in mealTypes) {
                mealEntryDao.upsert(
                    MealEntry(
                        providerId = pid,
                        date = date,
                        mealType = mt.name,
                        status = MealStatus.SKIPPED.name,
                        quantity = 0,
                        note = "Holiday"
                    )
                )
            }
        }
    }

    suspend fun markAwayRange(startDate: String, endDate: String, providerIds: List<Long>) {
        var current = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        while (!current.isAfter(end)) {
            markHoliday(current.toString(), providerIds)
            current = current.plusDays(1)
        }
    }

    // --- Clear all data ---

    suspend fun clearAllData() {
        val providers = providerDao.getAllProvidersList()
        for (p in providers) {
            providerDao.delete(p)
        }
    }

    // --- For backup ---

    suspend fun getAllEntries(): List<MealEntry> = mealEntryDao.getAllEntries()

    suspend fun getAllPayments(): List<Payment> = paymentDao.getAllPayments()
}
