package io.github.aadi1607.tiffintracker.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.aadi1607.tiffintracker.R
import io.github.aadi1607.tiffintracker.domain.BillCalculator
import java.time.format.DateTimeFormatter

private val monthLabel = DateTimeFormatter.ofPattern("MMM")

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                label = stringResource(R.string.stats_tiffins_this_cycle),
                value = "${state.cycleTiffins}",
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                label = stringResource(R.string.stats_days_skipped),
                value = "${state.cycleSkippedDays}",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                label = stringResource(R.string.stats_amount_due),
                value = BillCalculator.formatPaise(state.cycleDuePaise, state.currencySymbol),
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                label = stringResource(R.string.stats_amount_paid),
                value = BillCalculator.formatPaise(state.cyclePaidPaise, state.currencySymbol),
                modifier = Modifier.weight(1f),
            )
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.stats_last_six_months),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(12.dp))
                MonthlyBarChart(state)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.users.forEach { user ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(user.colorArgb))
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(user.name, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.stats_streak),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                state.users.forEach { user ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(user.colorArgb))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(user.name, Modifier.weight(1f))
                        Text(
                            "🔥 " + stringResource(
                                R.string.stats_streak_days,
                                state.streaks[user.id] ?: 0,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

/** Grouped bars: one group per month, one bar per user. */
@Composable
private fun MonthlyBarChart(state: StatsUiState) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxCount = state.monthly.maxOfOrNull { it.counts.maxOrNull() ?: 0 }?.coerceAtLeast(1) ?: 1
    val userColors = state.users.map { Color(it.colorArgb) }

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (state.monthly.isEmpty()) return@Canvas
        val labelSpace = 36f
        val chartHeight = size.height - labelSpace
        val groupWidth = size.width / state.monthly.size
        val barCount = state.users.size.coerceAtLeast(1)
        val barWidth = (groupWidth * 0.7f) / barCount
        val textPaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        state.monthly.forEachIndexed { groupIndex, monthBars ->
            val groupLeft = groupIndex * groupWidth + groupWidth * 0.15f
            monthBars.counts.forEachIndexed { userIndex, count ->
                val barHeight = chartHeight * (count.toFloat() / maxCount)
                val left = groupLeft + userIndex * barWidth
                drawRoundRect(
                    color = userColors.getOrElse(userIndex) { Color.Gray },
                    topLeft = Offset(left, chartHeight - barHeight),
                    size = Size(barWidth * 0.85f, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                )
                if (count > 0) {
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(
                            "$count",
                            left + barWidth * 0.42f,
                            (chartHeight - barHeight - 8f).coerceAtLeast(24f),
                            textPaint,
                        )
                    }
                }
            }
            drawIntoCanvas {
                it.nativeCanvas.drawText(
                    monthBars.month.atDay(1).format(monthLabel),
                    groupIndex * groupWidth + groupWidth / 2f,
                    size.height - 6f,
                    textPaint,
                )
            }
        }
    }
}
