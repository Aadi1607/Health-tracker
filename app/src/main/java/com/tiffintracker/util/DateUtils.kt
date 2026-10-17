package com.tiffintracker.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

fun today(): String = LocalDate.now().format(dateFormatter)

fun formatDate(date: LocalDate): String = date.format(dateFormatter)

fun parseDate(str: String): LocalDate = LocalDate.parse(str, dateFormatter)

fun getMonthRange(yearMonth: YearMonth): Pair<String, String> {
    val start = yearMonth.atDay(1)
    val end = yearMonth.atEndOfMonth()
    return formatDate(start) to formatDate(end)
}

fun getBillingCycleRange(yearMonth: YearMonth, startDay: Int): Pair<String, String> {
    val clampedDay = startDay.coerceIn(1, yearMonth.lengthOfMonth())
    val start = yearMonth.atDay(clampedDay)
    val nextMonth = yearMonth.plusMonths(1)
    val endDay = (startDay - 1).coerceIn(1, nextMonth.lengthOfMonth())
    val end = nextMonth.atDay(endDay)
    return formatDate(start) to formatDate(end)
}

fun formatDisplayDate(date: LocalDate): String = date.format(displayDateFormatter)

fun formatMonth(yearMonth: YearMonth): String = yearMonth.format(monthFormatter)

fun formatCurrency(amount: Double, symbol: String = "₹"): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return "$symbol${formatter.format(amount)}"
}
