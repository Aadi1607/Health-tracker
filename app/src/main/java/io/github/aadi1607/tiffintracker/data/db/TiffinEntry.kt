package io.github.aadi1607.tiffintracker.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MealType { LUNCH, DINNER, EXTRA }

enum class EntryStatus { TAKEN, SKIPPED }

/**
 * One row per user + date + meal slot. Dates are stored as
 * [java.time.LocalDate.toEpochDay] so they are immune to timezone changes.
 * The unique index + REPLACE insert makes logging an upsert, so re-tapping
 * a meal can never create duplicates.
 */
@Entity(
    tableName = "tiffin_entries",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["userId", "epochDay", "mealType"], unique = true),
        Index(value = ["epochDay"]),
    ],
)
data class TiffinEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val epochDay: Long,
    val mealType: MealType,
    val quantity: Int = 1,
    val status: EntryStatus = EntryStatus.TAKEN,
    val note: String = "",
)
