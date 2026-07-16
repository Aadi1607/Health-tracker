package io.github.aadi1607.tiffintracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.aadi1607.tiffintracker.TiffinApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Re-arms the WorkManager reminder chains whenever their computed delays
 * could have become stale: after a reboot, an app update, a timezone change,
 * or a manual clock change.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ReminderScheduler.scheduleAll(context.applicationContext, replace = true)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}

/** Handles "Mark taken" / "Mark skipped" taps on the daily reminder. */
class QuickLogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = TiffinApplication.from(context.applicationContext).repository
                when (action) {
                    ACTION_MARK_TAKEN -> repository.markDayTakenForAll(LocalDate.now())
                    ACTION_MARK_SKIPPED -> repository.markDaySkippedForAll(LocalDate.now())
                }
                NotificationHelper.cancelDailyReminder(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_TAKEN = "io.github.aadi1607.tiffintracker.MARK_TAKEN"
        const val ACTION_MARK_SKIPPED = "io.github.aadi1607.tiffintracker.MARK_SKIPPED"
    }
}
