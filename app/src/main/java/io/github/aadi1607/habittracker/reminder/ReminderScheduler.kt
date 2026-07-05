package io.github.aadi1607.habittracker.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderScheduler(private val context: Context) {

    fun schedule(hour: Int, minute: Int) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(Duration.ofDays(1))
            .setInitialDelay(delayUntilNext(hour, minute))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Repeating check-ins during the day while habits are still pending. */
    fun scheduleNudges(intervalHours: Int) {
        val request = PeriodicWorkRequestBuilder<NudgeWorker>(
            Duration.ofHours(intervalHours.toLong().coerceAtLeast(1L))
        )
            .setInitialDelay(Duration.ofHours(intervalHours.toLong().coerceAtLeast(1L)))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NUDGE_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    fun cancelNudges() {
        WorkManager.getInstance(context).cancelUniqueWork(NUDGE_WORK_NAME)
    }

    private fun delayUntilNext(hour: Int, minute: Int): Duration {
        val now = LocalDateTime.now()
        val todayAtTime = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        val next = if (todayAtTime.isAfter(now)) todayAtTime else todayAtTime.plusDays(1)
        return Duration.between(now, next)
    }

    companion object {
        private const val WORK_NAME = "daily_reminder"
        private const val NUDGE_WORK_NAME = "habit_nudges"
    }
}
