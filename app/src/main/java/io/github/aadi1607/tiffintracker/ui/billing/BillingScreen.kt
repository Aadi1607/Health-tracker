package io.github.aadi1607.tiffintracker.ui.billing

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.aadi1607.tiffintracker.R
import io.github.aadi1607.tiffintracker.data.db.User
import io.github.aadi1607.tiffintracker.domain.BillCalculator
import io.github.aadi1607.tiffintracker.domain.BillStatus
import io.github.aadi1607.tiffintracker.ui.theme.StatusPending
import io.github.aadi1607.tiffintracker.ui.theme.StatusTaken
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val cycleFormat = DateTimeFormatter.ofPattern("dd MMM")
private val paymentFormat = DateTimeFormatter.ofPattern("dd MMM yyyy")

@Composable
fun BillingScreen(viewModel: BillingViewModel = viewModel(factory = BillingViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var payingUser by remember { mutableStateOf<User?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::previousCycle) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.billing_previous_cycle),
                )
            }
            Text(
                state.cycle?.let {
                    stringResource(
                        R.string.billing_cycle_label,
                        it.start.format(cycleFormat),
                        it.end.format(cycleFormat),
                    )
                } ?: "",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = viewModel::nextCycle) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.billing_next_cycle),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.rows, key = { it.user.id }) { row ->
                BillCard(
                    row = row,
                    currency = state.currencySymbol,
                    onRecordPayment = { payingUser = row.user },
                )
            }

            item {
                Text(
                    stringResource(R.string.billing_payments_heading),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.cyclePayments.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.billing_no_payments),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.cyclePayments, key = { "p${it.id}" }) { payment ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${state.userNames[payment.userId] ?: "?"} · " +
                                    BillCalculator.formatPaise(payment.amountPaise, state.currencySymbol),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                LocalDate.ofEpochDay(payment.epochDay).format(paymentFormat),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.deletePayment(payment) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.billing_delete_payment),
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    payingUser?.let { user ->
        val balance = state.rows.firstOrNull { it.user.id == user.id }?.bill?.totalDuePaise ?: 0L
        PaymentDialog(
            user = user,
            suggestedPaise = balance.coerceAtLeast(0),
            currency = state.currencySymbol,
            onConfirm = { amountPaise ->
                viewModel.addPayment(user.id, amountPaise, LocalDate.now())
                payingUser = null
            },
            onDismiss = { payingUser = null },
        )
    }
}

@Composable
private fun BillCard(row: BillingRow, currency: String, onRecordPayment: () -> Unit) {
    val bill = row.bill
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(row.user.colorArgb))
                )
                Spacer(Modifier.width(8.dp))
                Text(row.user.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                StatusBadge(bill.status)
            }
            Spacer(Modifier.height(12.dp))
            AmountRow(stringResource(R.string.billing_tiffins), "${bill.tiffinCount}")
            AmountRow(
                stringResource(R.string.billing_bill),
                BillCalculator.formatPaise(bill.billPaise, currency),
            )
            AmountRow(
                stringResource(R.string.billing_paid),
                BillCalculator.formatPaise(bill.paidPaise, currency),
            )
            AmountRow(
                stringResource(R.string.billing_carry_forward),
                BillCalculator.formatPaise(bill.carryForwardPaise, currency),
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            AmountRow(
                stringResource(R.string.billing_total_due),
                BillCalculator.formatPaise(bill.totalDuePaise, currency),
                emphasize = true,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRecordPayment, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.billing_record_payment))
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else null,
        )
    }
}

@Composable
private fun StatusBadge(status: BillStatus) {
    val (labelRes, color) = when (status) {
        BillStatus.PAID -> R.string.billing_status_paid to StatusTaken
        BillStatus.PARTIAL -> R.string.billing_status_partial to StatusPending
        BillStatus.PENDING -> R.string.billing_status_pending to MaterialTheme.colorScheme.error
    }
    Text(
        stringResource(labelRes),
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}

@Composable
private fun PaymentDialog(
    user: User,
    suggestedPaise: Long,
    currency: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(if (suggestedPaise > 0) formatForInput(suggestedPaise) else "") }
    val parsed = parseRupees(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stringResource(R.string.billing_record_payment)} — ${user.name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("${stringResource(R.string.billing_payment_amount)} ($currency)") },
                    singleLine = true,
                )
                if (suggestedPaise > 0) {
                    AssistChip(
                        onClick = { text = formatForInput(suggestedPaise) },
                        label = {
                            Text(
                                "${stringResource(R.string.billing_pay_full)}: " +
                                    BillCalculator.formatPaise(suggestedPaise, currency)
                            )
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null && parsed > 0,
                onClick = { parsed?.let(onConfirm) },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun formatForInput(paise: Long): String =
    if (paise % 100 == 0L) (paise / 100).toString()
    else "${paise / 100}.${(paise % 100).toString().padStart(2, '0')}"

/** "62.5" -> 6250 paise; returns null for unparseable input. */
private fun parseRupees(text: String): Long? {
    val trimmed = text.trim().replace(",", "")
    if (trimmed.isEmpty()) return null
    return trimmed.toBigDecimalOrNull()
        ?.movePointRight(2)
        ?.setScale(0, java.math.RoundingMode.HALF_UP)
        ?.toLong()
}
