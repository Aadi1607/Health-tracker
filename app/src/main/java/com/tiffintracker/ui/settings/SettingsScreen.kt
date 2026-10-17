package com.tiffintracker.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tiffintracker.R
import com.tiffintracker.ui.components.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup { json ->
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray())
                }
                Toast.makeText(context, context.getString(R.string.backup_success), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            if (json != null) {
                viewModel.importBackup(json) { success ->
                    val msg = if (success) context.getString(R.string.restore_success)
                    else context.getString(R.string.restore_failed)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_settings)) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Reminders Section ──
            SectionHeader(text = "Reminders")

            ListItem(
                headlineContent = { Text(stringResource(R.string.reminder)) },
                supportingContent = { Text("Get reminded to log your meals") },
                leadingContent = {
                    Icon(Icons.Filled.Notifications, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = uiState.reminderEnabled,
                        onCheckedChange = { viewModel.updateReminderEnabled(it) }
                    )
                }
            )

            if (uiState.reminderEnabled) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.reminder_time)) },
                    supportingContent = {
                        Text(formatTime(uiState.reminderHour, uiState.reminderMinute))
                    },
                    modifier = Modifier.clickable { showTimePicker = true }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Appearance Section ──
            SectionHeader(text = "Appearance")

            ListItem(
                headlineContent = { Text(stringResource(R.string.theme)) },
                supportingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        listOf("system", "light", "dark").forEach { mode ->
                            val label = when (mode) {
                                "light" -> stringResource(R.string.theme_light)
                                "dark" -> stringResource(R.string.theme_dark)
                                else -> stringResource(R.string.theme_system)
                            }
                            FilterChip(
                                selected = uiState.themeMode == mode,
                                onClick = { viewModel.updateThemeMode(mode) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Data Section ──
            SectionHeader(text = "Data")

            ListItem(
                headlineContent = { Text(stringResource(R.string.backup_data)) },
                supportingContent = { Text("Export your data as a backup file") },
                modifier = Modifier.clickable {
                    exportLauncher.launch("tiffin_tracker_backup.json")
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.restore_data)) },
                supportingContent = { Text("Import data from a backup file") },
                modifier = Modifier.clickable {
                    importLauncher.launch(arrayOf("application/json"))
                }
            )

            ListItem(
                headlineContent = {
                    Text(
                        stringResource(R.string.clear_data),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                supportingContent = { Text("Delete all data permanently") },
                leadingContent = {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable { showClearDataDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── About Section ──
            SectionHeader(text = stringResource(R.string.about))

            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name)) },
                supportingContent = { Text(stringResource(R.string.version, "1.0.0")) },
                leadingContent = {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = uiState.reminderHour,
            initialMinute = uiState.reminderMinute,
            onConfirm = { hour, minute ->
                viewModel.updateReminderTime(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showClearDataDialog) {
        ConfirmDialog(
            title = stringResource(R.string.clear_data),
            message = stringResource(R.string.confirm_clear_data),
            onConfirm = {
                viewModel.clearAllData()
                showClearDataDialog = false
                Toast.makeText(context, "Data cleared", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearDataDialog = false }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%d:%02d %s", displayHour, minute, amPm)
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    MaterialTheme {
        SectionHeader(text = "Reminders")
    }
}
