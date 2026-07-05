package io.github.aadi1607.habittracker.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.aadi1607.habittracker.HabitApplication
import io.github.aadi1607.habittracker.MainActivity
import io.github.aadi1607.habittracker.R
import java.time.LocalDate

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as HabitApplication
        val repository = app.container.repository

        val total = repository.getHabits().size
        if (total == 0) return Result.success()
        val done = repository.countCompletionsOn(LocalDate.now())
        val remaining = total - done
        if (remaining <= 0) return Result.success()

        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return Result.success()

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val text = applicationContext.resources.getQuantityString(
            R.plurals.reminder_notification_text,
            remaining,
            remaining,
        )
        val notification = NotificationCompat.Builder(applicationContext, HabitApplication.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.reminder_notification_title))
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching { manager.notify(NOTIFICATION_ID, notification) }
        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
