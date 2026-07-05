package io.github.aadi1607.habittracker.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aadi1607.habittracker.R
import io.github.aadi1607.habittracker.ui.theme.DotEmpty
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitCard(
    card: HabitCardUi,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habitColor = Color(card.habit.color)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(habitColor.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = card.habit.emoji, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = card.habit.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (card.streak > 0) {
                                pluralStringResource(R.plurals.streak_days, card.streak, card.streak)
                            } else {
                                stringResource(R.string.no_streak_yet)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                DayChain(
                    week = card.week,
                    color = habitColor,
                    todayCompleted = card.completedToday,
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            CheckButton(
                checked = card.completedToday,
                color = habitColor,
                habitName = card.habit.name,
                onClick = onToggle,
            )
        }
    }
}

@Composable
private fun CheckButton(
    checked: Boolean,
    color: Color,
    habitName: String,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }

    // Bounce whenever the habit flips to completed.
    LaunchedEffect(checked) {
        if (checked) {
            scale.snapTo(0.7f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }

    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = Modifier
            .size(56.dp)
            .scale(scale.value)
            .then(
                if (checked) {
                    Modifier.background(color, CircleShape)
                } else {
                    Modifier.border(2.dp, color.copy(alpha = 0.6f), CircleShape)
                }
            ),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (checked) {
                MaterialTheme.colorScheme.surface
            } else {
                color
            },
        ),
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = if (checked) {
                stringResource(R.string.mark_not_done, habitName)
            } else {
                stringResource(R.string.mark_done, habitName)
            },
            modifier = Modifier.size(28.dp),
        )
    }
}

/**
 * Seven dots for the last seven days (today rightmost). Adjacent completed days
 * are joined by a colored line; today's dot is outlined and pulses while pending.
 */
@Composable
private fun DayChain(
    week: List<DayCell>,
    color: Color,
    todayCompleted: Boolean,
) {
    val pulse by if (todayCompleted) {
        remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    } else {
        rememberInfiniteTransition(label = "todayPulse").animateFloat(
            initialValue = 0.85f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "todayPulseScale",
        )
    }
    val emptyDot = DotEmpty

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
        ) {
            val slot = size.width / week.size
            val centerY = size.height / 2f
            val dotRadius = 5.dp.toPx()
            val lineWidth = 3.dp.toPx()

            fun centerX(index: Int) = slot * index + slot / 2f

            // Connecting lines between consecutive completed days.
            for (i in 0 until week.size - 1) {
                if (week[i].completed && week[i + 1].completed) {
                    drawLine(
                        color = color,
                        start = androidx.compose.ui.geometry.Offset(centerX(i) + dotRadius, centerY),
                        end = androidx.compose.ui.geometry.Offset(centerX(i + 1) - dotRadius, centerY),
                        strokeWidth = lineWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }

            week.forEachIndexed { index, day ->
                val cx = centerX(index)
                when {
                    day.completed -> drawCircle(
                        color = color,
                        radius = dotRadius,
                        center = androidx.compose.ui.geometry.Offset(cx, centerY),
                    )
                    day.isToday -> drawCircle(
                        color = color,
                        radius = dotRadius * pulse,
                        center = androidx.compose.ui.geometry.Offset(cx, centerY),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                    else -> drawCircle(
                        color = emptyDot,
                        radius = dotRadius,
                        center = androidx.compose.ui.geometry.Offset(cx, centerY),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            week.forEach { day ->
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
