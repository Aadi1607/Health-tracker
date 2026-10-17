package com.tiffintracker.di

import android.content.Context
import com.tiffintracker.data.db.MealEntryDao
import com.tiffintracker.data.db.PaymentDao
import com.tiffintracker.data.db.ProviderDao
import com.tiffintracker.data.db.TiffinDatabase
import com.tiffintracker.data.repository.BackupRepository
import com.tiffintracker.data.repository.SettingsRepository
import com.tiffintracker.data.repository.TiffinRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTiffinRepository(
        providerDao: ProviderDao,
        mealEntryDao: MealEntryDao,
        paymentDao: PaymentDao
    ): TiffinRepository = TiffinRepository(providerDao, mealEntryDao, paymentDao)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepository(context)

    @Provides
    @Singleton
    fun provideBackupRepository(
        database: TiffinDatabase,
        providerDao: ProviderDao,
        mealEntryDao: MealEntryDao,
        paymentDao: PaymentDao
    ): BackupRepository = BackupRepository(database, providerDao, mealEntryDao, paymentDao)
}
