package io.github.aadi1607.habittracker.data

import io.github.aadi1607.habittracker.data.db.Completion
import io.github.aadi1607.habittracker.data.db.Habit
import io.github.aadi1607.habittracker.data.db.HabitDao
import io.github.aadi1607.habittracker.domain.Streaks
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class PendingHabit(val habit: Habit, val count: Int)

class HabitRepository(private val dao: HabitDao) {

    fun observeHabits(): Flow<List<Habit>> = dao.observeHabits()

    fun observeCompletions(): Flow<List<Completion>> = dao.observeCompletions()

    suspend fun addHabit(name: String, emoji: String, color: Long, dailyTarget: Int, goalPeriod: String) {
        val now = System.currentTimeMillis()
        dao.insertHabit(
            Habit(
                name = name,
                emoji = emoji,
                color = color,
                createdAt = now,
                dailyTarget = dailyTarget.coerceAtLeast(1),
                goalPeriod = goalPeriod,
                sortOrder = now,
            )
        )
    }

    suspend fun updateHabit(
        habitId: Long,
        name: String,
        emoji: String,
        color: Long,
        dailyTarget: Int,
        goalPeriod: String,
    ) = dao.updateHabit(habitId, name.trim(), emoji, color, dailyTarget.coerceAtLeast(1), goalPeriod)

    suspend fun swapSortOrders(first: Habit, second: Habit) =
        dao.swapSortOrders(first.id, first.sortOrder, second.id, second.sortOrder)

    suspend fun deleteHabit(habitId: Long) = dao.deleteHabit(habitId)

    /** Logs one more completion of the habit for [date]. */
    suspend fun increment(habitId: Long, date: LocalDate) = dao.increment(habitId, date.toString())

    /** Undoes one logged completion of the habit for [date]. */
    suspend fun decrement(habitId: Long, date: LocalDate) = dao.decrement(habitId, date.toString())

    suspend fun getHabits(): List<Habit> = dao.getHabits()

    suspend fun getCompletions(): List<Completion> = dao.getCompletions()

    /**
     * Each habit's progress toward its target for the period containing [date]:
     * today's logs for daily habits, this ISO week's logs for weekly ones.
     */
    suspend fun getProgressOn(date: LocalDate): List<PendingHabit> {
        val weekStart = Streaks.weekStart(date)
        val weekCompletions = dao.getCompletionsBetween(weekStart.toString(), date.toString())
        return dao.getHabits().map { habit ->
            val count = weekCompletions
                .filter { it.habitId == habit.id }
                .filter { habit.isWeekly || it.date == date.toString() }
                .sumOf { it.count }
            PendingHabit(habit, count)
        }
    }

    /** Habits that have not reached their target in the current period. */
    suspend fun getPendingOn(date: LocalDate): List<PendingHabit> =
        getProgressOn(date).filter { it.count < it.habit.dailyTarget }

    suspend fun replaceAll(habits: List<Habit>, completions: List<Completion>) =
        dao.replaceAll(habits, completions)
}
