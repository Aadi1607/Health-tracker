package com.tiffintracker.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiffintracker.R
import com.tiffintracker.data.db.MealEntry
import com.tiffintracker.data.db.MealStatus
import com.tiffintracker.data.db.MealType
import com.tiffintracker.data.db.Provider
import com.tiffintracker.ui.theme.TiffinTrackerTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CalendarContent(
        uiState = uiState,
        onNavigateMonth = viewModel::navigateMonth,
        onDateSelected = viewModel::selectDate,
        onUpdateEntry = viewModel::updateEntry
    )
}

@Composable
private fun CalendarContent(
    uiState: CalendarUiState,
    onNavigateMonth: (Boolean) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onUpdateEntry: (MealEntry) -> Unit
) {
    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            MonthHeader(
                yearMonth = uiState.yearMonth,
                onPrevious = { onNavigateMonth(false) },
                onNext = { onNavigateMonth(true) }
            )
        }

        item {
            CalendarGrid(
                yearMonth = uiState.yearMonth,
                dayStatusMap = uiState.dayStatusMap,
                selectedDate = uiState.selectedDate,
                onDateSelected = onDateSelected
            )
        }

        item {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
        }

        if (uiState.selectedDate != null) {
            item {
                Text(
                    text = uiState.selectedDate.let { date ->
                        "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${date.year}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (uiState.selectedDateEntries.isEmpty()) {
                item {
                    EmptyDayCard()
                }
            } else {
                val grouped = uiState.selectedDateEntries.groupBy { it.providerId }
                grouped.forEach { (providerId, entries) ->
                    val provider = uiState.providers.find { it.id == providerId }
                    item(key = "provider_$providerId") {
                        DayDetailCard(
                            providerName = provider?.name ?: "Unknown",
                            entries = entries,
                            onUpdateEntry = onUpdateEntry
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Select a day to view details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun MonthHeader(
    yearMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous month"
            )
        }

        AnimatedContent(
            targetState = yearMonth,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                } else {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            },
            label = "month_animation"
        ) { month ->
            Text(
                text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next month"
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    dayStatusMap: Map<LocalDate, DayStatus>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysOfWeek = remember {
        DayOfWeek.entries.map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }

    val firstDayOfMonth = yearMonth.atDay(1)
    val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value - 1) // Monday = 0
    val daysInMonth = yearMonth.lengthOfMonth()
    val today = LocalDate.now()

    // Previous month's trailing days
    val prevMonth = yearMonth.minusMonths(1)
    val prevMonthDays = prevMonth.lengthOfMonth()

    Column {
        // Day-of-week headers
        Row(Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Calendar cells: 6 rows of 7 days
        val totalCells = 42
        for (row in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - dayOfWeekOffset + 1

                    when {
                        dayNumber < 1 -> {
                            // Previous month
                            val prevDay = prevMonthDays + dayNumber
                            DayCell(
                                day = prevDay,
                                status = DayStatus.NONE,
                                isToday = false,
                                isSelected = false,
                                isCurrentMonth = false,
                                onClick = null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        dayNumber > daysInMonth -> {
                            // Next month
                            val nextDay = dayNumber - daysInMonth
                            if (nextDay <= 14) {
                                DayCell(
                                    day = nextDay,
                                    status = DayStatus.NONE,
                                    isToday = false,
                                    isSelected = false,
                                    isCurrentMonth = false,
                                    onClick = null,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        else -> {
                            val date = yearMonth.atDay(dayNumber)
                            val status = dayStatusMap[date] ?: DayStatus.NONE
                            DayCell(
                                day = dayNumber,
                                status = status,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                isCurrentMonth = true,
                                onClick = { onDateSelected(date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    status: DayStatus,
    isToday: Boolean,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val receivedColor = Color(0xFF4CAF50)
    val skippedColor = Color(0xFFE53935)
    val mixedColor = Color(0xFFFF9800)

    val backgroundColor = when {
        !isCurrentMonth -> Color.Transparent
        isSelected -> MaterialTheme.colorScheme.primary
        status == DayStatus.HAS_RECEIVED -> receivedColor.copy(alpha = 0.2f)
        status == DayStatus.ALL_SKIPPED -> skippedColor.copy(alpha = 0.2f)
        status == DayStatus.MIXED -> mixedColor.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderModifier = when {
        isToday && !isSelected -> Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        )
        else -> Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .then(borderModifier)
            .background(backgroundColor, CircleShape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DayDetailCard(
    providerName: String,
    entries: List<MealEntry>,
    onUpdateEntry: (MealEntry) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = providerName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            entries.forEach { entry ->
                DayEntryRow(entry = entry, onUpdateEntry = onUpdateEntry)
                if (entry != entries.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayEntryRow(
    entry: MealEntry,
    onUpdateEntry: (MealEntry) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editQuantity by remember(entry) { mutableIntStateOf(entry.quantity) }
    var editNote by remember(entry) { mutableStateOf(entry.note) }

    val mealLabel = when (entry.mealType) {
        MealType.BREAKFAST.name -> stringResource(R.string.meal_breakfast)
        MealType.LUNCH.name -> stringResource(R.string.meal_lunch)
        MealType.DINNER.name -> stringResource(R.string.meal_dinner)
        else -> entry.mealType
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mealLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MealStatus.entries.forEach { status ->
                    val isActive = entry.status == status.name
                    val chipColor = when (status) {
                        MealStatus.RECEIVED -> Color(0xFF4CAF50)
                        MealStatus.SKIPPED -> Color(0xFFE53935)
                        MealStatus.NOT_ORDERED -> MaterialTheme.colorScheme.outline
                    }

                    FilterChip(
                        selected = isActive,
                        onClick = {
                            onUpdateEntry(entry.copy(status = status.name))
                        },
                        label = {
                            Text(
                                text = when (status) {
                                    MealStatus.RECEIVED -> stringResource(R.string.status_received)
                                    MealStatus.SKIPPED -> stringResource(R.string.status_skipped)
                                    MealStatus.NOT_ORDERED -> stringResource(R.string.status_not_ordered)
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor.copy(alpha = 0.2f),
                            selectedLabelColor = chipColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = chipColor.copy(alpha = 0.3f),
                            selectedBorderColor = chipColor,
                            enabled = true,
                            selected = isActive
                        )
                    )
                }

                IconButton(
                    onClick = { isEditing = !isEditing },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isEditing) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = editQuantity.toString(),
                    onValueChange = { newVal ->
                        newVal.toIntOrNull()?.let { q ->
                            if (q in 1..10) {
                                editQuantity = q
                                onUpdateEntry(entry.copy(quantity = q))
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.quantity)) },
                    modifier = Modifier.width(80.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = editNote,
                    onValueChange = { newNote ->
                        editNote = newNote
                        onUpdateEntry(entry.copy(note = newNote))
                    },
                    label = { Text(stringResource(R.string.add_note)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        } else if (entry.quantity > 1 || entry.note.isNotBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (entry.quantity > 1) {
                    Text(
                        text = "×${entry.quantity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDayCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Text(
            text = stringResource(R.string.no_entries),
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarContentPreview() {
    val today = LocalDate.now()
    val yearMonth = YearMonth.from(today)

    val sampleStatusMap = mapOf(
        today.minusDays(5) to DayStatus.HAS_RECEIVED,
        today.minusDays(4) to DayStatus.HAS_RECEIVED,
        today.minusDays(3) to DayStatus.ALL_SKIPPED,
        today.minusDays(2) to DayStatus.MIXED,
        today.minusDays(1) to DayStatus.HAS_RECEIVED,
        today to DayStatus.HAS_RECEIVED
    )

    val sampleProvider = Provider(
        id = 1L,
        name = "Sharma Tiffin",
        phone = "9876543210",
        lunchPrice = 80.0,
        dinnerPrice = 90.0
    )

    val sampleEntries = listOf(
        MealEntry(
            id = 1L,
            providerId = 1L,
            date = today.toString(),
            mealType = MealType.LUNCH.name,
            status = MealStatus.RECEIVED.name,
            quantity = 1,
            note = ""
        ),
        MealEntry(
            id = 2L,
            providerId = 1L,
            date = today.toString(),
            mealType = MealType.DINNER.name,
            status = MealStatus.SKIPPED.name,
            quantity = 1,
            note = "Out for dinner"
        )
    )

    TiffinTrackerTheme {
        Surface {
            CalendarContent(
                uiState = CalendarUiState(
                    yearMonth = yearMonth,
                    dayStatusMap = sampleStatusMap,
                    selectedDate = today,
                    selectedDateEntries = sampleEntries,
                    providers = listOf(sampleProvider),
                    isLoading = false
                ),
                onNavigateMonth = {},
                onDateSelected = {},
                onUpdateEntry = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DayCellPreview() {
    TiffinTrackerTheme {
        Row {
            DayCell(15, DayStatus.HAS_RECEIVED, false, false, true, {}, Modifier.size(48.dp))
            DayCell(16, DayStatus.ALL_SKIPPED, false, false, true, {}, Modifier.size(48.dp))
            DayCell(17, DayStatus.MIXED, true, false, true, {}, Modifier.size(48.dp))
            DayCell(18, DayStatus.NONE, false, true, true, {}, Modifier.size(48.dp))
            DayCell(28, DayStatus.NONE, false, false, false, null, Modifier.size(48.dp))
        }
    }
}
