package io.github.aadi1607.habittracker.reminder

import android.app.PendingIntent
import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.aadi1607.habittracker.HabitApplication
import io.github.aadi1607.habittracker.MainActivity
import io.github.aadi1607.habittracker.R
import io.github.aadi1607.habittracker.data.PendingHabit
import java.time.LocalDate

/** Evening summary at the user-chosen time: what is still pending today. */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as HabitApplication
        val pending = app.container.repository.getPendingOn(LocalDate.now())
        if (pending.isEmpty()) return Result.success()

        postPendingNotification(
            context = applicationContext,
            pending = pending,
            channelId = HabitApplication.REMINDER_CHANNEL_ID,
            notificationId = NOTIFICATION_ID,
            title = applicationContext.getString(R.string.reminder_notification_title),
        )
        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}

/**
 * Builds and posts a notification listing pending habits with their progress,
 * e.g. "💧 Drink water (3/8)". Shared by the daily reminder and the nudges.
 */
internal fun postPendingNotification(
    context: Context,
    pending: List<PendingHabit>,
    channelId: String,
    notificationId: Int,
    title: String,
) {
    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return

    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val lines = pending.map {
        context.getString(
            R.string.habit_progress_line,
            it.habit.emoji,
            it.habit.name,
            it.count,
            it.habit.dailyTarget,
        )
    }
    val contentText = if (pending.size == 1) {
        lines.first()
    } else {
        context.resources.getQuantityString(
            R.plurals.reminder_notification_text,
            pending.size,
            pending.size,
        )
    }

    val notification: Notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(contentText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    runCatching { manager.notify(notificationId, notification) }
}
