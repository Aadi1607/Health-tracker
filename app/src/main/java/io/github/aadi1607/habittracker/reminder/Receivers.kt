package io.github.aadi1607.habittracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import io.github.aadi1607.habittracker.HabitApplication
import io.github.aadi1607.habittracker.widget.HabitWidget
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Runs suspending work from a receiver, keeping the process alive until done. */
private fun BroadcastReceiver.goAsync(block: suspend () -> Unit) {
    val result = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block()
        } finally {
            result.finish()
        }
    }
}

/** Fired by the exact alarms; posts the notification and arms the next alarm. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext as HabitApplication
        goAsync {
            val settings = app.container.settings.reminder.first()
            val scheduler = app.container.reminderScheduler
            when (action) {
                ACTION_DAILY -> {
                    if (!settings.enabled) return@goAsync
                    val pending = app.container.repository.getPendingOn(LocalDate.now())
                    if (pending.isNotEmpty()) {
                        NotificationHelper.postPending(
                            context,
                            pending,
                            NotificationHelper.DAILY_NOTIFICATION_ID,
                        )
                    }
                    scheduler.scheduleDaily(settings.hour, settings.minute)
                }
                ACTION_NUDGE -> {
                    if (!settings.nudgesEnabled) return@goAsync
                    val hour = LocalTime.now().hour
                    val inWindow = hour >= ReminderScheduler.ACTIVE_WINDOW_START_HOUR &&
                        hour < ReminderScheduler.ACTIVE_WINDOW_END_HOUR
                    if (inWindow) {
                        val pending = app.container.repository.getPendingOn(LocalDate.now())
                        if (pending.isNotEmpty()) {
                            NotificationHelper.postPending(
                                context,
                                pending,
                                NotificationHelper.NUDGE_NOTIFICATION_ID,
                            )
                        }
                    }
                    scheduler.scheduleNextNudge(settings.nudgeIntervalHours)
                }
            }
        }
    }

    companion object {
        const val ACTION_DAILY = "io.github.aadi1607.habittracker.action.DAILY_REMINDER"
        const val ACTION_NUDGE = "io.github.aadi1607.habittracker.action.NUDGE"
    }
}

/** Handles the "+1" quick action on notifications: logs one completion. */
class LogHabitReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_LOG) return
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NotificationHelper.NUDGE_NOTIFICATION_ID)
        if (habitId == -1L) return
        val app = context.applicationContext as HabitApplication
        goAsync {
            val today = LocalDate.now()
            app.container.repository.increment(habitId, today)
            val pending = app.container.repository.getPendingOn(today)
            if (pending.isEmpty()) {
                NotificationHelper.cancel(context, notificationId)
            } else {
                NotificationHelper.postPending(context, pending, notificationId)
            }
            HabitWidget().updateAll(context)
        }
    }

    companion object {
        const val ACTION_LOG = "io.github.aadi1607.habittracker.action.LOG_HABIT"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}

/** Restores alarms after a reboot or an app update (alarms don't survive either). */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val app = context.applicationContext as HabitApplication
        goAsync {
            val settings = app.container.settings.reminder.first()
            val scheduler = app.container.reminderScheduler
            if (settings.enabled) {
                scheduler.scheduleDaily(settings.hour, settings.minute)
            }
            if (settings.nudgesEnabled) {
                scheduler.scheduleNextNudge(settings.nudgeIntervalHours)
            }
        }
    }
}
