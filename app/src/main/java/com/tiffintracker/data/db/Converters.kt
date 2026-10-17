package com.tiffintracker.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMealType(value: MealType): String = value.name

    @TypeConverter
    fun toMealType(value: String): MealType = MealType.valueOf(value)

    @TypeConverter
    fun fromMealStatus(value: MealStatus): String = value.name

    @TypeConverter
    fun toMealStatus(value: String): MealStatus = MealStatus.valueOf(value)

    @TypeConverter
    fun fromPaymentMode(value: PaymentMode): String = value.name

    @TypeConverter
    fun toPaymentMode(value: String): PaymentMode = PaymentMode.valueOf(value)
}
