package io.github.aadi1607.tiffintracker.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.aadi1607.tiffintracker.TiffinApplication
import io.github.aadi1607.tiffintracker.domain.BillCalculator
import io.github.aadi1607.tiffintracker.domain.BillingCycle
import java.time.LocalDate

/**
 * Fires the "Did you log today's tiffin?" notification if anyone is
 * unlogged, then re-arms itself for tomorrow at the configured time.
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = TiffinApplication.from(applicationContext)
        val settings = app.settingsRepository.current()
        try {
            if (settings.reminderEnabled) {
                val unlogged = app.repository.usersNotLogged(LocalDate.now())
                if (unlogged.isNotEmpty()) {
                    NotificationHelper.showDailyReminder(
                        applicationContext,
                        unlogged.map { it.name },
                    )
                }
            }
        } finally {
            // Always re-arm, even if today's check failed, so the chain never dies.
            ReminderScheduler.scheduleDailyReminder(
                applicationContext,
                settings.reminderHour,
                settings.reminderMinute,
                replace = true,
            )
        }
        return Result.success()
    }
}

/**
 * Runs once a day in the evening. On the last day of the billing cycle it
 * posts the per-user summary; [paymentReminderDays] after a cycle ends it
 * nags about any unpaid balance. Re-arms itself for the next day.
 */
class BillingCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = TiffinApplication.from(applicationContext)
        try {
            val settings = app.settingsRepository.current()
            val today = LocalDate.now()
            val cycle = BillingCycle.cycleFor(today, settings.cycleStartDay)
            val users = app.repository.getUsers()
            if (users.isEmpty()) return Result.success()

            if (today == cycle.end) {
                val entries = app.repository.getEntriesForRange(cycle.start, cycle.end)
                val lines = users.map { user ->
                    val own = entries.filter { it.userId == user.id }
                    val count = BillCalculator.tiffinCount(own)
                    val amount = BillCalculator.formatPaise(
                        count * user.pricePaise, settings.currencySymbol,
                    )
                    "${user.name}: $count tiffins · $amount"
                }
                NotificationHelper.showCycleSummary(applicationContext, lines)
            }

            val previous = cycle.previous()
            if (settings.paymentReminderDays > 0 &&
                today == previous.end.plusDays(settings.paymentReminderDays.toLong())
            ) {
                val allEntries = app.repository.getEntriesForRange(
                    LocalDate.ofEpochDay(0), previous.end,
                )
                val allPayments = app.repository.getPayments()
                users.forEachIndexed { index, user ->
                    val bill = BillCalculator.userBill(
                        user.id, user.pricePaise, previous, allEntries, allPayments,
                    )
                    if (bill.totalDuePaise > 0) {
                        NotificationHelper.showPaymentReminder(
                            applicationContext,
                            user.name,
                            BillCalculator.formatPaise(bill.totalDuePaise, settings.currencySymbol),
                            NotificationHelper.NOTIF_ID_PAYMENT + index,
                        )
                    }
                }
            }
        } finally {
            ReminderScheduler.scheduleBillingCheck(applicationContext, replace = true)
        }
        return Result.success()
    }
}
