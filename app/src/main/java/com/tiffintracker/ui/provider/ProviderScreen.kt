package com.tiffintracker.ui.provider

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tiffintracker.R
import com.tiffintracker.data.db.Provider
import com.tiffintracker.ui.components.ConfirmDialog
import com.tiffintracker.ui.components.EmptyStateView
import com.tiffintracker.ui.components.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListScreen(
    onAddProvider: () -> Unit,
    onEditProvider: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ProviderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var providerToDelete by remember { mutableStateOf<Provider?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.providers)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProvider) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_provider))
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingView(modifier = Modifier.padding(innerPadding))
        } else if (uiState.providers.isEmpty()) {
            EmptyStateView(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.empty_providers),
                subtitle = stringResource(R.string.empty_providers_desc),
                actionLabel = stringResource(R.string.add_provider),
                onAction = onAddProvider,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.providers, key = { it.id }) { provider ->
                    ProviderCard(
                        provider = provider,
                        onEdit = { onEditProvider(provider.id) },
                        onDelete = { providerToDelete = provider }
                    )
                }
            }
        }
    }

    providerToDelete?.let { provider ->
        ConfirmDialog(
            title = stringResource(R.string.delete_provider),
            message = stringResource(R.string.confirm_delete_provider),
            onConfirm = {
                viewModel.deleteProvider(provider)
                providerToDelete = null
            },
            onDismiss = { providerToDelete = null }
        )
    }
}

@Composable
private fun ProviderCard(
    provider: Provider,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (provider.phone.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        Icons.Filled.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = provider.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(
                        onClick = {
                            val url = "https://wa.me/${provider.phone.replace("+", "").replace(" ", "")}"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("WhatsApp", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (provider.offersBreakfast) {
                    MealPriceChip(
                        label = stringResource(R.string.meal_breakfast),
                        price = provider.breakfastPrice
                    )
                }
                if (provider.offersLunch) {
                    MealPriceChip(
                        label = stringResource(R.string.meal_lunch),
                        price = provider.lunchPrice
                    )
                }
                if (provider.offersDinner) {
                    MealPriceChip(
                        label = stringResource(R.string.meal_dinner),
                        price = provider.dinnerPrice
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Billing cycle: Day ${provider.billingCycleStartDay}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MealPriceChip(label: String, price: Double) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = "$label: ₹${String.format("%.0f", price)}",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ── Add / Edit Provider Screen ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProviderScreen(
    providerId: Long?,
    onBack: () -> Unit,
    viewModel: ProviderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditing = providerId != null

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var offersBreakfast by remember { mutableStateOf(false) }
    var offersLunch by remember { mutableStateOf(true) }
    var offersDinner by remember { mutableStateOf(true) }
    var breakfastPrice by remember { mutableStateOf("") }
    var lunchPrice by remember { mutableStateOf("") }
    var dinnerPrice by remember { mutableStateOf("") }
    var billingCycleStartDay by remember { mutableStateOf("1") }
    var nameError by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(providerId) {
        if (providerId != null) {
            viewModel.loadProvider(providerId)
        }
    }

    LaunchedEffect(uiState.editingProvider) {
        if (isEditing && uiState.editingProvider != null && !initialized) {
            val p = uiState.editingProvider!!
            name = p.name
            phone = p.phone
            offersBreakfast = p.offersBreakfast
            offersLunch = p.offersLunch
            offersDinner = p.offersDinner
            breakfastPrice = if (p.breakfastPrice > 0) p.breakfastPrice.toLong().toString() else ""
            lunchPrice = if (p.lunchPrice > 0) p.lunchPrice.toLong().toString() else ""
            dinnerPrice = if (p.dinnerPrice > 0) p.dinnerPrice.toLong().toString() else ""
            billingCycleStartDay = p.billingCycleStartDay.toString()
            initialized = true
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) stringResource(R.string.edit_provider)
                        else stringResource(R.string.add_provider)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text(stringResource(R.string.provider_name)) },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Provider name is required") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.provider_phone)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Text(
                text = "Meal Types Offered",
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = offersBreakfast,
                    onClick = { offersBreakfast = !offersBreakfast },
                    label = { Text(stringResource(R.string.meal_breakfast)) }
                )
                FilterChip(
                    selected = offersLunch,
                    onClick = { offersLunch = !offersLunch },
                    label = { Text(stringResource(R.string.meal_lunch)) }
                )
                FilterChip(
                    selected = offersDinner,
                    onClick = { offersDinner = !offersDinner },
                    label = { Text(stringResource(R.string.meal_dinner)) }
                )
            }

            if (offersBreakfast) {
                OutlinedTextField(
                    value = breakfastPrice,
                    onValueChange = { breakfastPrice = it },
                    label = { Text(stringResource(R.string.provider_price_breakfast)) },
                    prefix = { Text("₹") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            if (offersLunch) {
                OutlinedTextField(
                    value = lunchPrice,
                    onValueChange = { lunchPrice = it },
                    label = { Text(stringResource(R.string.provider_price_lunch)) },
                    prefix = { Text("₹") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            if (offersDinner) {
                OutlinedTextField(
                    value = dinnerPrice,
                    onValueChange = { dinnerPrice = it },
                    label = { Text(stringResource(R.string.provider_price_dinner)) },
                    prefix = { Text("₹") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            OutlinedTextField(
                value = billingCycleStartDay,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    val num = filtered.toIntOrNull()
                    if (num == null || num in 1..31) {
                        billingCycleStartDay = filtered
                    }
                },
                label = { Text(stringResource(R.string.provider_billing_start)) },
                supportingText = { Text("Day of month (1-28)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val provider = Provider(
                        id = if (isEditing) providerId!! else 0L,
                        name = name.trim(),
                        phone = phone.trim(),
                        offersBreakfast = offersBreakfast,
                        offersLunch = offersLunch,
                        offersDinner = offersDinner,
                        breakfastPrice = breakfastPrice.toDoubleOrNull() ?: 0.0,
                        lunchPrice = lunchPrice.toDoubleOrNull() ?: 0.0,
                        dinnerPrice = dinnerPrice.toDoubleOrNull() ?: 0.0,
                        billingCycleStartDay = billingCycleStartDay.toIntOrNull() ?: 1
                    )
                    if (isEditing) viewModel.updateProvider(provider)
                    else viewModel.addProvider(provider)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }

    if (showDeleteDialog && isEditing) {
        ConfirmDialog(
            title = stringResource(R.string.delete_provider),
            message = stringResource(R.string.confirm_delete_provider),
            onConfirm = {
                uiState.editingProvider?.let { viewModel.deleteProvider(it) }
                showDeleteDialog = false
                onBack()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealPriceChipPreview() {
    MaterialTheme {
        MealPriceChip(label = "Lunch", price = 80.0)
    }
}
