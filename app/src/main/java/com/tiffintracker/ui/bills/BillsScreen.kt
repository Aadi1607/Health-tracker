package com.tiffintracker.ui.bills

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tiffintracker.R
import com.tiffintracker.data.db.Payment
import com.tiffintracker.data.db.PaymentMode
import com.tiffintracker.data.db.Provider
import com.tiffintracker.data.repository.BillSummary
import com.tiffintracker.ui.theme.TiffinTrackerTheme
import com.tiffintracker.util.formatCurrency
import com.tiffintracker.util.formatMonth
import com.tiffintracker.util.today
import java.time.YearMonth

@Composable
fun BillsScreen(
    viewModel: BillsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    BillsContent(
        uiState = uiState,
        onPreviousMonth = { viewModel.navigateMonth(false) },
        onNextMonth = { viewModel.navigateMonth(true) },
        onSelectProvider = { viewModel.loadBillForProvider(it, uiState.yearMonth) },
        onAddPayment = { providerId, amount, date, mode, note ->
            viewModel.addPayment(Payment(providerId = providerId, amount = amount, date = date, mode = mode.name, note = note))
        },
        onDeletePayment = { viewModel.deletePayment(it) },
        onShareBill = { providerId ->
            val text = viewModel.generateBillText(providerId, uiState.yearMonth)
            com.tiffintracker.util.shareText(context, text)
        }
    )
}

@Composable
private fun BillsContent(
    uiState: BillsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectProvider: (Long) -> Unit,
    onAddPayment: (Long, Double, String, PaymentMode, String) -> Unit,
    onDeletePayment: (Payment) -> Unit,
    onShareBill: (Long) -> Unit
) {
    var showPaymentDialog by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        MonthSelector(
            yearMonth = uiState.yearMonth,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.providerBills.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No providers yet. Add a provider first.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    MonthlySummaryCard(providerBills = uiState.providerBills)
                }

                items(uiState.providerBills, key = { it.provider.id }) { providerBill ->
                    val isExpanded = uiState.selectedProviderBill?.provider?.id == providerBill.provider.id
                    val paymentsForProvider = if (isExpanded) uiState.payments else emptyList()

                    ProviderBillCard(
                        providerBill = providerBill,
                        isExpanded = isExpanded,
                        payments = paymentsForProvider,
                        onToggleExpand = { onSelectProvider(providerBill.provider.id) },
                        onRecordPayment = { showPaymentDialog = providerBill.provider.id },
                        onDeletePayment = onDeletePayment,
                        onShareBill = { onShareBill(providerBill.provider.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    showPaymentDialog?.let { providerId ->
        RecordPaymentDialog(
            onDismiss = { showPaymentDialog = null },
            onConfirm = { amount, date, mode, note ->
                onAddPayment(providerId, amount, date, mode, note)
                showPaymentDialog = null
            }
        )
    }
}

@Composable
private fun MonthSelector(
    yearMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous month"
            )
        }

        Text(
            text = formatMonth(yearMonth),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next month"
            )
        }
    }
}

