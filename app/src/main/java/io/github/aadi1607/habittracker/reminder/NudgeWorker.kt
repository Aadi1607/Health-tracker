package io.github.aadi1607.habittracker.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.aadi1607.habittracker.HabitApplication
import io.github.aadi1607.habittracker.R
import java.time.LocalDate
import java.time.LocalTime

/**
 * Repeating daytime check-in for habits done several times a day (water, stretch
 * breaks, …). Fires every few hours and lists what is still pending; stays quiet
 * outside the active window and once everything is done.
 */
class NudgeWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val hour = LocalTime.now().hour
        if (hour < ACTIVE_WINDOW_START_HOUR || hour >= ACTIVE_WINDOW_END_HOUR) {
            return Result.success()
        }

        val app = applicationContext as HabitApplication
        val pending = app.container.repository.getPendingOn(LocalDate.now())
        if (pending.isEmpty()) return Result.success()

        postPendingNotification(
            context = applicationContext,
            pending = pending,
            channelId = HabitApplication.NUDGE_CHANNEL_ID,
            notificationId = NOTIFICATION_ID,
            title = applicationContext.getString(R.string.nudge_notification_title),
        )
        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 1002
        const val ACTIVE_WINDOW_START_HOUR = 8
        const val ACTIVE_WINDOW_END_HOUR = 22
    }
}
