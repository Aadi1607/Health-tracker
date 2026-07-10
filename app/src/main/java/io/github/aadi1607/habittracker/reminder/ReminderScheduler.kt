package io.github.aadi1607.habittracker.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Exact-time scheduling with AlarmManager so reminders arrive at the precise
 * minute, even in Doze. Each alarm reschedules the next one when it fires
 * (see [ReminderReceiver]); [BootReceiver] restores alarms after reboots and
 * app updates.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    /** True when Android will let us schedule exact alarms right now. */
    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun scheduleDaily(hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        val todayAtTime = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        val next = if (todayAtTime.isAfter(now)) todayAtTime else todayAtTime.plusDays(1)
        setAlarm(next, dailyPendingIntent())
    }

    fun cancelDaily() {
        alarmManager.cancel(dailyPendingIntent())
    }

    /** Schedules the next nudge [intervalHours] from now, clamped to the active window. */
    fun scheduleNextNudge(intervalHours: Int) {
        val now = LocalDateTime.now()
        var next = now.plusHours(intervalHours.toLong().coerceAtLeast(1L))
        if (next.hour >= ACTIVE_WINDOW_END_HOUR) {
            next = next.toLocalDate().plusDays(1).atTime(ACTIVE_WINDOW_START_HOUR, 0)
        } else if (next.hour < ACTIVE_WINDOW_START_HOUR) {
            next = next.toLocalDate().atTime(ACTIVE_WINDOW_START_HOUR, 0)
        }
        setAlarm(next, nudgePendingIntent())
    }

    fun cancelNudges() {
        alarmManager.cancel(nudgePendingIntent())
    }

    private fun setAlarm(at: LocalDateTime, pendingIntent: PendingIntent) {
        val triggerAtMillis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            // Exact-alarm permission revoked between the check and the call.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun dailyPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_DAILY,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_DAILY),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun nudgePendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_NUDGE,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_NUDGE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val REQUEST_DAILY = 1
        private const val REQUEST_NUDGE = 2
        const val ACTIVE_WINDOW_START_HOUR = 8
        const val ACTIVE_WINDOW_END_HOUR = 22
    }
}
