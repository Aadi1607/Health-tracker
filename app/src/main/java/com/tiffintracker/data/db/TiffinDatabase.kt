package com.tiffintracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Provider::class, MealEntry::class, Payment::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TiffinDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        const val DATABASE_NAME = "tiffin_tracker.db"
    }
}
