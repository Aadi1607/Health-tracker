package io.github.aadi1607.tiffintracker.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.aadi1607.tiffintracker.R
import io.github.aadi1607.tiffintracker.ui.components.DayEditorCard
import io.github.aadi1607.tiffintracker.ui.theme.StatusPending
import io.github.aadi1607.tiffintracker.ui.theme.StatusSkipped
import io.github.aadi1607.tiffintracker.ui.theme.StatusTaken
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy")
private val sheetTitleFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val daySheet by viewModel.daySheet.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::previousMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.calendar_previous_month),
                )
            }
            Text(
                state.month.atDay(1).format(monthFormat),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )
            IconButton(onClick = viewModel::nextMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.calendar_next_month),
                )
            }
        }

        // Weekday header, Monday-first.
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            DayOfWeek.values().forEach { dow ->
                Text(
                    dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val firstDay = state.month.atDay(1)
        val leadingBlanks = firstDay.dayOfWeek.value - 1 // Monday == 1
        val cells = List(leadingBlanks) { null } +
            (1..state.month.lengthOfMonth()).map { state.month.atDay(it) }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            items(cells) { date ->
                if (date == null) {
                    Box(Modifier.aspectRatio(0.8f))
                } else {
                    DayCell(
                        date = date,
                        isToday = date == today,
                        statuses = state.statuses[date.toEpochDay()].orEmpty(),
                        onClick = { viewModel.selectDay(date) },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Legend(userNames = state.users.map { it.name })
    }

    daySheet?.let { sheet ->
        ModalBottomSheet(onDismissRequest = { viewModel.selectDay(null) }) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(
                        R.string.calendar_edit_day,
                        sheet.date.format(sheetTitleFormat),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                sheet.users.forEach { user ->
                    DayEditorCard(
                        user = user,
                        log = sheet.logs[user.id] ?: io.github.aadi1607.tiffintracker.data.DayLog(),
                        onLogChange = { viewModel.updateLog(user.id, it) },
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    statuses: List<DayStatus>,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .aspectRatio(0.8f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isToday) {
                    Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(8.dp),
                    )
                } else Modifier
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "${date.dayOfMonth}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            statuses.forEach { status ->
                val color = when (status) {
                    DayStatus.TAKEN -> StatusTaken
                    DayStatus.SKIPPED -> StatusSkipped
                    DayStatus.PENDING -> StatusPending
                    DayStatus.FUTURE -> Color.Transparent
                }
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun Legend(userNames: List<String>) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendDot(StatusTaken, stringResource(R.string.legend_taken))
            LegendDot(StatusSkipped, stringResource(R.string.legend_skipped))
            LegendDot(StatusPending, stringResource(R.string.legend_pending))
        }
        if (userNames.size > 1) {
            Text(
                userNames.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
