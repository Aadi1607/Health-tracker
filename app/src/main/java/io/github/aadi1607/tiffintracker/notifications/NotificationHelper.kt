package io.github.aadi1607.tiffintracker.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import io.github.aadi1607.tiffintracker.MainActivity
import io.github.aadi1607.tiffintracker.R

object NotificationHelper {

    const val CHANNEL_REMINDERS = "daily_reminders"
    const val CHANNEL_BILLING = "billing"

    const val NOTIF_ID_DAILY = 100
    const val NOTIF_ID_SUMMARY = 200
    const val NOTIF_ID_PAYMENT = 300

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_reminders_description)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BILLING,
                context.getString(R.string.channel_billing_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_billing_description)
            }
        )
    }

    private fun canNotify(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun contentIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun showDailyReminder(context: Context, unloggedNames: List<String>) {
        if (!canNotify(context)) return

        fun action(intentAction: String, label: String, requestCode: Int): NotificationCompat.Action {
            val intent = Intent(context, QuickLogReceiver::class.java).setAction(intentAction)
            val pending = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Action(0, label, pending)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_daily_title))
            .setContentText(
                context.getString(R.string.notif_daily_body, unloggedNames.joinToString(", "))
            )
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .addAction(
                action(
                    QuickLogReceiver.ACTION_MARK_TAKEN,
                    context.getString(R.string.notif_action_mark_taken),
                    1,
                )
            )
            .addAction(
                action(
                    QuickLogReceiver.ACTION_MARK_SKIPPED,
                    context.getString(R.string.notif_action_mark_skipped),
                    2,
                )
            )
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID_DAILY, notification)
    }

    fun cancelDailyReminder(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIF_ID_DAILY)
    }

    fun showCycleSummary(context: Context, lines: List<String>) {
        if (!canNotify(context)) return
        val style = NotificationCompat.InboxStyle()
        lines.forEach { style.addLine(it) }
        val notification = NotificationCompat.Builder(context, CHANNEL_BILLING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_summary_title))
            .setContentText(lines.firstOrNull() ?: "")
            .setStyle(style)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID_SUMMARY, notification)
    }

    fun showPaymentReminder(context: Context, userName: String, amountText: String, notifId: Int) {
        if (!canNotify(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_BILLING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_payment_title))
            .setContentText(context.getString(R.string.notif_payment_body, userName, amountText))
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(notifId, notification)
    }
}
