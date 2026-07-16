package io.github.aadi1607.tiffintracker.data

import io.github.aadi1607.tiffintracker.data.db.EntryStatus
import io.github.aadi1607.tiffintracker.data.db.MealType
import io.github.aadi1607.tiffintracker.data.db.Payment
import io.github.aadi1607.tiffintracker.data.db.TiffinDatabase
import io.github.aadi1607.tiffintracker.data.db.TiffinEntry
import io.github.aadi1607.tiffintracker.data.db.User
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Tri-state of a lunch/dinner slot in the day editor. */
enum class MealState { NONE, TAKEN, SKIPPED }

/** Editable snapshot of one user's log for one day. */
data class DayLog(
    val lunch: MealState = MealState.NONE,
    val dinner: MealState = MealState.NONE,
    val extraQuantity: Int = 0,
    val note: String = "",
) {
    val isSkippedDay: Boolean =
        lunch == MealState.SKIPPED && dinner == MealState.SKIPPED
    val isLogged: Boolean =
        lunch != MealState.NONE || dinner != MealState.NONE || extraQuantity > 0
    val takenCount: Int =
        (if (lunch == MealState.TAKEN) 1 else 0) +
            (if (dinner == MealState.TAKEN) 1 else 0) +
            extraQuantity

    companion object {
        fun fromEntries(entries: List<TiffinEntry>): DayLog {
            fun stateOf(meal: MealType): MealState =
                when (entries.firstOrNull { it.mealType == meal }?.status) {
                    EntryStatus.TAKEN -> MealState.TAKEN
                    EntryStatus.SKIPPED -> MealState.SKIPPED
                    null -> MealState.NONE
                }
            return DayLog(
                lunch = stateOf(MealType.LUNCH),
                dinner = stateOf(MealType.DINNER),
                extraQuantity = entries
                    .filter { it.mealType == MealType.EXTRA && it.status == EntryStatus.TAKEN }
                    .sumOf { it.quantity },
                note = entries.firstOrNull { it.note.isNotBlank() }?.note ?: "",
            )
        }
    }
}

class TiffinRepository(private val db: TiffinDatabase) {

    private val userDao = db.userDao()
    private val entryDao = db.tiffinEntryDao()
    private val paymentDao = db.paymentDao()

    // Users -----------------------------------------------------------------

    val users: Flow<List<User>> = userDao.observeUsers()

    suspend fun getUsers(): List<User> = userDao.getUsers()

    suspend fun addUser(user: User) = userDao.insert(user)

    suspend fun updateUser(user: User) = userDao.update(user)

    suspend fun deleteUser(userId: Long) = userDao.delete(userId)

    suspend fun seedDefaultUsersIfEmpty(names: List<String>, colors: List<Long>, pricePaise: Long) {
        if (userDao.count() == 0) {
            userDao.insertAll(
                names.mapIndexed { index, name ->
                    User(
                        name = name,
                        colorArgb = colors[index % colors.size],
                        pricePaise = pricePaise,
                        sortOrder = index,
                    )
                }
            )
        }
    }

    // Entries ---------------------------------------------------------------

    fun entriesForDay(date: LocalDate): Flow<List<TiffinEntry>> =
        entryDao.observeForDay(date.toEpochDay())

    fun entriesForRange(start: LocalDate, end: LocalDate): Flow<List<TiffinEntry>> =
        entryDao.observeForRange(start.toEpochDay(), end.toEpochDay())

    fun allEntries(): Flow<List<TiffinEntry>> = entryDao.observeAll()

    suspend fun getEntriesForRange(start: LocalDate, end: LocalDate): List<TiffinEntry> =
        entryDao.getForRange(start.toEpochDay(), end.toEpochDay())

