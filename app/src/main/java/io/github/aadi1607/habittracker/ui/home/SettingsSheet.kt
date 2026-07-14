package io.github.aadi1607.habittracker.ui.home

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.aadi1607.habittracker.R
import io.github.aadi1607.habittracker.data.ReminderSettings
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val NudgeIntervalChoices = listOf(1, 2, 3, 4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    dynamicColor: Boolean,
    reminder: ReminderSettings?,
    onDismiss: () -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (hour: Int, minute: Int) -> Unit,
    onNudgesEnabledChange: (Boolean) -> Unit,
    onNudgeIntervalChange: (Int) -> Unit,
    onTestNotification: () -> Unit,
    onChangePassword: (current: String, new: String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onLogout: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showPasswordDialog by rememberSaveable { mutableStateOf(false) }

    // Runs the action that asked for POST_NOTIFICATIONS once the system dialog
    // closes. The toggles stay enabled even on denial; workers check
    // notification access before posting, so a denial simply means silence.
    val pendingPermissionAction = remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        pendingPermissionAction.value?.invoke()
        pendingPermissionAction.value = null
    }

    fun withNotificationPermission(action: () -> Unit) {
        val needsRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsRequest) {
            pendingPermissionAction.value = action
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dynamic_color),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            stringResource(R.string.dynamic_color_summary)
                        } else {
                            stringResource(R.string.dynamic_color_unavailable)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = dynamicColor,
                    onCheckedChange = onDynamicColorChange,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.daily_reminder),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.daily_reminder_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = reminder?.enabled == true,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            withNotificationPermission { onReminderEnabledChange(true) }
                        } else {
                            onReminderEnabledChange(false)
                        }
                    },
                )
            }

            if (reminder?.enabled == true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.reminder_time),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = LocalTime.of(reminder.hour, reminder.minute)
                            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTestNotification)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.weight(0.05f))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.test_notification),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.test_notification_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val powerManager = remember { context.getSystemService(PowerManager::class.java) }
            val ignoringBattery = remember {
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }
            if (!ignoringBattery) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.battery_optimization_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}"),
                                )
                            )
                        },
                    ) {
                        Text(stringResource(R.string.allow))
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = remember { context.getSystemService(AlarmManager::class.java) }
                val exactGranted = remember { alarmManager.canScheduleExactAlarms() }
                if (!exactGranted) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.exact_alarms_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}"),
                                    )
                                )
                            },
                        ) {
                            Text(stringResource(R.string.allow))
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.nudges),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.nudges_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = reminder?.nudgesEnabled == true,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            withNotificationPermission { onNudgesEnabledChange(true) }
                        } else {
                            onNudgesEnabledChange(false)
                        }
                    },
                )
            }

            if (reminder?.nudgesEnabled == true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NudgeIntervalChoices.forEach { hours ->
                        val selected = reminder.nudgeIntervalHours == hours
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    }
                                )
                                .selectable(
                                    selected = selected,
                                    onClick = { onNudgeIntervalChange(hours) },
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.every_n_hours, hours),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.export_data))
                }
                Spacer(modifier = Modifier.weight(0.1f))
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.import_data))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPasswordDialog = true }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.weight(0.05f))
                Text(
                    text = stringResource(R.string.change_password),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLogout)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.weight(0.05f))
                Text(
                    text = stringResource(R.string.logout),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showPasswordDialog) {
        var current by rememberSaveable { mutableStateOf("") }
        var new by rememberSaveable { mutableStateOf("") }
        var confirm by rememberSaveable { mutableStateOf("") }
        val mismatch = confirm.isNotEmpty() && new != confirm
        val tooShort = new.isNotEmpty() && new.length < 4
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text(stringResource(R.string.change_password)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = current,
                        onValueChange = { current = it },
                        label = { Text(stringResource(R.string.current_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = new,
                        onValueChange = { new = it },
                        label = { Text(stringResource(R.string.new_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = tooShort,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text(stringResource(R.string.confirm_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = mismatch,
                    )
                    if (mismatch || tooShort) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                if (mismatch) R.string.password_mismatch else R.string.password_too_short
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = current.isNotEmpty() && new.length >= 4 && new == confirm,
                    onClick = {
                        onChangePassword(current, new)
                        showPasswordDialog = false
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showTimePicker && reminder != null) {
        val timeState = rememberTimePickerState(
            initialHour = reminder.hour,
            initialMinute = reminder.minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.reminder_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReminderTimeChange(timeState.hour, timeState.minute)
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
