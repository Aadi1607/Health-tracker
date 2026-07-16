package io.github.aadi1607.tiffintracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

const val MAX_USERS = 5

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** ARGB color used as this user's tag across the app. */
    val colorArgb: Long,
    /** Price of one tiffin in paise (₹60.00 == 6000) so money math stays exact. */
    val pricePaise: Long,
    val sortOrder: Int = 0,
)
