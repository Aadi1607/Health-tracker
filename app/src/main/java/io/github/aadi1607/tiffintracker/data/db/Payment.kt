package io.github.aadi1607.tiffintracker.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A payment made by a user against a billing cycle. A cycle's Paid /
 * Partially paid / Pending status is derived by comparing the sum of its
 * payments against the computed bill, so it can never drift out of sync.
 */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["userId", "cycleStartEpochDay"])],
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    /** Epoch day of the billing cycle's first day; identifies the cycle. */
    val cycleStartEpochDay: Long,
    val amountPaise: Long,
    /** Date the payment was made, as epoch day. */
    val epochDay: Long,
)
