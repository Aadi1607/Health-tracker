package io.github.aadi1607.habittracker.data

import io.github.aadi1607.habittracker.data.db.Completion
import io.github.aadi1607.habittracker.data.db.Habit
import io.github.aadi1607.habittracker.data.db.HabitDao
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val dao: HabitDao) {

    fun observeHabits(): Flow<List<Habit>> = dao.observeHabits()

    fun observeCompletions(): Flow<List<Completion>> = dao.observeCompletions()

    suspend fun addHabit(name: String, emoji: String, color: Long) {
        dao.insertHabit(
            Habit(
                name = name,
                emoji = emoji,
                color = color,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun deleteHabit(habitId: Long) = dao.deleteHabit(habitId)

    suspend fun setCompleted(habitId: Long, date: LocalDate, completed: Boolean) {
        if (completed) {
            dao.insertCompletion(Completion(habitId, date.toString()))
        } else {
            dao.deleteCompletion(habitId, date.toString())
        }
    }

    suspend fun getHabits(): List<Habit> = dao.getHabits()

    suspend fun getCompletions(): List<Completion> = dao.getCompletions()

    suspend fun countCompletionsOn(date: LocalDate): Int = dao.countCompletionsOn(date.toString())

    suspend fun replaceAll(habits: List<Habit>, completions: List<Completion>) =
        dao.replaceAll(habits, completions)
}
