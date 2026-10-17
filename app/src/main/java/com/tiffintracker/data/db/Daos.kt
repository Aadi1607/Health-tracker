package com.tiffintracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class MonthlyStats(
    val totalReceived: Int,
    val totalSkipped: Int,
    val totalNotOrdered: Int
)

// --- ProviderDao ---

@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers ORDER BY name ASC")
    fun getAllProviders(): Flow<List<Provider>>

    @Query("SELECT * FROM providers WHERE is_active = 1 ORDER BY name ASC")
    fun getActiveProviders(): Flow<List<Provider>>

    @Query("SELECT * FROM providers WHERE id = :id")
    fun getProviderById(id: Long): Flow<Provider?>

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getProviderByIdOnce(id: Long): Provider?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provider: Provider): Long

    @Update
    suspend fun update(provider: Provider)

    @Delete
    suspend fun delete(provider: Provider)

    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM providers ORDER BY name ASC")
    suspend fun getAllProvidersList(): List<Provider>
}

// --- MealEntryDao ---

@Dao
interface MealEntryDao {
    @Query("SELECT * FROM meal_entries WHERE date = :date ORDER BY provider_id, meal_type")
    fun getEntriesForDate(date: String): Flow<List<MealEntry>>

    @Query(
        "SELECT * FROM meal_entries WHERE provider_id = :providerId " +
        "AND date >= :startDate AND date <= :endDate ORDER BY date, meal_type"
    )
    fun getEntriesForProviderInRange(
        providerId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<MealEntry>>

    @Query(
        "SELECT * FROM meal_entries WHERE date >= :startDate AND date <= :endDate " +
        "ORDER BY date, provider_id, meal_type"
    )
    fun getEntriesInRange(startDate: String, endDate: String): Flow<List<MealEntry>>

    @Query(
        "SELECT * FROM meal_entries WHERE provider_id = :providerId " +
        "AND date = :date AND meal_type = :mealType LIMIT 1"
    )
    suspend fun getEntryForProviderDateMeal(
        providerId: Long,
        date: String,
        mealType: String
    ): MealEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MealEntry): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: MealEntry): Long

    @Update
    suspend fun update(entry: MealEntry)

    @Delete
    suspend fun delete(entry: MealEntry)

    @Query("DELETE FROM meal_entries WHERE date = :date")
    suspend fun deleteEntriesForDate(date: String)

    @Query(
        "SELECT COUNT(*) FROM meal_entries WHERE provider_id = :providerId " +
        "AND date >= :startDate AND date <= :endDate " +
        "AND meal_type = :mealType AND status = 'RECEIVED'"
    )
    suspend fun getReceivedCountForProviderInRange(
        providerId: Long,
        startDate: String,
        endDate: String,
        mealType: String
    ): Int

    @Query(
        "SELECT SUM(quantity) FROM meal_entries WHERE provider_id = :providerId " +
        "AND date >= :startDate AND date <= :endDate " +
        "AND meal_type = :mealType AND status = 'RECEIVED'"
    )
    suspend fun getTotalQuantityForProviderInRange(
        providerId: Long,
        startDate: String,
        endDate: String,
        mealType: String
    ): Int?

    @Query(
        "SELECT " +
        "SUM(CASE WHEN status = 'RECEIVED' THEN 1 ELSE 0 END) as totalReceived, " +
        "SUM(CASE WHEN status = 'SKIPPED' THEN 1 ELSE 0 END) as totalSkipped, " +
        "SUM(CASE WHEN status = 'NOT_ORDERED' THEN 1 ELSE 0 END) as totalNotOrdered " +
        "FROM meal_entries WHERE date >= :startDate AND date <= :endDate"
    )
    fun getMonthlyStats(startDate: String, endDate: String): Flow<MonthlyStats>

    @Query("SELECT * FROM meal_entries ORDER BY date DESC")
    suspend fun getAllEntries(): List<MealEntry>
}

// --- PaymentDao ---

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE provider_id = :providerId ORDER BY date DESC")
    fun getPaymentsForProvider(providerId: Long): Flow<List<Payment>>

    @Query(
        "SELECT * FROM payments WHERE provider_id = :providerId " +
        "AND date >= :startDate AND date <= :endDate ORDER BY date DESC"
    )
    fun getPaymentsForProviderInRange(
        providerId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<Payment>>

    @Query(
        "SELECT * FROM payments WHERE date >= :startDate AND date <= :endDate " +
        "ORDER BY date DESC"
    )
    fun getAllPaymentsInRange(startDate: String, endDate: String): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment): Long

    @Update
    suspend fun update(payment: Payment)

    @Delete
    suspend fun delete(payment: Payment)

    @Query(
        "SELECT SUM(amount) FROM payments WHERE provider_id = :providerId " +
        "AND date >= :startDate AND date <= :endDate"
    )
    suspend fun getTotalPaidForProviderInRange(
        providerId: Long,
        startDate: String,
        endDate: String
    ): Double?

    @Query("SELECT * FROM payments ORDER BY date DESC")
    suspend fun getAllPayments(): List<Payment>
}
