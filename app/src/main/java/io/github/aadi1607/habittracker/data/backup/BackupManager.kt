package io.github.aadi1607.habittracker.data.backup

import android.content.Context
import android.net.Uri
import io.github.aadi1607.habittracker.data.HabitRepository
import io.github.aadi1607.habittracker.data.db.Completion
import io.github.aadi1607.habittracker.data.db.Habit
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class BackupManager(
    private val context: Context,
    private val repository: HabitRepository,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun exportTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val data = BackupData(
                habits = repository.getHabits().map {
                    BackupHabit(
                        it.id, it.name, it.emoji, it.color, it.createdAt,
                        it.dailyTarget, it.goalPeriod, it.sortOrder, it.archived,
                    )
                },
                completions = repository.getCompletions().map {
                    BackupCompletion(it.habitId, it.date, it.count)
                },
            )
            val stream = context.contentResolver.openOutputStream(uri)
                ?: throw IOException("Cannot open $uri")
            stream.use { it.write(json.encodeToString(BackupData.serializer(), data).toByteArray()) }
        }
    }

    suspend fun importFrom(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open $uri")
            val text = stream.use { it.readBytes().decodeToString() }
            val data = json.decodeFromString(BackupData.serializer(), text)
            repository.replaceAll(
                habits = data.habits.map {
                    Habit(
                        it.id, it.name, it.emoji, it.color, it.createdAt,
                        it.dailyTarget, it.goalPeriod, it.sortOrder, it.archived,
                    )
                },
                completions = data.completions.map {
                    Completion(it.habitId, it.date, it.count)
                },
            )
            data.habits.size
        }
    }
}
