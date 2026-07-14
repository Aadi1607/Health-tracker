package io.github.aadi1607.habittracker.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.aadi1607.habittracker.R
import io.github.aadi1607.habittracker.data.db.Habit
import io.github.aadi1607.habittracker.ui.theme.HabitPalette

@Composable
fun HomeRoute(
    onOpenStats: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reminder by viewModel.reminder.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var actionsHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var habitIdToDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = userMessage?.let { stringResource(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            viewModel.messageShown()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::exportTo) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importFrom) }

    // First run: ask for notification permission and switch reminders on by
    // default, so the app notifies out of the box.
    val context = LocalContext.current
    val onboarded by viewModel.onboarded.collectAsStateWithLifecycle(initialValue = true)
    val firstRunPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> viewModel.completeFirstRunSetup() }
    LaunchedEffect(onboarded) {
        if (!onboarded) {
            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                firstRunPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.completeFirstRunSetup()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            HomeHeader(
                doneToday = state.doneToday,
                total = state.total,
                progress = state.progress,
                onOpenStats = onOpenStats,
                onOpenSettings = { showSettingsSheet = true },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_habit_title),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (state.loaded && state.habits.isEmpty() && state.archived.isEmpty()) {
            EmptyState(
                onAddClick = { showAddSheet = true },
                onAddSuggested = { name, emoji, color, target, period ->
                    viewModel.addHabit(name, emoji, color, target, period)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.habits, key = { it.habit.id }) { card ->
                    HabitCard(
                        card = card,
                        onTap = { viewModel.tap(card) },
                        onLongPress = { actionsHabitId = card.habit.id },
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 16.dp),
                    )
                }
                if (state.archived.isNotEmpty()) {
                    item(key = "archived-header") {
                        Text(
                            text = stringResource(R.string.archived_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                    }
                    items(state.archived, key = { "archived-${it.habit.id}" }) { card ->
                        ArchivedHabitRow(
                            card = card,
                            onUnarchive = { viewModel.archiveHabit(card.habit.id, archived = false) },
                            onDelete = { habitIdToDelete = card.habit.id },
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 16.dp),
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(88.dp)) }
            }
        }
    }
    ConfettiOverlay(celebrate = state.allDone)
    }

    if (showAddSheet) {
        HabitFormSheet(
            initial = null,
            onDismiss = { showAddSheet = false },
            onSave = { name, emoji, color, dailyTarget, goalPeriod ->
                viewModel.addHabit(name, emoji, color, dailyTarget, goalPeriod)
                showAddSheet = false
            },
        )
    }

    val editingHabit = state.habits.firstOrNull { it.habit.id == editingHabitId }?.habit
    if (editingHabit != null) {
        HabitFormSheet(
            initial = editingHabit,
            onDismiss = { editingHabitId = null },
            onSave = { name, emoji, color, dailyTarget, goalPeriod ->
                viewModel.updateHabit(editingHabit.id, name, emoji, color, dailyTarget, goalPeriod)
                editingHabitId = null
            },
        )
    }

    val actionsCard = state.habits.firstOrNull { it.habit.id == actionsHabitId }
    if (actionsCard != null) {
        val index = state.habits.indexOf(actionsCard)
        HabitActionsSheet(
            habitName = actionsCard.habit.name,
            canMoveUp = index > 0,
            canMoveDown = index < state.habits.lastIndex,
            onDismiss = { actionsHabitId = null },
            onEdit = {
                editingHabitId = actionsCard.habit.id
                actionsHabitId = null
            },
            onMoveUp = { viewModel.moveHabit(actionsCard, up = true) },
            onMoveDown = { viewModel.moveHabit(actionsCard, up = false) },
            onArchive = {
                viewModel.archiveHabit(actionsCard.habit.id, archived = true)
                actionsHabitId = null
            },
            onDelete = {
                habitIdToDelete = actionsCard.habit.id
                actionsHabitId = null
            },
        )
    }

    if (showSettingsSheet) {
        SettingsSheet(
            dynamicColor = dynamicColor,
            reminder = reminder,
            onDismiss = { showSettingsSheet = false },
            onDynamicColorChange = viewModel::setDynamicColor,
            onReminderEnabledChange = viewModel::setReminderEnabled,
            onReminderTimeChange = viewModel::setReminderTime,
            onNudgesEnabledChange = viewModel::setNudgesEnabled,
            onNudgeIntervalChange = viewModel::setNudgeInterval,
            onTestNotification = viewModel::sendTestNotification,
            onChangePassword = viewModel::changePassword,
            onExport = { exportLauncher.launch("habit-tracker-backup.json") },
            onImport = { importLauncher.launch(arrayOf("application/json")) },
            onLogout = {
                showSettingsSheet = false
                viewModel.logout()
            },
        )
    }

    val deletingId = habitIdToDelete
    if (deletingId != null) {
        val habitName = (state.habits + state.archived)
            .firstOrNull { it.habit.id == deletingId }?.habit?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { habitIdToDelete = null },
            title = { Text(stringResource(R.string.delete_habit_title)) },
            text = { Text(stringResource(R.string.delete_habit_message, habitName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHabit(deletingId)
                        habitIdToDelete = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { habitIdToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HabitActionsSheet(
    habitName: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            Text(
                text = habitName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEdit)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.padding(start = 16.dp))
                Text(
                    text = stringResource(R.string.edit_habit_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (canMoveUp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onMoveUp)
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 16.dp))
                    Text(
                        text = stringResource(R.string.move_up),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            if (canMoveDown) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onMoveDown)
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 16.dp))
                    Text(
                        text = stringResource(R.string.move_down),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onArchive)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_archive),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.padding(start = 16.dp))
                Text(
                    text = stringResource(R.string.archive),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDelete)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.padding(start = 16.dp))
                Text(
                    text = stringResource(R.string.delete),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ArchivedHabitRow(
    card: HabitCardUi,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = card.habit.emoji)
        Spacer(modifier = Modifier.padding(start = 12.dp))
        Text(
            text = card.habit.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onUnarchive) {
            Text(stringResource(R.string.unarchive))
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun HomeHeader(
    doneToday: Int,
    total: Int,
    progress: Float,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (total > 0) {
                    val allDone = doneToday == total
                    Text(
                        text = if (allDone) {
                            stringResource(R.string.all_done_header)
                        } else {
                            stringResource(R.string.progress_done, doneToday, total)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (allDone) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            IconButton(onClick = onOpenStats) {
                Icon(
                    painter = painterResource(R.drawable.ic_stats),
                    contentDescription = stringResource(R.string.stats_title),
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                )
            }
        }
        if (total > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 500),
                label = "headerProgress",
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .height(6.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

private data class SuggestedHabit(
    val nameRes: Int,
    val emoji: String,
    val color: Long,
    val target: Int,
    val period: String,
)

private val Suggestions = listOf(
    SuggestedHabit(R.string.suggest_water, "💧", HabitPalette[3], 8, Habit.PERIOD_DAILY),
    SuggestedHabit(R.string.suggest_walk, "🚶", HabitPalette[2], 1, Habit.PERIOD_DAILY),
    SuggestedHabit(R.string.suggest_read, "📖", HabitPalette[1], 1, Habit.PERIOD_DAILY),
    SuggestedHabit(R.string.suggest_meditate, "🧘", HabitPalette[4], 1, Habit.PERIOD_DAILY),
    SuggestedHabit(R.string.suggest_gym, "💪", HabitPalette[0], 3, Habit.PERIOD_WEEKLY),
    SuggestedHabit(R.string.suggest_sleep, "😴", HabitPalette[5], 1, Habit.PERIOD_DAILY),
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EmptyState(
    onAddClick: () -> Unit,
    onAddSuggested: (name: String, emoji: String, color: Long, target: Int, period: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.empty_emoji),
            style = MaterialTheme.typography.displayMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.empty_cta))
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.suggest_title),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Suggestions.forEach { suggestion ->
                val name = stringResource(suggestion.nameRes)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable {
                            onAddSuggested(
                                name,
                                suggestion.emoji,
                                suggestion.color,
                                suggestion.target,
                                suggestion.period,
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = suggestion.emoji)
                    Spacer(modifier = Modifier.padding(start = 6.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
