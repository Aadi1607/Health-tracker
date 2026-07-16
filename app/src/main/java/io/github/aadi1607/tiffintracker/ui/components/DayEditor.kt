package io.github.aadi1607.tiffintracker.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.aadi1607.tiffintracker.R
import io.github.aadi1607.tiffintracker.data.DayLog
import io.github.aadi1607.tiffintracker.data.MealState
import io.github.aadi1607.tiffintracker.data.db.User
import io.github.aadi1607.tiffintracker.ui.theme.StatusSkipped

/**
 * Editable card for one user's log on one day: lunch/dinner chips, an
 * extra-tiffin stepper, skip-day and note actions. Used on the home screen
 * and inside the calendar's day sheet.
 */
@Composable
fun DayEditorCard(
    user: User,
    log: DayLog,
    onLogChange: (DayLog) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var noteDialogOpen by remember { mutableStateOf(false) }

    fun change(newLog: DayLog) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onLogChange(newLog)
    }

    Card(
        modifier = modifier
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
                        .background(Color(user.colorArgb))
                )
                Spacer(Modifier.width(8.dp))
                Text(user.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                if (log.takenCount > 0) {
                    Text(
                        stringResource(R.string.tiffins_today_count, log.takenCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (log.isSkippedDay) {
                    Text(
                        stringResource(R.string.day_skipped),
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusSkipped,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MealChip(
                    label = stringResource(R.string.meal_lunch),
                    state = log.lunch,
                    onClick = {
                        change(log.copy(lunch = if (log.lunch == MealState.TAKEN) MealState.NONE else MealState.TAKEN))
                    },
                )
                MealChip(
                    label = stringResource(R.string.meal_dinner),
                    state = log.dinner,
                    onClick = {
                        change(log.copy(dinner = if (log.dinner == MealState.TAKEN) MealState.NONE else MealState.TAKEN))
                    },
                )

                Spacer(Modifier.weight(1f))

                // Extra tiffin stepper
                IconButton(
                    onClick = { if (log.extraQuantity > 0) change(log.copy(extraQuantity = log.extraQuantity - 1)) },
                    enabled = log.extraQuantity > 0,
                ) {
                    Icon(
                        Icons.Outlined.Remove,
                        contentDescription = stringResource(R.string.decrease_extra),
                    )
                }
                Text(
                    "${log.extraQuantity}",
                    style = MaterialTheme.typography.titleMedium,
                )
                FilledIconButton(
                    onClick = { change(log.copy(extraQuantity = log.extraQuantity + 1)) },
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.increase_extra),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        change(
                            if (log.isSkippedDay) DayLog(note = log.note)
                            else log.copy(
                                lunch = MealState.SKIPPED,
                                dinner = MealState.SKIPPED,
                                extraQuantity = 0,
                            )
                        )
                    },
                ) {
                    Icon(Icons.Filled.EventBusy, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(
                            if (log.isSkippedDay) R.string.undo_skip else R.string.mark_skipped
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { noteDialogOpen = true }) {
                    Icon(Icons.Filled.NoteAdd, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(
                            if (log.note.isBlank()) R.string.add_note else R.string.edit_note
                        )
                    )
                }
            }

            if (log.note.isNotBlank()) {
                Text(
                    log.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (noteDialogOpen) {
        var draft by remember { mutableStateOf(log.note) }
        AlertDialog(
            onDismissRequest = { noteDialogOpen = false },
            title = { Text(stringResource(R.string.note_label)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.note_hint)) },
                    singleLine = false,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    noteDialogOpen = false
                    onLogChange(log.copy(note = draft))
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { noteDialogOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun MealChip(label: String, state: MealState, onClick: () -> Unit) {
    FilterChip(
        selected = state == MealState.TAKEN,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = when (state) {
            MealState.TAKEN -> {
                { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
            }
            MealState.SKIPPED -> {
                { Icon(Icons.Filled.Close, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
            }
            MealState.NONE -> null
        },
    )
}
