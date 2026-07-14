package io.github.aadi1607.habittracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt ASC")
    fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM completions")
    fun observeCompletions(): Flow<List<Completion>>

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getHabits(): List<Habit>

    @Query("SELECT * FROM completions")
    suspend fun getCompletions(): List<Completion>

    /** All completion dates of one habit, newest first — input for streak computation. */
    @Query("SELECT date FROM completions WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getCompletionDates(habitId: Long): List<String>

    /** Completions in the last 7 days window used by the habit card chain. */
    @Query("SELECT * FROM completions WHERE date >= :fromDate AND date <= :toDate")
    suspend fun getCompletionsBetween(fromDate: String, toDate: String): List<Completion>

    @Query("SELECT * FROM completions WHERE date = :date")
    suspend fun getCompletionsOn(date: String): List<Completion>

    @Insert
    suspend fun insertHabit(habit: Habit): Long

    @Query(
        "UPDATE habits SET name = :name, emoji = :emoji, color = :color, " +
            "dailyTarget = :dailyTarget, goalPeriod = :goalPeriod WHERE id = :habitId"
    )
    suspend fun updateHabit(
        habitId: Long,
        name: String,
        emoji: String,
        color: Long,
        dailyTarget: Int,
        goalPeriod: String,
    )

    @Query("UPDATE habits SET sortOrder = :sortOrder WHERE id = :habitId")
    suspend fun updateSortOrder(habitId: Long, sortOrder: Long)

    @Query("UPDATE habits SET archived = :archived WHERE id = :habitId")
    suspend fun setArchived(habitId: Long, archived: Boolean)

    /** Swaps the display positions of two habits. */
    @Transaction
    suspend fun swapSortOrders(firstId: Long, firstOrder: Long, secondId: Long, secondOrder: Long) {
        updateSortOrder(firstId, secondOrder)
        updateSortOrder(secondId, firstOrder)
    }

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: Completion)

    @Query("DELETE FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCompletion(habitId: Long, date: String)

    @Query("UPDATE completions SET count = count + 1 WHERE habitId = :habitId AND date = :date")
    suspend fun incrementCount(habitId: Long, date: String): Int

    @Query("UPDATE completions SET count = count - 1 WHERE habitId = :habitId AND date = :date AND count > 1")
    suspend fun decrementCount(habitId: Long, date: String): Int

    /** Logs the habit once more for [date], creating the row on the first log. */
    @Transaction
    suspend fun increment(habitId: Long, date: String) {
        if (incrementCount(habitId, date) == 0) {
            insertCompletion(Completion(habitId, date, 1))
        }
    }

    /** Removes one log for [date]; the row disappears when the count reaches zero. */
    @Transaction
    suspend fun decrement(habitId: Long, date: String) {
        if (decrementCount(habitId, date) == 0) {
            deleteCompletion(habitId, date)
        }
    }

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
