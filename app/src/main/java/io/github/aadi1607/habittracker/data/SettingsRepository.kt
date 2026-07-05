package io.github.aadi1607.habittracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ReminderSettings(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    /** Extra reminders during the day while habits are pending. */
    val nudgesEnabled: Boolean,
    /** Hours between nudges. */
    val nudgeIntervalHours: Int,
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val NUDGES_ENABLED = booleanPreferencesKey("nudges_enabled")
        val NUDGE_INTERVAL_HOURS = intPreferencesKey("nudge_interval_hours")
    }

    val dynamicColor: Flow<Boolean> =
        dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: false }

    val reminder: Flow<ReminderSettings> =
        dataStore.data.map {
            ReminderSettings(
                enabled = it[Keys.REMINDER_ENABLED] ?: false,
                hour = it[Keys.REMINDER_HOUR] ?: DEFAULT_HOUR,
                minute = it[Keys.REMINDER_MINUTE] ?: DEFAULT_MINUTE,
                nudgesEnabled = it[Keys.NUDGES_ENABLED] ?: false,
                nudgeIntervalHours = it[Keys.NUDGE_INTERVAL_HOURS] ?: DEFAULT_NUDGE_INTERVAL_HOURS,
            )
        }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun setNudgesEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NUDGES_ENABLED] = enabled }
    }

    suspend fun setNudgeIntervalHours(hours: Int) {
        dataStore.edit { it[Keys.NUDGE_INTERVAL_HOURS] = hours }
    }

    companion object {
        const val DEFAULT_HOUR = 20
        const val DEFAULT_MINUTE = 0
        const val DEFAULT_NUDGE_INTERVAL_HOURS = 2
    }
}
