package io.github.aadi1607.tiffintracker.data.backup

import android.content.Context
import android.net.Uri
import io.github.aadi1607.tiffintracker.data.TiffinRepository
import io.github.aadi1607.tiffintracker.data.db.EntryStatus
import io.github.aadi1607.tiffintracker.data.db.MealType
import io.github.aadi1607.tiffintracker.data.db.Payment
import io.github.aadi1607.tiffintracker.data.db.TiffinEntry
import io.github.aadi1607.tiffintracker.data.db.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupUser(
    val id: Long,
    val name: String,
    val colorArgb: Long,
    val pricePaise: Long,
    val sortOrder: Int,
)

@Serializable
data class BackupEntry(
    val userId: Long,
    val epochDay: Long,
    val mealType: String,
    val quantity: Int,
    val status: String,
    val note: String,
)

@Serializable
data class BackupPayment(
    val userId: Long,
    val cycleStartEpochDay: Long,
    val amountPaise: Long,
    val epochDay: Long,
)

@Serializable
data class BackupData(
    val version: Int = 1,
    val users: List<BackupUser>,
    val entries: List<BackupEntry>,
    val payments: List<BackupPayment>,
)

/** Whole-database JSON backup/restore through the Storage Access Framework. */
class BackupManager(private val context: Context, private val repository: TiffinRepository) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun exportTo(uri: Uri) {
        val (users, entries, payments) = repository.snapshotForBackup()
        val data = BackupData(
            users = users.map { BackupUser(it.id, it.name, it.colorArgb, it.pricePaise, it.sortOrder) },
            entries = entries.map {
                BackupEntry(it.userId, it.epochDay, it.mealType.name, it.quantity, it.status.name, it.note)
            },
            payments = payments.map {
                BackupPayment(it.userId, it.cycleStartEpochDay, it.amountPaise, it.epochDay)
            },
        )
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(json.encodeToString(BackupData.serializer(), data).toByteArray())
        } ?: error("Could not open $uri for writing")
    }

    suspend fun importFrom(uri: Uri) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: error("Could not open $uri for reading")
        val data = json.decodeFromString(BackupData.serializer(), text)
        repository.restoreFromBackup(
            users = data.users.map {
                User(it.id, it.name, it.colorArgb, it.pricePaise, it.sortOrder)
            },
            entries = data.entries.map {
                TiffinEntry(
                    userId = it.userId,
                    epochDay = it.epochDay,
                    mealType = MealType.valueOf(it.mealType),
                    quantity = it.quantity,
                    status = EntryStatus.valueOf(it.status),
                    note = it.note,
                )
            },
            payments = data.payments.map {
                Payment(
                    userId = it.userId,
                    cycleStartEpochDay = it.cycleStartEpochDay,
                    amountPaise = it.amountPaise,
                    epochDay = it.epochDay,
                )
            },
        )
    }
}
