package io.github.aadi1607.habittracker.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.aadi1607.habittracker.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsRoute(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val shareHeader = stringResource(R.string.share_header)
    val shareLine = stringResource(R.string.share_line)
    val shareChooser = stringResource(R.string.share_stats)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (state.habitStats.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val text = buildString {
                                    appendLine(shareHeader)
                                    state.habitStats.forEach { s ->
                                        appendLine(
                                            shareLine.format(
                                                s.habit.emoji,
                                                s.habit.name,
                                                s.currentStreak,
                                                s.bestStreak,
                                                (s.completionRate * 100).toInt(),
                                            )
                                        )
                                    }
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, shareChooser))
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share_stats),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthHeatmapCard(
                    month = state.month,
                    heatmap = state.heatmap,
                    onPreviousMonth = viewModel::previousMonth,
                    onNextMonth = viewModel::nextMonth,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                Text(
                    text = pluralStringResource(
                        R.plurals.total_completions,
                        state.totalCompletions,
                        state.totalCompletions,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            items(state.habitStats, key = { it.habit.id }) { stats ->
                HabitStatsCard(stats = stats, modifier = Modifier.padding(horizontal = 16.dp))
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun HabitStatsCard(
    stats: HabitStats,
    modifier: Modifier = Modifier,
) {
    val habitColor = Color(stats.habit.color)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(habitColor.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stats.habit.emoji)
                }
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = stats.habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                StatCell(
                    value = stats.currentStreak.toString(),
                    label = stringResource(R.string.current_streak),
                    modifier = Modifier.weight(1f),
                )
                StatCell(
                    value = stats.bestStreak.toString(),
                    label = stringResource(R.string.best_streak),
                    modifier = Modifier.weight(1f),
                )
                StatCell(
                    value = stringResource(
                        R.string.completion_rate_value,
                        (stats.completionRate * 100).toInt(),
                    ),
                    label = stringResource(R.string.completion_rate),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MonthHeatmapCard(
    month: YearMonth,
    heatmap: Map<LocalDate, Float>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.previous_month),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.month_year,
                        month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        month.year,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next_month),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            MonthGrid(month = month, heatmap = heatmap)
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    heatmap: Map<LocalDate, Float>,
) {
    val today = LocalDate.now()
    val firstDay = month.atDay(1)
    // Weeks start on Monday.
    val leadingBlanks = firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value
    val totalCells = leadingBlanks + month.lengthOfMonth()
    val weeks = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            for (i in 0 until 7) {
                Text(
                    text = DayOfWeek.MONDAY.plus(i.toLong())
                        .getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (week in 0 until weeks) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (dayOfWeek in 0 until 7) {
                    val cellIndex = week * 7 + dayOfWeek
                    val dayOfMonth = cellIndex - leadingBlanks + 1
                    Box(modifier = Modifier.weight(1f)) {
                        if (dayOfMonth in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayOfMonth)
                            val fraction = heatmap[date] ?: 0f
                            val cellColor = when {
                                date.isAfter(today) -> Color.Transparent
                                fraction <= 0f -> MaterialTheme.colorScheme.surfaceContainerHigh
                                else -> lerp(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.primary,
                                    0.25f + 0.75f * fraction,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(cellColor, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (fraction > 0.5f && !date.isAfter(today)) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
