package io.github.aadi1607.tiffintracker.domain

import io.github.aadi1607.tiffintracker.data.db.EntryStatus
import io.github.aadi1607.tiffintracker.data.db.Payment
import io.github.aadi1607.tiffintracker.data.db.TiffinEntry

enum class BillStatus { PAID, PARTIAL, PENDING }

/** Everything the billing screen needs for one user in one cycle. */
data class UserBill(
    val userId: Long,
    val tiffinCount: Int,
    val billPaise: Long,
    val paidPaise: Long,
    /** Unpaid balance from all cycles before this one (can be negative if overpaid). */
    val carryForwardPaise: Long,
) {
    val balancePaise: Long get() = billPaise - paidPaise
    val totalDuePaise: Long get() = billPaise + carryForwardPaise - paidPaise
    val status: BillStatus
        get() = when {
            billPaise > 0 && paidPaise >= billPaise -> BillStatus.PAID
            paidPaise > 0 -> BillStatus.PARTIAL
            billPaise == 0L -> BillStatus.PAID
            else -> BillStatus.PENDING
        }
}

object BillCalculator {

    /** Tiffins actually taken (skipped rows never count, whatever their quantity). */
    fun tiffinCount(entries: List<TiffinEntry>): Int =
        entries.filter { it.status == EntryStatus.TAKEN }.sumOf { it.quantity }

    fun billPaise(entries: List<TiffinEntry>, pricePaise: Long): Long =
        tiffinCount(entries) * pricePaise

    /**
     * Computes the bill for [userId] in [cycle], including carry-forward:
     * everything billed before the cycle started minus everything ever paid
     * toward those earlier cycles.
     *
     * @param allEntries every entry for this user (any range containing history works)
     * @param allPayments every payment for this user
     */
    fun userBill(
        userId: Long,
        pricePaise: Long,
        cycle: BillingCycle,
        allEntries: List<TiffinEntry>,
        allPayments: List<Payment>,
    ): UserBill {
        val own = allEntries.filter { it.userId == userId }
        val ownPayments = allPayments.filter { it.userId == userId }

        val inCycle = own.filter { it.epochDay in cycle.startEpochDay..cycle.endEpochDay }
        val bill = billPaise(inCycle, pricePaise)
        val paid = ownPayments
            .filter { it.cycleStartEpochDay == cycle.startEpochDay }
            .sumOf { it.amountPaise }

        val billedBefore = billPaise(own.filter { it.epochDay < cycle.startEpochDay }, pricePaise)
        val paidBefore = ownPayments
            .filter { it.cycleStartEpochDay < cycle.startEpochDay }
            .sumOf { it.amountPaise }

        return UserBill(
            userId = userId,
            tiffinCount = tiffinCount(inCycle),
            billPaise = bill,
            paidPaise = paid,
            carryForwardPaise = billedBefore - paidBefore,
        )
    }

    /** Formats paise as e.g. "₹1,240" or "₹62.50" (decimals only when needed). */
    fun formatPaise(paise: Long, symbol: String = "₹"): String {
        val negative = paise < 0
        val abs = if (negative) -paise else paise
        val rupees = abs / 100
        val rest = abs % 100
        val grouped = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(rupees)
        val body = if (rest == 0L) grouped else "$grouped.${rest.toString().padStart(2, '0')}"
        return (if (negative) "-" else "") + symbol + body
    }
}
