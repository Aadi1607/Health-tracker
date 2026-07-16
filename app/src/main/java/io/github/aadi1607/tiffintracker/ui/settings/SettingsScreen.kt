package io.github.aadi1607.tiffintracker.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.aadi1607.tiffintracker.R
import io.github.aadi1607.tiffintracker.data.ThemeMode
import io.github.aadi1607.tiffintracker.data.db.MAX_USERS
import io.github.aadi1607.tiffintracker.data.db.User
import io.github.aadi1607.tiffintracker.domain.BillCalculator
import io.github.aadi1607.tiffintracker.ui.theme.UserColors
import java.math.RoundingMode

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editingUser by remember { mutableStateOf<User?>(null) }
    var addingUser by remember { mutableStateOf(false) }
    var deletingUser by remember { mutableStateOf<User?>(null) }
    var pickingTime by remember { mutableStateOf(false) }
    var pickingCycleDay by remember { mutableStateOf(false) }
    var editingCurrency by remember { mutableStateOf(false) }
    var pickingPaymentDays by remember { mutableStateOf(false) }
    var confirmRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var confirmResetMonth by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { resId ->
            Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::backupTo) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { confirmRestoreUri = it } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // Users ---------------------------------------------------------
        SectionCard(stringResource(R.string.settings_users_heading)) {
            state.users.forEach { user ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(user.colorArgb))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(user.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            BillCalculator.formatPaise(user.pricePaise, state.settings.currencySymbol) +
                                " / tiffin",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { editingUser = user }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.settings_edit_user))
                    }
                    if (state.users.size > 1) {
                        IconButton(onClick = { deletingUser = user }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.settings_delete_user))
                        }
                    }
                }
            }
            if (state.users.size < MAX_USERS) {
                TextButton(onClick = { addingUser = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.settings_add_user))
                }
            } else {
                Text(
                    stringResource(R.string.settings_max_users),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Billing --------------------------------------------------------
        SectionCard(stringResource(R.string.settings_billing_heading)) {
            SettingRow(
                title = stringResource(R.string.settings_cycle_start_day),
                value = stringResource(R.string.settings_cycle_start_day_value, state.settings.cycleStartDay),
                onClick = { pickingCycleDay = true },
            )
            SettingRow(
                title = stringResource(R.string.settings_currency),
                value = state.settings.currencySymbol,
                onClick = { editingCurrency = true },
            )
        }

        // Notifications ---------------------------------------------------
        SectionCard(stringResource(R.string.settings_notifications_heading)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.settings_daily_reminder),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = state.settings.reminderEnabled,
                    onCheckedChange = viewModel::setReminderEnabled,
                )
            }
            SettingRow(
                title = stringResource(R.string.settings_reminder_time),
                value = "%02d:%02d".format(state.settings.reminderHour, state.settings.reminderMinute),
                onClick = { pickingTime = true },
            )
            SettingRow(
                title = stringResource(R.string.settings_payment_reminder_days),
                value = "${state.settings.paymentReminderDays}",
                onClick = { pickingPaymentDays = true },
            )
        }

        // Appearance ------------------------------------------------------
        SectionCard(stringResource(R.string.settings_appearance_heading)) {
            ThemeMode.entries.forEach { mode ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = state.settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                    )
                    Text(
                        stringResource(
                            when (mode) {
                                ThemeMode.SYSTEM -> R.string.theme_system
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.DARK -> R.string.theme_dark
                            }
                        )
                    )
                }
            }
        }

        // Export & backup ---------------------------------------------------
        SectionCard(stringResource(R.string.settings_export_heading)) {
            OutlinedButton(onClick = viewModel::exportCsv, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_export_csv))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = viewModel::exportPdf, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_export_pdf))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { backupLauncher.launch("tiffin-backup.json") },
                Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_backup))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream")) },
                Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_restore))
            }
        }

        // Danger zone -------------------------------------------------------
        SectionCard(stringResource(R.string.settings_danger_heading)) {
            OutlinedButton(onClick = { confirmResetMonth = true }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_reset_month))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { confirmClearAll = true },
                Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.settings_clear_all))
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    // Dialogs ---------------------------------------------------------------

    if (addingUser || editingUser != null) {
        UserDialog(
            user = editingUser,
            currency = state.settings.currencySymbol,
            onSave = { user ->
                viewModel.saveUser(user)
                addingUser = false
                editingUser = null
            },
            onDismiss = { addingUser = false; editingUser = null },
        )
    }

    deletingUser?.let { user ->
        ConfirmDialog(
            title = stringResource(R.string.settings_delete_user),
            message = stringResource(R.string.settings_delete_user_message, user.name),
            onConfirm = {
                viewModel.deleteUser(user.id)
                deletingUser = null
            },
            onDismiss = { deletingUser = null },
        )
    }

    if (pickingTime) {
        ReminderTimeDialog(
            hour = state.settings.reminderHour,
            minute = state.settings.reminderMinute,
            onConfirm = { h, m ->
                viewModel.setReminderTime(h, m)
                pickingTime = false
            },
            onDismiss = { pickingTime = false },
        )
    }

    if (pickingCycleDay) {
        NumberSliderDialog(
            title = stringResource(R.string.settings_cycle_start_day),
            initial = state.settings.cycleStartDay,
            range = 1..28,
            onConfirm = { viewModel.setCycleStartDay(it); pickingCycleDay = false },
            onDismiss = { pickingCycleDay = false },
        )
    }

    if (pickingPaymentDays) {
        NumberSliderDialog(
            title = stringResource(R.string.settings_payment_reminder_days),
            initial = state.settings.paymentReminderDays,
            range = 0..14,
            onConfirm = { viewModel.setPaymentReminderDays(it); pickingPaymentDays = false },
            onDismiss = { pickingPaymentDays = false },
        )
    }

    if (editingCurrency) {
        var symbol by remember { mutableStateOf(state.settings.currencySymbol) }
        AlertDialog(
            onDismissRequest = { editingCurrency = false },
            title = { Text(stringResource(R.string.settings_currency)) },
            text = {
                OutlinedTextField(value = symbol, onValueChange = { symbol = it.take(3) }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setCurrencySymbol(symbol)
                    editingCurrency = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { editingCurrency = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    confirmRestoreUri?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.settings_restore),
            message = stringResource(R.string.settings_restore_message),
            onConfirm = {
                viewModel.restoreFrom(uri)
                confirmRestoreUri = null
            },
            onDismiss = { confirmRestoreUri = null },
        )
    }

    if (confirmResetMonth) {
        ConfirmDialog(
            title = stringResource(R.string.settings_reset_month),
            message = stringResource(R.string.settings_reset_month_message),
            onConfirm = { viewModel.resetCurrentCycle(); confirmResetMonth = false },
            onDismiss = { confirmResetMonth = false },
        )
    }

    if (confirmClearAll) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_all),
            message = stringResource(R.string.settings_clear_all_message),
            onConfirm = { viewModel.clearAllData(); confirmClearAll = false },
            onDismiss = { confirmClearAll = false },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun UserDialog(
    user: User?,
    currency: String,
    onSave: (User) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var priceText by remember {
        mutableStateOf(
            user?.let {
                if (it.pricePaise % 100 == 0L) (it.pricePaise / 100).toString()
                else (it.pricePaise / 100.0).toString()
            } ?: "60"
        )
    }
    var color by remember { mutableStateOf(user?.colorArgb ?: UserColors.palette.first()) }

    val pricePaise = priceText.trim().toBigDecimalOrNull()
        ?.movePointRight(2)
        ?.setScale(0, RoundingMode.HALF_UP)
        ?.toLong()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (user == null) R.string.settings_add_user else R.string.settings_edit_user
                )
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.settings_user_name)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("${stringResource(R.string.settings_user_price)} ($currency)") },
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.settings_user_color),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserColors.palette.forEach { candidate ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(candidate))
                                .clickable { color = candidate },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (candidate == color) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && pricePaise != null && pricePaise >= 0,
                onClick = {
                    onSave(
                        User(
                            id = user?.id ?: 0L,
                            name = name.trim(),
                            colorArgb = color,
                            pricePaise = pricePaise ?: 0L,
                            sortOrder = user?.sortOrder ?: 0,
                        )
                    )
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    hour: Int,
    minute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timeState = rememberTimePickerState(initialHour = hour, initialMinute = minute)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_reminder_time)) },
        text = { TimePicker(state = timeState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun NumberSliderDialog(
    title: String,
    initial: Int,
    range: IntRange,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableIntStateOf(initial.coerceIn(range)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("$value", style = MaterialTheme.typography.headlineSmall)
                Slider(
                    value = value.toFloat(),
                    onValueChange = { value = it.toInt().coerceIn(range) },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    steps = (range.last - range.first - 1).coerceAtLeast(0),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
