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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.aadi1607.habittracker.R
import io.github.aadi1607.habittracker.ui.theme.HabitPalette

private val EmojiChoices = listOf(
    "💪", "🏃", "🚶", "🧘", "📖", "✍️",
    "💧", "🥗", "😴", "🦷", "🧹", "🎯",
    "🎨", "🎸", "🌱", "💊", "🙏", "💻",
)

private val TargetChoices = listOf(1, 2, 3, 4, 5, 6, 8, 10, 12)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddHabitSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, color: Long, dailyTarget: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable { mutableStateOf("") }
    var emojiIndex by rememberSaveable { mutableIntStateOf(0) }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    var targetIndex by rememberSaveable { mutableIntStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.add_habit_title),
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
                EmojiChoices.forEachIndexed { index, emoji ->
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
                text = stringResource(R.string.times_per_day),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.times_per_day_hint),
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
                        EmojiChoices[emojiIndex],
                        HabitPalette[colorIndex],
                        TargetChoices[targetIndex],
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.save_habit))
            }
        }
    }
}
