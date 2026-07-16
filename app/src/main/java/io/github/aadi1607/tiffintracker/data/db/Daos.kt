package io.github.aadi1607.tiffintracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY sortOrder, id")
    fun observeUsers(): Flow<List<User>>

    @Query("SELECT * FROM users ORDER BY sortOrder, id")
    suspend fun getUsers(): List<User>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Insert
    suspend fun insert(user: User): Long

    @Insert
    suspend fun insertAll(users: List<User>)

    @Update
    suspend fun update(user: User)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun delete(userId: Long)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}

@Dao
interface TiffinEntryDao {
    @Query("SELECT * FROM tiffin_entries WHERE epochDay = :epochDay")
    fun observeForDay(epochDay: Long): Flow<List<TiffinEntry>>

    @Query("SELECT * FROM tiffin_entries WHERE epochDay BETWEEN :startDay AND :endDay")
    fun observeForRange(startDay: Long, endDay: Long): Flow<List<TiffinEntry>>

    @Query("SELECT * FROM tiffin_entries WHERE epochDay BETWEEN :startDay AND :endDay")
    suspend fun getForRange(startDay: Long, endDay: Long): List<TiffinEntry>

    @Query("SELECT * FROM tiffin_entries")
    fun observeAll(): Flow<List<TiffinEntry>>

    @Query("SELECT * FROM tiffin_entries")
    suspend fun getAll(): List<TiffinEntry>

    @Query("SELECT COUNT(*) FROM tiffin_entries WHERE userId = :userId AND epochDay = :epochDay")
    suspend fun countForUserDay(userId: Long, epochDay: Long): Int

    /** REPLACE + the (userId, epochDay, mealType) unique index = duplicate-safe upsert. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: TiffinEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<TiffinEntry>)

    @Query("DELETE FROM tiffin_entries WHERE userId = :userId AND epochDay = :epochDay AND mealType = :mealType")
    suspend fun delete(userId: Long, epochDay: Long, mealType: MealType)

    @Query("DELETE FROM tiffin_entries WHERE epochDay BETWEEN :startDay AND :endDay")
    suspend fun deleteRange(startDay: Long, endDay: Long)

    @Query("DELETE FROM tiffin_entries")
    suspend fun deleteAll()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY epochDay DESC, id DESC")
    fun observeAll(): Flow<List<Payment>>

    @Query("SELECT * FROM payments")
    suspend fun getAll(): List<Payment>

    @Insert
    suspend fun insert(payment: Payment)

    @Insert
    suspend fun insertAll(payments: List<Payment>)

    @Delete
    suspend fun delete(payment: Payment)

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}