@Composable
private fun MonthlySummaryCard(providerBills: List<ProviderBill>) {
    val totalReceived = providerBills.sumOf {
        it.billSummary.breakfastCount + it.billSummary.lunchCount + it.billSummary.dinnerCount
    }
    val totalDue = providerBills.sumOf { it.billSummary.totalAmount }
    val totalPaid = providerBills.sumOf { it.billSummary.totalPaid }
    val balance = totalDue - totalPaid

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "Total Meals",
                    value = totalReceived.toString()
                )
                SummaryItem(
                    label = "Amount Due",
                    value = formatCurrency(totalDue)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "Amount Paid",
                    value = formatCurrency(totalPaid)
                )
                SummaryItem(
                    label = "Balance",
                    value = formatCurrency(balance),
                    valueColor = if (balance > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun ProviderBillCard(
    providerBill: ProviderBill,
    isExpanded: Boolean,
    payments: List<Payment>,
    onToggleExpand: () -> Unit,
    onRecordPayment: () -> Unit,
    onDeletePayment: (Payment) -> Unit,
    onShareBill: () -> Unit
) {
    val provider = providerBill.provider
    val bill = providerBill.billSummary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (provider.offersBreakfast && bill.breakfastCount > 0) {
                MealLineItem(
                    label = stringResource(R.string.meal_breakfast),
                    count = bill.breakfastCount,
                    price = provider.breakfastPrice,
                    total = bill.breakfastAmount
                )
            }
            if (provider.offersLunch && bill.lunchCount > 0) {
                MealLineItem(
                    label = stringResource(R.string.meal_lunch),
                    count = bill.lunchCount,
                    price = provider.lunchPrice,
                    total = bill.lunchAmount
                )
            }
            if (provider.offersDinner && bill.dinnerCount > 0) {
                MealLineItem(
                    label = stringResource(R.string.meal_dinner),
                    count = bill.dinnerCount,
                    price = provider.dinnerPrice,
                    total = bill.dinnerAmount
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatCurrency(bill.totalAmount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Paid",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(bill.totalPaid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Balance",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatCurrency(bill.balanceDue),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (bill.balanceDue > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onRecordPayment,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Record Payment")
                }
                OutlinedButton(
                    onClick = onShareBill,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    PaymentHistorySection(
                        payments = payments,
                        onDeletePayment = onDeletePayment
                    )
                }
            }
        }
    }
}

@Composable
private fun MealLineItem(
    label: String,
    count: Int,
    price: Double,
    total: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label: $count × ${formatCurrency(price)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatCurrency(total),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PaymentHistorySection(
    payments: List<Payment>,
    onDeletePayment: (Payment) -> Unit
) {
    if (payments.isEmpty()) {
        Text(
            text = "No payments recorded",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    } else {
        Text(
            text = "Payment History",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        payments.forEach { payment ->
            PaymentRow(
                payment = payment,
                onDelete = { onDeletePayment(payment) }
            )
        }
    }
}

@Composable
private fun PaymentRow(
    payment: Payment,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrency(payment.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = payment.mode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = payment.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (payment.note.isNotBlank()) {
                Text(
                    text = payment.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = { showDeleteConfirm = true }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Payment") },
            text = { Text("Are you sure you want to delete this payment?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordPaymentDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, date: String, mode: PaymentMode, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today()) }
    var selectedMode by remember { mutableStateOf(PaymentMode.UPI) }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                    },
                    label = { Text("Amount") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("Please enter a valid amount") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text(stringResource(R.string.payment_date)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.payment_mode),
                    style = MaterialTheme.typography.labelLarge
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = {
                                Text(
                                    when (mode) {
                                        PaymentMode.CASH -> stringResource(R.string.payment_cash)
                                        PaymentMode.UPI -> stringResource(R.string.payment_upi)
                                        PaymentMode.OTHER -> stringResource(R.string.payment_other)
                                    }
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.payment_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        amountError = true
                    } else {
                        onConfirm(amount, date, selectedMode, note)
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun BillsContentPreview() {
    val sampleProvider = Provider(
        id = 1L,
        name = "Sharma Tiffin",
        phone = "9876543210",
        breakfastPrice = 50.0,
        lunchPrice = 80.0,
        dinnerPrice = 70.0,
        offersBreakfast = true,
        offersLunch = true,
        offersDinner = true,
        billingCycleStartDay = 1,
        isActive = true,
        createdAt = System.currentTimeMillis()
    )
    val sampleBill = BillSummary(
        providerId = 1L,
        providerName = "Sharma Tiffin",
        startDate = "2026-10-01",
        endDate = "2026-10-31",
        breakfastCount = 22,
        lunchCount = 25,
        dinnerCount = 20,
        breakfastAmount = 1100.0,
        lunchAmount = 2000.0,
        dinnerAmount = 1400.0,
        totalAmount = 4500.0,
        totalPaid = 3000.0,
        balanceDue = 1500.0
    )

    TiffinTrackerTheme {
        BillsContent(
            uiState = BillsUiState(
                providerBills = listOf(ProviderBill(sampleProvider, sampleBill)),
                isLoading = false
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onSelectProvider = {},
            onAddPayment = { _, _, _, _, _ -> },
            onDeletePayment = {},
            onShareBill = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordPaymentDialogPreview() {
    TiffinTrackerTheme {
        RecordPaymentDialog(
            onDismiss = {},
            onConfirm = { _, _, _, _ -> }
        )
    }
}
