package io.github.aadi1607.habittracker.data

import io.github.aadi1607.habittracker.data.db.Completion
import io.github.aadi1607.habittracker.data.db.Habit
import io.github.aadi1607.habittracker.data.db.HabitDao
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class PendingHabit(val habit: Habit, val count: Int)

class HabitRepository(private val dao: HabitDao) {

    fun observeHabits(): Flow<List<Habit>> = dao.observeHabits()

    fun observeCompletions(): Flow<List<Completion>> = dao.observeCompletions()

    suspend fun addHabit(name: String, emoji: String, color: Long, dailyTarget: Int) {
        dao.insertHabit(
            Habit(
                name = name,
                emoji = emoji,
                color = color,
                createdAt = System.currentTimeMillis(),
                dailyTarget = dailyTarget.coerceAtLeast(1),
            )
        )
    }

    suspend fun deleteHabit(habitId: Long) = dao.deleteHabit(habitId)

    /** Logs one more completion of the habit for [date]. */
    suspend fun increment(habitId: Long, date: LocalDate) = dao.increment(habitId, date.toString())

    /** Undoes one logged completion of the habit for [date]. */
    suspend fun decrement(habitId: Long, date: LocalDate) = dao.decrement(habitId, date.toString())

    suspend fun getHabits(): List<Habit> = dao.getHabits()

    suspend fun getCompletions(): List<Completion> = dao.getCompletions()

    /** Habits that have not reached their daily target on [date], with progress. */
    suspend fun getPendingOn(date: LocalDate): List<PendingHabit> {
        val countByHabit = dao.getCompletionsOn(date.toString()).associate { it.habitId to it.count }
        return dao.getHabits().mapNotNull { habit ->
            val count = countByHabit[habit.id] ?: 0
            if (count < habit.dailyTarget) PendingHabit(habit, count) else null
        }
    }

    suspend fun replaceAll(habits: List<Habit>, completions: List<Completion>) =
        dao.replaceAll(habits, completions)
}
