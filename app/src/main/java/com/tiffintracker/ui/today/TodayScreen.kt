package com.tiffintracker.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tiffintracker.R
import com.tiffintracker.data.db.MealEntry
import com.tiffintracker.data.db.MealStatus
import com.tiffintracker.data.db.MealType
import com.tiffintracker.data.db.Provider
import com.tiffintracker.ui.theme.TiffinTrackerTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onNavigateToAddProvider: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAwayDialog by remember { mutableStateOf(false) }
    var showHolidayConfirm by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.todayDate.format(
                                DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.getDefault())
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.today_greeting),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (uiState.providers.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(visible = showFabMenu) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    showFabMenu = false
                                    showHolidayConfirm = true
                                },
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ) {
                                Icon(
                                    Icons.Outlined.EventBusy,
                                    contentDescription = stringResource(R.string.mark_holiday)
                                )
                            }
                            SmallFloatingActionButton(
                                onClick = {
                                    showFabMenu = false
                                    showAwayDialog = true
                                },
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = stringResource(R.string.mark_away)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            if (showFabMenu) Icons.Default.Close else Icons.Outlined.EventBusy,
                            contentDescription = stringResource(R.string.bulk_actions)
                        )
                    }
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.providers.isEmpty() -> {
                EmptyProviderState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onAddProvider = onNavigateToAddProvider
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.providers,
                        key = { it.id }
                    ) { provider ->
                        MealToggleCard(
                            provider = provider,
                            entries = uiState.entries.filter { it.providerId == provider.id },
                            onToggleStatus = { mealType, currentStatus ->
                                viewModel.toggleMealStatus(provider.id, mealType, currentStatus)
                            },
                            onUpdateQuantity = { mealType, qty ->
                                viewModel.updateQuantity(provider.id, mealType, qty)
                            },
                            onUpdateNote = { mealType, note ->
                                viewModel.updateNote(provider.id, mealType, note)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showHolidayConfirm) {
        AlertDialog(
            onDismissRequest = { showHolidayConfirm = false },
            title = { Text(stringResource(R.string.mark_holiday)) },
            text = { Text(stringResource(R.string.mark_holiday_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markHoliday()
                        showHolidayConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHolidayConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAwayDialog) {
        DateRangePickerDialog(
            onDismiss = { showAwayDialog = false },
            onConfirm = { startDate, endDate ->
                viewModel.markAwayRange(startDate, endDate)
                showAwayDialog = false
            }
        )
    }
}

@Composable
fun MealToggleCard(
    provider: Provider,
    entries: List<MealEntry>,
    onToggleStatus: (MealType, MealStatus) -> Unit,
    onUpdateQuantity: (MealType, Int) -> Unit,
    onUpdateNote: (MealType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = provider.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            val offeredMeals = buildList {
                if (provider.offersBreakfast) add(MealType.BREAKFAST)
                if (provider.offersLunch) add(MealType.LUNCH)
                if (provider.offersDinner) add(MealType.DINNER)
            }

            offeredMeals.forEach { mealType ->
                val entry = entries.find { it.mealType == mealType.name }
                val status = entry?.let { MealStatus.valueOf(it.status) } ?: MealStatus.NOT_ORDERED
                val quantity = entry?.quantity ?: 1
                val note = entry?.note ?: ""

                MealRow(
                    mealType = mealType,
                    status = status,
                    quantity = quantity,
                    note = note,
                    onToggle = { onToggleStatus(mealType, status) },
                    onQuantityChange = { onUpdateQuantity(mealType, it) },
                    onNoteChange = { onUpdateNote(mealType, it) }
                )

                if (mealType != offeredMeals.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MealRow(
    mealType: MealType,
    status: MealStatus,
    quantity: Int,
    note: String,
    onToggle: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onNoteChange: (String) -> Unit
) {
    var showNoteField by remember { mutableStateOf(note.isNotEmpty()) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Meal type icon + label
            Icon(
                imageVector = mealTypeIcon(mealType),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = mealTypeLabel(mealType),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            // Status toggle button - large and thumb-friendly
            StatusToggleButton(
                status = status,
                onClick = onToggle
            )
        }

        // Quantity selector appears when RECEIVED
        AnimatedVisibility(
            visible = status == MealStatus.RECEIVED,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.quantity),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = stringResource(R.string.decrease_quantity),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = { onQuantityChange(quantity + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.increase_quantity),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Note toggle
                IconButton(
                    onClick = { showNoteField = !showNoteField },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.add_note),
                        modifier = Modifier.size(16.dp),
                        tint = if (note.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Note field
        AnimatedVisibility(
            visible = showNoteField && status == MealStatus.RECEIVED,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, top = 4.dp),
                placeholder = { Text(stringResource(R.string.note_placeholder)) },
                textStyle = MaterialTheme.typography.bodySmall,
                singleLine = true
            )
        }
    }
}

@Composable
private fun StatusToggleButton(
    status: MealStatus,
    onClick: () -> Unit
) {
    val (containerColor, contentColor, icon, label) = when (status) {
        MealStatus.RECEIVED -> StatusButtonStyle(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.Default.Check,
            label = stringResource(R.string.status_received)
        )
        MealStatus.SKIPPED -> StatusButtonStyle(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            icon = Icons.Default.Close,
            label = stringResource(R.string.status_skipped)
        )
        MealStatus.NOT_ORDERED -> StatusButtonStyle(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = null,
            label = stringResource(R.string.status_not_ordered)
        )
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

private data class StatusButtonStyle(
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
    val icon: ImageVector?,
    val label: String
)

@Composable
private fun EmptyProviderState(
    modifier: Modifier = Modifier,
    onAddProvider: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.LunchDining,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_providers_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_providers_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddProvider) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_provider))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    val state = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val startMillis = state.selectedStartDateMillis
                    val endMillis = state.selectedEndDateMillis
                    if (startMillis != null && endMillis != null) {
                        val startDate = Instant.ofEpochMilli(startMillis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        val endDate = Instant.ofEpochMilli(endMillis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        onConfirm(startDate, endDate)
                    }
                },
                enabled = state.selectedStartDateMillis != null &&
                        state.selectedEndDateMillis != null
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DateRangePicker(
            state = state,
            title = {
                Text(
                    text = stringResource(R.string.mark_away),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            },
            modifier = Modifier.height(500.dp)
        )
    }
}

@Composable
private fun mealTypeLabel(mealType: MealType): String = when (mealType) {
    MealType.BREAKFAST -> stringResource(R.string.breakfast)
    MealType.LUNCH -> stringResource(R.string.lunch)
    MealType.DINNER -> stringResource(R.string.dinner)
}

private fun mealTypeIcon(mealType: MealType): ImageVector = when (mealType) {
    MealType.BREAKFAST -> Icons.Outlined.FreeBreakfast
    MealType.LUNCH -> Icons.Outlined.LunchDining
    MealType.DINNER -> Icons.Outlined.DinnerDining
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    TiffinTrackerTheme {
        val sampleProvider = Provider(
            id = 1L,
            name = "Sharma Tiffin",
            phone = "9876543210",
            breakfastPrice = 50.0,
            lunchPrice = 80.0,
            dinnerPrice = 70.0,
            offersBreakfast = true,
            offersLunch = true,
            offersDinner = true,
            billingCycleStartDay = 1,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        val sampleEntries = listOf(
            MealEntry(
                id = 1L,
                providerId = 1L,
                date = "2025-10-17",
                mealType = MealType.BREAKFAST.name,
                status = MealStatus.RECEIVED.name,
                quantity = 1,
                note = "",
                createdAt = System.currentTimeMillis()
            ),
            MealEntry(
                id = 2L,
                providerId = 1L,
                date = "2025-10-17",
                mealType = MealType.LUNCH.name,
                status = MealStatus.SKIPPED.name,
                quantity = 1,
                note = "",
                createdAt = System.currentTimeMillis()
            )
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MealToggleCard(
                    provider = sampleProvider,
                    entries = sampleEntries,
                    onToggleStatus = { _, _ -> },
                    onUpdateQuantity = { _, _ -> },
                    onUpdateNote = { _, _ -> }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    TiffinTrackerTheme {
        EmptyProviderState(
            modifier = Modifier.fillMaxSize(),
            onAddProvider = {}
        )
    }
}
