package io.github.aadi1607.habittracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.aadi1607.habittracker.R
import io.github.aadi1607.habittracker.data.db.Habit
import io.github.aadi1607.habittracker.ui.theme.HabitPalette

@Composable
private fun PeriodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                shape = CircleShape,
            )
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

private val EmojiChoices = listOf(
    "💪", "🏃", "🚶", "🧘", "📖", "✍️",
    "💧", "🥗", "😴", "🦷", "🧹", "🎯",
    "🎨", "🎸", "🌱", "💊", "🙏", "💻",
)

private val TargetChoices = listOf(1, 2, 3, 4, 5, 6, 8, 10, 12)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitFormSheet(
    initial: Habit?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, color: Long, dailyTarget: Int, goalPeriod: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // When editing a habit whose emoji is not in the presets (e.g. imported),
    // surface it as an extra first choice so the selection stays intact.
    val emojis = remember(initial) {
        if (initial != null && initial.emoji !in EmojiChoices) {
            listOf(initial.emoji) + EmojiChoices
        } else {
            EmojiChoices
        }
    }
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var emojiIndex by rememberSaveable {
        mutableIntStateOf(if (initial == null) 0 else emojis.indexOf(initial.emoji).coerceAtLeast(0))
    }
    var colorIndex by rememberSaveable {
        mutableIntStateOf(if (initial == null) 0 else HabitPalette.indexOf(initial.color).coerceAtLeast(0))
    }
    var targetIndex by rememberSaveable {
        mutableIntStateOf(
            if (initial == null) {
                0
            } else {
                TargetChoices.indexOf(initial.dailyTarget).takeIf { it >= 0 }
                    ?: TargetChoices.indexOfLast { it <= initial.dailyTarget }.coerceAtLeast(0)
            }
        )
    }
    var weekly by rememberSaveable { mutableStateOf(initial?.isWeekly == true) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(
                    if (initial == null) R.string.add_habit_title else R.string.edit_habit_title
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.habit_name_label)) },
                placeholder = { Text(stringResource(R.string.habit_name_hint)) },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.pick_emoji),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                emojis.forEachIndexed { index, emoji ->
                    val selected = index == emojiIndex
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                } else {
                                    Color.Transparent
                                },
                                shape = CircleShape,
                            )
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                },
                                shape = CircleShape,
                            )
                            .selectable(selected = selected, onClick = { emojiIndex = index }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = emoji, textAlign = TextAlign.Center)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.pick_color),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitPalette.forEachIndexed { index, argb ->
                    val selected = index == colorIndex
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(argb), CircleShape)
                            .selectable(selected = selected, onClick = { colorIndex = index }),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.color_selected),
                                tint = MaterialTheme.colorScheme.surface,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.goal_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodChip(
                    label = stringResource(R.string.per_day),
                    selected = !weekly,
                    onClick = { weekly = false },
                )
                PeriodChip(
                    label = stringResource(R.string.per_week),
                    selected = weekly,
                    onClick = { weekly = true },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(
                    if (weekly) R.string.times_per_week else R.string.times_per_day
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    if (weekly) R.string.times_per_week_hint else R.string.times_per_day_hint
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TargetChoices.forEachIndexed { index, target ->
                    val selected = index == targetIndex
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                shape = CircleShape,
                            )
                            .selectable(selected = selected, onClick = { targetIndex = index }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = target.toString(),
                            textAlign = TextAlign.Center,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    onSave(
                        name,
                        emojis[emojiIndex],
                        HabitPalette[colorIndex],
                        TargetChoices[targetIndex],
                        if (weekly) Habit.PERIOD_WEEKLY else Habit.PERIOD_DAILY,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = name.isNotBlank(),
            ) {
                Text(
                    stringResource(
                        if (initial == null) R.string.save_habit else R.string.update_habit
                    )
                )
            }
        }
    }
}
