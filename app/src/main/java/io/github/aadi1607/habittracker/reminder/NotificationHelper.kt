package io.github.aadi1607.habittracker.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.aadi1607.habittracker.HabitApplication
import io.github.aadi1607.habittracker.MainActivity
import io.github.aadi1607.habittracker.R
import io.github.aadi1607.habittracker.data.PendingHabit

object NotificationHelper {

    const val DAILY_NOTIFICATION_ID = 1001
    const val NUDGE_NOTIFICATION_ID = 1002

    /**
     * Posts a notification listing pending habits with their progress, plus
     * "+1" quick actions (up to three) that log a completion without opening
     * the app.
     */
    fun postPending(
        context: Context,
        pending: List<PendingHabit>,
        notificationId: Int,
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val channelId = if (notificationId == DAILY_NOTIFICATION_ID) {
            HabitApplication.REMINDER_CHANNEL_ID
        } else {
            HabitApplication.NUDGE_CHANNEL_ID
        }
        val title = if (notificationId == DAILY_NOTIFICATION_ID) {
            context.getString(R.string.reminder_notification_title)
        } else {
            context.getString(R.string.nudge_notification_title)
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
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

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)

        pending.take(3).forEach { item ->
            val logIntent = Intent(context, LogHabitReceiver::class.java)
                .setAction(LogHabitReceiver.ACTION_LOG)
                .putExtra(LogHabitReceiver.EXTRA_HABIT_ID, item.habit.id)
                .putExtra(LogHabitReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            val logPendingIntent = PendingIntent.getBroadcast(
                context,
                item.habit.id.toInt() + 100,
                logIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                0,
                context.getString(R.string.log_one, item.habit.emoji, item.habit.name),
                logPendingIntent,
            )
        }

        runCatching { manager.notify(notificationId, builder.build()) }
    }

    fun cancel(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
