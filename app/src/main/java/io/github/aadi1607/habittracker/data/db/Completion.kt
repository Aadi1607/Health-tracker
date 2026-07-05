package io.github.aadi1607.habittracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("habitId"), Index("date")],
)
data class Completion(
    val habitId: Long,
    /** ISO-8601 local date, e.g. "2026-07-05". Sorts chronologically as text. */
    val date: String,
    /** How many times the habit was logged on this date. */
    @ColumnInfo(defaultValue = "1")
    val count: Int = 1,
)
