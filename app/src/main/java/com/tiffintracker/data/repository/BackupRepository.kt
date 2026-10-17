package com.tiffintracker.data.repository

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tiffintracker.data.db.MealEntry
import com.tiffintracker.data.db.MealEntryDao
import com.tiffintracker.data.db.Payment
import com.tiffintracker.data.db.PaymentDao
import com.tiffintracker.data.db.Provider
import com.tiffintracker.data.db.ProviderDao
import com.tiffintracker.data.db.TiffinDatabase
import javax.inject.Inject
import javax.inject.Singleton

data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val providers: List<Provider>,
    val mealEntries: List<MealEntry>,
    val payments: List<Payment>
)

@Singleton
class BackupRepository @Inject constructor(
    private val database: TiffinDatabase,
    private val providerDao: ProviderDao,
    private val mealEntryDao: MealEntryDao,
    private val paymentDao: PaymentDao
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToJson(): String {
        val providers = providerDao.getAllProvidersList()
        val entries = mealEntryDao.getAllEntries()
        val payments = paymentDao.getAllPayments()
        val backup = BackupData(
            providers = providers,
            mealEntries = entries,
            payments = payments
        )
        return gson.toJson(backup)
    }

    suspend fun importFromJson(json: String): Boolean {
        return try {
            val backup = gson.fromJson(json, BackupData::class.java)
            database.clearAllTables()
            for (provider in backup.providers) {
                providerDao.insert(provider)
            }
            for (entry in backup.mealEntries) {
                mealEntryDao.upsert(entry)
            }
            for (payment in backup.payments) {
                paymentDao.insert(payment)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
