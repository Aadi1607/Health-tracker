package io.github.aadi1607.habittracker.data.db

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
)
