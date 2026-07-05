package io.github.aadi1607.habittracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import io.github.aadi1607.habittracker.data.HabitRepository
import io.github.aadi1607.habittracker.data.SettingsRepository
import io.github.aadi1607.habittracker.data.backup.BackupManager
import io.github.aadi1607.habittracker.data.db.HabitDatabase
import io.github.aadi1607.habittracker.data.settingsDataStore
import io.github.aadi1607.habittracker.reminder.ReminderScheduler

class HabitApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createReminderChannel()
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
