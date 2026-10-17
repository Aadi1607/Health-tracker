package com.tiffintracker.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tiffintracker.data.db.MealEntry
import com.tiffintracker.data.db.MealStatus
import com.tiffintracker.data.db.MealType
import com.tiffintracker.data.db.MealEntryDao
import com.tiffintracker.data.db.ProviderDao
import com.tiffintracker.data.repository.SettingsRepository
import com.tiffintracker.util.today
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var providerDao: ProviderDao
    @Inject lateinit var mealEntryDao: MealEntryDao
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MARK_RECEIVED -> handleMarkReceived(context)
            Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(context)
        }
    }

    private fun handleMarkReceived(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val providers = providerDao.getActiveProviders().first()
                val dateStr = today()
                for (provider in providers) {
                    val mealTypes = buildList {
                        if (provider.offersBreakfast) add(MealType.BREAKFAST)
                        if (provider.offersLunch) add(MealType.LUNCH)
                        if (provider.offersDinner) add(MealType.DINNER)
                    }
                    for (mealType in mealTypes) {
                        val existing = mealEntryDao.getEntryForProviderDateMeal(
                            provider.id, dateStr, mealType.name
                        )
                        if (existing == null) {
                            mealEntryDao.insert(
                                MealEntry(
                                    providerId = provider.id,
                                    date = dateStr,
                                    mealType = mealType.name,
                                    status = MealStatus.RECEIVED.name,
                                    quantity = 1
                                )
                            )
                        }
                    }
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(ReminderWorker.NOTIFICATION_ID)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleBootCompleted(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = settingsRepository.settings.first()
                if (settings.reminderEnabled) {
                    ReminderScheduler.scheduleDaily(
                        context, settings.reminderHour, settings.reminderMinute
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_RECEIVED = "com.tiffintracker.ACTION_MARK_RECEIVED"
    }
}
