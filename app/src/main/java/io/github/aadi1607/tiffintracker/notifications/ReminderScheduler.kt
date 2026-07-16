package io.github.aadi1607.tiffintracker.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.github.aadi1607.tiffintracker.TiffinApplication
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Schedules the two recurring checks as self-re-arming one-time WorkManager
 * jobs. WorkManager persists its queue in its own database, and the
 * BOOT_COMPLETED / TIMEZONE_CHANGED receiver re-arms with fresh delays, so
 * reminders survive reboots, app updates, and clock changes.
 *
 * One-time requests with a computed initial delay are used instead of
 * periodic work because periodic work cannot hit an exact wall-clock time;
 * each worker re-arms itself for the next day after it runs.
 */
object ReminderScheduler {

    const val WORK_DAILY_REMINDER = "daily_reminder"
    const val WORK_BILLING_CHECK = "billing_check"

    /** Fixed evening slot for cycle-end summaries and payment reminders. */
    private val BILLING_CHECK_TIME: LocalTime = LocalTime.of(20, 0)

    suspend fun scheduleAll(context: Context, replace: Boolean) {
        val settings = TiffinApplication.from(context).settingsRepository.current()
        scheduleDailyReminder(context, settings.reminderHour, settings.reminderMinute, replace)
        scheduleBillingCheck(context, replace)
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int, replace: Boolean) {
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delayUntilNext(LocalTime.of(hour, minute)))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_DAILY_REMINDER,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun scheduleBillingCheck(context: Context, replace: Boolean) {
        val request = OneTimeWorkRequestBuilder<BillingCheckWorker>()
            .setInitialDelay(delayUntilNext(BILLING_CHECK_TIME))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_BILLING_CHECK,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun delayUntilNext(target: LocalTime): Duration {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(target)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
