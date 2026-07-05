package io.github.aadi1607.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aadi1607.habittracker.ui.home.HomeRoute
import io.github.aadi1607.habittracker.ui.stats.StatsRoute
import io.github.aadi1607.habittracker.ui.theme.HabitTrackerTheme

private const val SCREEN_HOME = "home"
private const val SCREEN_STATS = "stats"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as HabitApplication).container
        setContent {
            val dynamicColor by container.settings.dynamicColor.collectAsStateWithLifecycle(false)
            HabitTrackerTheme(dynamicColor = dynamicColor) {
                HabitTrackerApp()
            }
        }
    }
}

@Composable
private fun HabitTrackerApp() {
    var screen by rememberSaveable { mutableStateOf(SCREEN_HOME) }

    BackHandler(enabled = screen != SCREEN_HOME) {
        screen = SCREEN_HOME
    }

    Crossfade(targetState = screen, label = "screens") { current ->
        when (current) {
            SCREEN_STATS -> StatsRoute(onBack = { screen = SCREEN_HOME })
            else -> HomeRoute(onOpenStats = { screen = SCREEN_STATS })
        }
    }
}
