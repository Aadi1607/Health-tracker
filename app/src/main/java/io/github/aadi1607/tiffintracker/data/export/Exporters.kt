package io.github.aadi1607.tiffintracker.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import io.github.aadi1607.tiffintracker.data.db.EntryStatus
import io.github.aadi1607.tiffintracker.data.db.Payment
import io.github.aadi1607.tiffintracker.data.db.TiffinEntry
import io.github.aadi1607.tiffintracker.data.db.User
import io.github.aadi1607.tiffintracker.domain.BillCalculator
import io.github.aadi1607.tiffintracker.domain.BillingCycle
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy")

private fun exportDir(context: Context): File =
    File(context.cacheDir, "exports").apply { mkdirs() }

fun shareFile(context: Context, file: File, mimeType: String, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

object CsvExporter {
    /** Date-wise entries for every user in [cycle], with per-user totals. */
    fun export(
        context: Context,
        cycle: BillingCycle,
        users: List<User>,
        entries: List<TiffinEntry>,
        currencySymbol: String,
    ): File {
        val sb = StringBuilder()
        sb.appendLine("User,Date,Meal,Status,Quantity,Note")
        for (user in users) {
            val own = entries
                .filter { it.userId == user.id && it.epochDay in cycle.startEpochDay..cycle.endEpochDay }
                .sortedWith(compareBy({ it.epochDay }, { it.mealType }))
            for (e in own) {
                val date = LocalDate.ofEpochDay(e.epochDay).format(dateFormat)
                sb.appendLine(
                    listOf(
                        csv(user.name), date, e.mealType.name, e.status.name,
                        e.quantity.toString(), csv(e.note),
                    ).joinToString(",")
                )
            }
            val count = BillCalculator.tiffinCount(own)
            val bill = BillCalculator.formatPaise(count * user.pricePaise, currencySymbol)
            sb.appendLine("${csv(user.name)} TOTAL,,,,$count,$bill")
        }
        val file = File(
            exportDir(context),
            "tiffin-report-${cycle.start.format(DateTimeFormatter.ISO_DATE)}.csv",
        )
        file.writeText(sb.toString())
        return file
    }

    private fun csv(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value
}

object PdfExporter {
    private const val PAGE_WIDTH = 595 // A4 @ 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f

    /** One invoice page (or more, if long) per user. Returns the PDF file. */
    fun export(
        context: Context,
        cycle: BillingCycle,
        users: List<User>,
        entries: List<TiffinEntry>,
        payments: List<Payment>,
        currencySymbol: String,
    ): File {
        val doc = PdfDocument()
        try {
            for (user in users) {
                writeUserInvoice(doc, cycle, user, entries, payments, currencySymbol)
            }
            val file = File(
                exportDir(context),
                "tiffin-invoice-${cycle.start.format(DateTimeFormatter.ISO_DATE)}.pdf",
            )
            file.outputStream().use { doc.writeTo(it) }
            return file
        } finally {
            doc.close()
        }
    }

    private fun writeUserInvoice(
        doc: PdfDocument,
        cycle: BillingCycle,
        user: User,
        allEntries: List<TiffinEntry>,
        allPayments: List<Payment>,
        symbol: String,
    ) {
        val title = Paint().apply {
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(230, 81, 0)
        }
        val heading = Paint().apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.DKGRAY
        }
        val body = Paint().apply { textSize = 11f; color = Color.BLACK }
        val muted = Paint().apply { textSize = 10f; color = Color.GRAY }
        val line = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        val bill = BillCalculator.userBill(
            user.id, user.pricePaise, cycle, allEntries, allPayments,
        )
        val rows = allEntries
            .filter {
                it.userId == user.id &&
                    it.epochDay in cycle.startEpochDay..cycle.endEpochDay &&
                    it.status == EntryStatus.TAKEN
            }
            .sortedWith(compareBy({ it.epochDay }, { it.mealType }))

        var pageNumber = doc.pages.size + 1
        var page = doc.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        )
        var canvas = page.canvas
        var y = MARGIN + 10f

        fun newPageIfNeeded() {
            if (y > PAGE_HEIGHT - MARGIN - 100f) {
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                canvas = page.canvas
                y = MARGIN + 10f
            }
        }

        canvas.drawText("Tiffin Invoice", MARGIN, y, title)
        y += 20f
        canvas.drawText(
            "Cycle: ${cycle.start.format(dateFormat)} – ${cycle.end.format(dateFormat)}",
            MARGIN, y, muted,
        )
        y += 14f
        canvas.drawText("Generated on ${LocalDate.now().format(dateFormat)}", MARGIN, y, muted)
        y += 28f

        canvas.drawText("Billed to: ${user.name}", MARGIN, y, heading)
        canvas.drawText(
            "Rate: ${BillCalculator.formatPaise(user.pricePaise, symbol)} / tiffin",
            PAGE_WIDTH - MARGIN - 160f, y, heading,
        )
        y += 18f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, line)
        y += 18f

        canvas.drawText("Date", MARGIN, y, heading)
        canvas.drawText("Meal", MARGIN + 130f, y, heading)
        canvas.drawText("Qty", MARGIN + 230f, y, heading)
        canvas.drawText("Note", MARGIN + 290f, y, heading)
        y += 8f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, line)
        y += 16f

        if (rows.isEmpty()) {
            canvas.drawText("No tiffins taken in this cycle.", MARGIN, y, body)
            y += 16f
        }
        for (row in rows) {
            newPageIfNeeded()
            canvas.drawText(LocalDate.ofEpochDay(row.epochDay).format(dateFormat), MARGIN, y, body)
            canvas.drawText(row.mealType.name.lowercase().replaceFirstChar { it.uppercase() }, MARGIN + 130f, y, body)
            canvas.drawText(row.quantity.toString(), MARGIN + 230f, y, body)
            canvas.drawText(row.note.take(40), MARGIN + 290f, y, body)
            y += 16f
        }

        y += 10f
        newPageIfNeeded()
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, line)
        y += 20f

        fun totalLine(label: String, value: String, bold: Boolean = false) {
            val paint = if (bold) heading else body
            canvas.drawText(label, PAGE_WIDTH - MARGIN - 240f, y, paint)
            canvas.drawText(value, PAGE_WIDTH - MARGIN - 80f, y, paint)
            y += 16f
        }

        totalLine("Tiffins taken", bill.tiffinCount.toString())
        totalLine("Bill amount", BillCalculator.formatPaise(bill.billPaise, symbol))
        totalLine("Paid", BillCalculator.formatPaise(bill.paidPaise, symbol))
        totalLine("Carry-forward", BillCalculator.formatPaise(bill.carryForwardPaise, symbol))
        totalLine("Total due", BillCalculator.formatPaise(bill.totalDuePaise, symbol), bold = true)

        doc.finishPage(page)
    }
}
