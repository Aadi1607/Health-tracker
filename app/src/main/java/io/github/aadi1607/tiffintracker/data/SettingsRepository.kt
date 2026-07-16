package io.github.aadi1607.tiffintracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val cycleStartDay: Int = 1,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 30,
    val paymentReminderDays: Int = 3,
    val currencySymbol: String = "₹",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val cycleStartDay = intPreferencesKey("cycle_start_day")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val paymentReminderDays = intPreferencesKey("payment_reminder_days")
        val currencySymbol = stringPreferencesKey("currency_symbol")
        val themeMode = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            cycleStartDay = (prefs[Keys.cycleStartDay] ?: 1).coerceIn(1, 28),
            reminderEnabled = prefs[Keys.reminderEnabled] ?: true,
            reminderHour = prefs[Keys.reminderHour] ?: 21,
            reminderMinute = prefs[Keys.reminderMinute] ?: 30,
            paymentReminderDays = prefs[Keys.paymentReminderDays] ?: 3,
            currencySymbol = prefs[Keys.currencySymbol] ?: "₹",
            themeMode = prefs[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setCycleStartDay(day: Int) =
        context.dataStore.edit { it[Keys.cycleStartDay] = day.coerceIn(1, 28) }

    suspend fun setReminderEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.reminderEnabled] = enabled }

    suspend fun setReminderTime(hour: Int, minute: Int) = context.dataStore.edit {
        it[Keys.reminderHour] = hour
        it[Keys.reminderMinute] = minute
    }

    suspend fun setPaymentReminderDays(days: Int) =
        context.dataStore.edit { it[Keys.paymentReminderDays] = days.coerceIn(0, 28) }

    suspend fun setCurrencySymbol(symbol: String) =
        context.dataStore.edit { it[Keys.currencySymbol] = symbol.ifBlank { "₹" } }

    suspend fun setThemeMode(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.themeMode] = mode.name }
}
