package io.github.aadi1607.habittracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM completions")
    fun observeCompletions(): Flow<List<Completion>>

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    suspend fun getHabits(): List<Habit>

    @Query("SELECT * FROM completions")
    suspend fun getCompletions(): List<Completion>

    /** All completion dates of one habit, newest first — input for streak computation. */
    @Query("SELECT date FROM completions WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getCompletionDates(habitId: Long): List<String>

    /** Completions in the last 7 days window used by the habit card chain. */
    @Query("SELECT * FROM completions WHERE date >= :fromDate AND date <= :toDate")
    suspend fun getCompletionsBetween(fromDate: String, toDate: String): List<Completion>

    @Query("SELECT COUNT(*) FROM completions WHERE date = :date")
    suspend fun countCompletionsOn(date: String): Int

    @Insert
    suspend fun insertHabit(habit: Habit): Long

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: Completion)

    @Query("DELETE FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCompletion(habitId: Long, date: String)

    @Insert
    suspend fun insertHabits(habits: List<Habit>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletions(completions: List<Completion>)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Transaction
    suspend fun replaceAll(habits: List<Habit>, completions: List<Completion>) {
        // Completions cascade-delete with their habits.
        deleteAllHabits()
        insertHabits(habits)
        insertCompletions(completions)
    }
}
