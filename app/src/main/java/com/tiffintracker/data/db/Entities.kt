package com.tiffintracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MealType { BREAKFAST, LUNCH, DINNER }

enum class MealStatus { RECEIVED, SKIPPED, NOT_ORDERED }

enum class PaymentMode { CASH, UPI, OTHER }

@Entity(tableName = "providers")
data class Provider(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    @ColumnInfo(name = "breakfast_price") val breakfastPrice: Double = 0.0,
    @ColumnInfo(name = "lunch_price") val lunchPrice: Double = 0.0,
    @ColumnInfo(name = "dinner_price") val dinnerPrice: Double = 0.0,
    @ColumnInfo(name = "offers_breakfast") val offersBreakfast: Boolean = false,
    @ColumnInfo(name = "offers_lunch") val offersLunch: Boolean = true,
    @ColumnInfo(name = "offers_dinner") val offersDinner: Boolean = true,
    @ColumnInfo(name = "billing_cycle_start_day") val billingCycleStartDay: Int = 1,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "meal_entries",
    foreignKeys = [
        ForeignKey(
            entity = Provider::class,
            parentColumns = ["id"],
            childColumns = ["provider_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["provider_id", "date", "meal_type"], unique = true),
        Index(value = ["provider_id"]),
        Index(value = ["date"])
    ]
)
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "provider_id") val providerId: Long,
    val date: String,
    @ColumnInfo(name = "meal_type") val mealType: String,
    val status: String,
    val quantity: Int = 1,
    val note: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Provider::class,
            parentColumns = ["id"],
            childColumns = ["provider_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["provider_id"]),
        Index(value = ["date"])
    ]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "provider_id") val providerId: Long,
    val amount: Double,
    val date: String,
    val mode: String,
    val note: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
