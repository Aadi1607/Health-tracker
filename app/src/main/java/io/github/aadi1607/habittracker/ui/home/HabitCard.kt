package io.github.aadi1607.habittracker.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habitColor = Color(card.habit.color)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
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
                        val streakText = when {
                            card.streak <= 0 -> stringResource(R.string.no_streak_yet)
                            card.habit.isWeekly ->
                                pluralStringResource(R.plurals.streak_weeks, card.streak, card.streak)
                            else ->
                                pluralStringResource(R.plurals.streak_days, card.streak, card.streak)
                        }
                        Text(
                            text = when {
                                card.habit.isWeekly -> stringResource(
                                    R.string.card_subtitle_with_count_week,
                                    card.periodCount.coerceAtMost(card.habit.dailyTarget),
                                    card.habit.dailyTarget,
                                    streakText,
                                )
                                card.habit.dailyTarget > 1 -> stringResource(
                                    R.string.card_subtitle_with_count,
                                    card.periodCount.coerceAtMost(card.habit.dailyTarget),
                                    card.habit.dailyTarget,
                                    streakText,
                                )
                                else -> streakText
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
                    todayCompleted = card.completedNow,
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            CheckButton(
                count = card.periodCount,
                target = card.habit.dailyTarget,
                color = habitColor,
                habitName = card.habit.name,
                onClick = onTap,
            )
        }
    }
}

/**
 * Circular tap target: a progress ring fills as the habit is logged during the
 * day and turns into a solid check once the daily target is reached. For
 * single-target habits it behaves like a plain check toggle.
 */
@Composable
private fun CheckButton(
    count: Int,
    target: Int,
    color: Color,
    habitName: String,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    val checked = count >= target

    val ringProgress by animateFloatAsState(
        targetValue = (count.toFloat() / target).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "ringProgress",
    )

    // Bounce whenever the habit reaches its target.
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

    val trackColor = color.copy(alpha = 0.25f)
    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = Modifier
            .size(56.dp)
            .scale(scale.value)
            .drawBehind {
                if (!checked) {
                    val stroke = 3.dp.toPx()
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(stroke / 2f, stroke / 2f)
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * ringProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            .then(
                if (checked) {
                    Modifier.background(color, CircleShape)
                } else {
                    Modifier
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
        val description = if (checked) {
            stringResource(R.string.mark_not_done, habitName)
        } else {
            stringResource(R.string.mark_done, habitName)
        }
        if (!checked && target > 1) {
            Text(
                text = stringResource(R.string.count_of_target, count, target),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { contentDescription = description },
            )
        } else {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = description,
                modifier = Modifier.size(28.dp),
            )
        }
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
                        start = Offset(centerX(i) + dotRadius, centerY),
                        end = Offset(centerX(i + 1) - dotRadius, centerY),
                        strokeWidth = lineWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }

            week.forEachIndexed { index, day ->
                val cx = centerX(index)
                val center = Offset(cx, centerY)
                when {
                    day.completed -> drawCircle(
                        color = color,
                        radius = dotRadius,
                        center = center,
                    )
                    day.isToday -> {
                        drawCircle(
                            color = color,
                            radius = dotRadius * pulse,
                            center = center,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                        if (day.partial) {
                            drawCircle(
                                color = color.copy(alpha = 0.5f),
                                radius = dotRadius * 0.55f,
                                center = center,
                            )
                        }
                    }
                    day.partial -> drawCircle(
                        color = color.copy(alpha = 0.35f),
                        radius = dotRadius,
                        center = center,
                    )
                    else -> drawCircle(
                        color = emptyDot,
                        radius = dotRadius,
                        center = center,
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
