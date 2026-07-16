package io.github.aadi1607.tiffintracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aadi1607.tiffintracker.data.AppSettings
import io.github.aadi1607.tiffintracker.ui.billing.BillingScreen
import io.github.aadi1607.tiffintracker.ui.calendar.CalendarScreen
import io.github.aadi1607.tiffintracker.ui.home.HomeScreen
import io.github.aadi1607.tiffintracker.ui.settings.SettingsScreen
import io.github.aadi1607.tiffintracker.ui.stats.StatsScreen
import io.github.aadi1607.tiffintracker.ui.theme.TiffinTheme

enum class AppTab(val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.nav_home, Icons.Filled.Home),
    CALENDAR(R.string.nav_calendar, Icons.Filled.CalendarMonth),
    BILLING(R.string.nav_billing, Icons.Filled.Payments),
    STATS(R.string.nav_stats, Icons.Filled.BarChart),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings),
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = TiffinApplication.from(this)
        setContent {
            val settings by app.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            TiffinTheme(themeMode = settings.themeMode) {
                NotificationPermissionRequester()
                MainScaffold()
            }
        }
    }
}

@Composable
private fun NotificationPermissionRequester() {
    if (Build.VERSION.SDK_INT < 33) return
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* the app works fine without it; reminders just stay silent */ }
    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun MainScaffold() {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(padding),
            label = "tab",
        ) { tab ->
            when (tab) {
                AppTab.HOME -> HomeScreen()
                AppTab.CALENDAR -> CalendarScreen()
                AppTab.BILLING -> BillingScreen()
                AppTab.STATS -> StatsScreen()
                AppTab.SETTINGS -> SettingsScreen()
            }
        }
    }
}
