package com.tiffintracker.di

import android.content.Context
import androidx.room.Room
import com.tiffintracker.data.db.MealEntryDao
import com.tiffintracker.data.db.PaymentDao
import com.tiffintracker.data.db.ProviderDao
import com.tiffintracker.data.db.TiffinDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TiffinDatabase =
        Room.databaseBuilder(
            context,
            TiffinDatabase::class.java,
            "tiffin_tracker.db"
        ).build()

    @Provides
    @Singleton
    fun provideProviderDao(db: TiffinDatabase): ProviderDao = db.providerDao()

    @Provides
    @Singleton
    fun provideMealEntryDao(db: TiffinDatabase): MealEntryDao = db.mealEntryDao()

    @Provides
    @Singleton
    fun providePaymentDao(db: TiffinDatabase): PaymentDao = db.paymentDao()
}
