package com.tiffintracker.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tiffintracker.R
import com.tiffintracker.ui.components.EmptyStateView
import com.tiffintracker.ui.components.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_stats)) })
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingView(modifier = Modifier.padding(innerPadding))
            uiState.monthlyData.isEmpty() -> {
                EmptyStateView(
                    icon = Icons.Filled.DateRange,
                    title = stringResource(R.string.empty_stats),
                    subtitle = stringResource(R.string.empty_stats_desc),
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MealsBarChart(data = uiState.monthlyData)
                    SpendingTrendChart(data = uiState.monthlyData)
                    SkipRateCard(data = uiState.monthlyData)
                    SummaryCard(data = uiState.monthlyData)
                }
            }
        }
    }
}

@Composable
private fun MealsBarChart(
    data: List<MonthlyStats>,
    modifier: Modifier = Modifier
) {
    val receivedColor = Color(0xFF2E7D32)
    val skippedColor = Color(0xFFC62828)
    val textColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.meals_per_month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawRect(receivedColor)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.status_received), style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawRect(skippedColor)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.status_skipped), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val maxVal = data.maxOf { it.receivedCount + it.skippedCount }.coerceAtLeast(1)
                val barGroupWidth = size.width / data.size
                val barWidth = barGroupWidth * 0.3f
                val bottomPadding = 30f

                val textPaint = android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }

                data.forEachIndexed { index, stats ->
                    val x = index * barGroupWidth + barGroupWidth * 0.15f
                    val chartHeight = size.height - bottomPadding

                    val receivedH = (stats.receivedCount.toFloat() / maxVal) * chartHeight
                    drawRoundRect(
                        color = receivedColor,
                        topLeft = Offset(x, chartHeight - receivedH),
                        size = Size(barWidth, receivedH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    val skippedH = (stats.skippedCount.toFloat() / maxVal) * chartHeight
                    drawRoundRect(
                        color = skippedColor,
                        topLeft = Offset(x + barWidth + 4f, chartHeight - skippedH),
                        size = Size(barWidth, skippedH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        stats.label.take(3),
                        x + barWidth,
                        size.height - 4f,
                        textPaint
                    )
                }
            }
        }
    }
}

@Composable
private fun SpendingTrendChart(
    data: List<MonthlyStats>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.spending_trend),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (data.isEmpty()) return@Canvas
                val maxSpent = data.maxOf { it.totalSpent }.coerceAtLeast(1.0)
                val bottomPadding = 30f
                val chartHeight = size.height - bottomPadding
                val segmentWidth = size.width / (data.size - 1).coerceAtLeast(1)

                val textPaint = android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }

                val points = data.mapIndexed { index, stats ->
                    val x = index * segmentWidth
                    val y = chartHeight - (stats.totalSpent / maxSpent * chartHeight).toFloat()
                    Offset(x, y)
                }

                // Draw the line
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }

                // Draw dots and labels
                points.forEachIndexed { index, point ->
                    drawCircle(
                        color = lineColor,
                        radius = 5f,
                        center = point
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        data[index].label.take(3),
                        point.x,
                        size.height - 4f,
                        textPaint
                    )
                }
            }
        }
    }
}

@Composable
private fun SkipRateCard(
    data: List<MonthlyStats>,
    modifier: Modifier = Modifier
) {
    val totalReceived = data.sumOf { it.receivedCount }
    val totalSkipped = data.sumOf { it.skippedCount }
    val total = totalReceived + totalSkipped
    val skipRate = if (total > 0) totalSkipped.toFloat() / total else 0f
    val skipPercent = (skipRate * 100).toInt()

    val progressColor = when {
        skipPercent < 10 -> Color(0xFF2E7D32)
        skipPercent < 25 -> Color(0xFFF9A825)
        else -> Color(0xFFC62828)
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(80.dp),
                    color = trackColor,
                    strokeWidth = 8.dp
                )
                CircularProgressIndicator(
                    progress = { skipRate },
                    modifier = Modifier.size(80.dp),
                    color = progressColor,
                    strokeWidth = 8.dp
                )
                Text(
                    text = "$skipPercent%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(
                    text = stringResource(R.string.skip_rate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$totalSkipped of $total meals skipped",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    data: List<MonthlyStats>,
    modifier: Modifier = Modifier
) {
    val totalReceived = data.sumOf { it.receivedCount }
    val totalSkipped = data.sumOf { it.skippedCount }
    val totalSpent = data.sumOf { it.totalSpent }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem(
                    label = "Total Received",
                    value = totalReceived.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryItem(
                    label = "Total Skipped",
                    value = totalSkipped.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryItem(
                    label = "Total Spent",
                    value = "₹${String.format("%,.0f", totalSpent)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SkipRatePreview() {
    MaterialTheme {
        SkipRateCard(
            data = listOf(
                MonthlyStats("Oct", 25, 5, 2000.0),
                MonthlyStats("Nov", 28, 2, 2400.0)
            )
        )
    }
}
