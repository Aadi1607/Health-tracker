package io.github.aadi1607.habittracker.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import io.github.aadi1607.habittracker.R
import io.github.aadi1607.habittracker.data.SettingsRepository
import androidx.compose.ui.unit.dp

// Local app lock, not real authentication: the data on disk is not encrypted.
@Composable
fun LoginScreen(
    expectedPassword: String?,
    biometricAvailable: Boolean,
    onBiometric: () -> Unit,
    onSuccess: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }

    fun submit() {
        if (username.trim() == SettingsRepository.USERNAME && password == expectedPassword) {
            showError = false
            onSuccess()
        } else {
            showError = true
        }
    }

    // Offer the fingerprint sheet right away when biometrics are enrolled.
    LaunchedEffect(biometricAvailable) {
        if (biometricAvailable) onBiometric()
    }

    // One-shot entrance: everything fades and floats up on first composition.
    var appeared by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = appeared,
            enter = fadeIn(animationSpec = tween(600)) +
                slideInVertically(animationSpec = tween(600)) { it / 6 },
        ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.login_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                showError = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.username_label)) },
            singleLine = true,
            isError = showError,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                showError = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.password_label)) },
            singleLine = true,
            isError = showError,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        )
        AnimatedVisibility(
            visible = showError,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Text(
                text = stringResource(R.string.login_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { submit() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = username.isNotBlank() && password.isNotBlank() && expectedPassword != null,
        ) {
            Text(stringResource(R.string.login_button))
        }
        if (biometricAvailable) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onBiometric, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.biometric_button))
            }
        }
        }
        }
    }
}
