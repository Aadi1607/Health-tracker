package io.github.aadi1607.habittracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val emoji: String,
    /** Packed ARGB color, e.g. 0xFFFF6B6B. */
    val color: Long,
    /** Epoch millis when the habit was created. */
    val createdAt: Long,
    /** How many times per period this habit should be done (e.g. 8 glasses a day, 3 gym visits a week). */
    @ColumnInfo(defaultValue = "1")
    val dailyTarget: Int = 1,
    /** [PERIOD_DAILY] or [PERIOD_WEEKLY] — the period [dailyTarget] applies to. */
    @ColumnInfo(defaultValue = PERIOD_DAILY)
    val goalPeriod: String = PERIOD_DAILY,
    /** Manual ordering on the home screen; lower comes first. */
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Long = 0,
) {
    val isWeekly: Boolean get() = goalPeriod == PERIOD_WEEKLY

    companion object {
        const val PERIOD_DAILY = "daily"
        const val PERIOD_WEEKLY = "weekly"
    }
}