    /**
     * Persists a full [DayLog] for one user + date. Lunch/dinner slots set to
     * NONE are deleted; everything else is upserted (duplicate-safe thanks to
     * the unique index). The note is stored on every written row.
     */
    suspend fun saveDayLog(userId: Long, date: LocalDate, log: DayLog) {
        val day = date.toEpochDay()
        val note = log.note.trim()

        suspend fun writeMeal(meal: MealType, state: MealState) {
            when (state) {
                MealState.NONE -> entryDao.delete(userId, day, meal)
                MealState.TAKEN, MealState.SKIPPED -> entryDao.upsert(
                    TiffinEntry(
                        userId = userId,
                        epochDay = day,
                        mealType = meal,
                        quantity = 1,
                        status = if (state == MealState.TAKEN) EntryStatus.TAKEN else EntryStatus.SKIPPED,
                        note = note,
                    )
                )
            }
        }

        writeMeal(MealType.LUNCH, log.lunch)
        writeMeal(MealType.DINNER, log.dinner)

        if (log.extraQuantity > 0) {
            entryDao.upsert(
                TiffinEntry(
                    userId = userId,
                    epochDay = day,
                    mealType = MealType.EXTRA,
                    quantity = log.extraQuantity,
                    status = EntryStatus.TAKEN,
                    note = note,
                )
            )
        } else {
            entryDao.delete(userId, day, MealType.EXTRA)
        }
    }

    /** Users who have nothing logged for [date]; used by the daily reminder. */
    suspend fun usersNotLogged(date: LocalDate): List<User> =
        userDao.getUsers().filter { user ->
            entryDao.countForUserDay(user.id, date.toEpochDay()) == 0
        }

    /** Quick action from the notification: both meals taken for every user. */
    suspend fun markDayTakenForAll(date: LocalDate) {
        val day = date.toEpochDay()
        val entries = userDao.getUsers().flatMap { user ->
            listOf(MealType.LUNCH, MealType.DINNER).map { meal ->
                TiffinEntry(userId = user.id, epochDay = day, mealType = meal)
            }
        }
        entryDao.upsertAll(entries)
    }

    /** Quick action from the notification: whole day skipped for every user. */
    suspend fun markDaySkippedForAll(date: LocalDate) {
        val day = date.toEpochDay()
        val entries = userDao.getUsers().flatMap { user ->
            listOf(MealType.LUNCH, MealType.DINNER).map { meal ->
                TiffinEntry(
                    userId = user.id,
                    epochDay = day,
                    mealType = meal,
                    status = EntryStatus.SKIPPED,
                )
            }
        }
        entryDao.upsertAll(entries)
        userDao.getUsers().forEach { user ->
            entryDao.delete(user.id, day, MealType.EXTRA)
        }
    }

    // Payments --------------------------------------------------------------

    val payments: Flow<List<Payment>> = paymentDao.observeAll()

    suspend fun getPayments(): List<Payment> = paymentDao.getAll()

    suspend fun addPayment(payment: Payment) = paymentDao.insert(payment)

    suspend fun deletePayment(payment: Payment) = paymentDao.delete(payment)

    // Maintenance -----------------------------------------------------------

    suspend fun deleteEntriesInRange(start: LocalDate, end: LocalDate) =
        entryDao.deleteRange(start.toEpochDay(), end.toEpochDay())

    suspend fun clearAllData() {
        db.tiffinEntryDao().deleteAll()
        db.paymentDao().deleteAll()
        db.userDao().deleteAll()
    }

    // Backup ----------------------------------------------------------------

    suspend fun snapshotForBackup(): Triple<List<User>, List<TiffinEntry>, List<Payment>> =
        Triple(userDao.getUsers(), entryDao.getAll(), paymentDao.getAll())

    suspend fun restoreFromBackup(
        users: List<User>,
        entries: List<TiffinEntry>,
        payments: List<Payment>,
    ) {
        clearAllData()
        userDao.insertAll(users)
        entryDao.upsertAll(entries)
        paymentDao.insertAll(payments)
    }
}
