package io.github.aadi1607.habittracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import io.github.aadi1607.habittracker.data.HabitRepository
import io.github.aadi1607.habittracker.data.SettingsRepository
import io.github.aadi1607.habittracker.data.backup.BackupManager
import io.github.aadi1607.habittracker.data.db.HabitDatabase
import io.github.aadi1607.habittracker.data.settingsDataStore
import io.github.aadi1607.habittracker.reminder.ReminderScheduler

class HabitApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createReminderChannel()
        // Re-arm alarms on every process start: aggressive OEM battery managers
        // (and force-stops) silently drop AlarmManager schedules, and this heals
        // them the next time the user opens the app.
        appScope.launch {
            val reminder = container.settings.reminder.first()
            if (reminder.enabled) {
                container.reminderScheduler.scheduleDaily(reminder.hour, reminder.minute)
            }
            if (reminder.nudgesEnabled) {
                container.reminderScheduler.ensureNudgeScheduled(reminder.nudgeIntervalHours)
            }
        }
    }

    private fun createReminderChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                REMINDER_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NUDGE_CHANNEL_ID,
                getString(R.string.nudge_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.nudge_channel_description)
            }
        )
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "daily_reminders"
        const val NUDGE_CHANNEL_ID = "habit_nudges"
    }
}

class AppContainer(context: Context) {
    private val database = HabitDatabase.build(context)
    val repository = HabitRepository(database.habitDao())
    val settings = SettingsRepository(context.settingsDataStore)
    val reminderScheduler = ReminderScheduler(context)
    val backupManager = BackupManager(context, repository)
}
