package io.github.aadi1607.tiffintracker

import android.app.Application
import android.content.Context
import io.github.aadi1607.tiffintracker.data.SettingsRepository
import io.github.aadi1607.tiffintracker.data.TiffinRepository
import io.github.aadi1607.tiffintracker.data.backup.BackupManager
import io.github.aadi1607.tiffintracker.data.db.TiffinDatabase
import io.github.aadi1607.tiffintracker.notifications.NotificationHelper
import io.github.aadi1607.tiffintracker.notifications.ReminderScheduler
import io.github.aadi1607.tiffintracker.ui.theme.UserColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TiffinApplication : Application() {

    val database: TiffinDatabase by lazy { TiffinDatabase.get(this) }
    val repository: TiffinRepository by lazy { TiffinRepository(database) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val backupManager: BackupManager by lazy { BackupManager(this, repository) }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        applicationScope.launch {
            repository.seedDefaultUsersIfEmpty(
                names = listOf("Me", "Rahul"),
                colors = UserColors.palette,
                pricePaise = 6000, // ₹60 default
            )
            // KEEP policy: don't reset a pending reminder on every app launch.
            ReminderScheduler.scheduleAll(this@TiffinApplication, replace = false)
        }
    }

    companion object {
        fun from(context: Context): TiffinApplication =
            context.applicationContext as TiffinApplication
    }
}
