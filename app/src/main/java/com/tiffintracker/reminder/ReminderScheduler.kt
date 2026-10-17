package com.tiffintracker.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val WORK_NAME = "tiffin_daily_reminder"

    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var targetTime = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        if (targetTime.isBefore(now) || targetTime.isEqual(now)) {
            targetTime = targetTime.plusDays(1)
        }
        val initialDelay = Duration.between(now, targetTime).toMillis()

        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
