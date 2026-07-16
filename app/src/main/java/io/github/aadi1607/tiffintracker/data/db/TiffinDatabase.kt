package io.github.aadi1607.tiffintracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun mealTypeToString(value: MealType): String = value.name

    @TypeConverter
    fun stringToMealType(value: String): MealType = MealType.valueOf(value)

    @TypeConverter
    fun entryStatusToString(value: EntryStatus): String = value.name

    @TypeConverter
    fun stringToEntryStatus(value: String): EntryStatus = EntryStatus.valueOf(value)
}

@Database(
    entities = [User::class, TiffinEntry::class, Payment::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TiffinDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun tiffinEntryDao(): TiffinEntryDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var instance: TiffinDatabase? = null

        fun get(context: Context): TiffinDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TiffinDatabase::class.java,
                    "tiffin.db",
                ).build().also { instance = it }
            }
    }
}
