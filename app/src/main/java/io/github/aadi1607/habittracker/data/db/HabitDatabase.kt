package io.github.aadi1607.habittracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Habit::class, Completion::class],
    version = 3,
    exportSchema = false,
)
abstract class HabitDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN dailyTarget INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE completions ADD COLUMN count INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN goalPeriod TEXT NOT NULL DEFAULT 'daily'")
                db.execSQL("ALTER TABLE habits ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE habits SET sortOrder = createdAt")
            }
        }

        fun build(context: Context): HabitDatabase =
            Room.databaseBuilder(context, HabitDatabase::class.java, "habits.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
