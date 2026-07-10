package io.github.aadi1607.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aadi1607.habittracker.ui.home.HomeRoute
import io.github.aadi1607.habittracker.ui.login.LoginScreen
import io.github.aadi1607.habittracker.ui.stats.StatsRoute
import io.github.aadi1607.habittracker.ui.theme.HabitTrackerTheme
import kotlinx.coroutines.launch

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
                val loggedIn by container.settings.loggedIn
                    .collectAsStateWithLifecycle(initialValue = null)
                val scope = rememberCoroutineScope()
                Crossfade(targetState = loggedIn, label = "auth") { state ->
                    when (state) {
                        // Still reading the stored session: draw only the background
                        // to avoid a login-screen flash for signed-in users.
                        null -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                        )
                        false -> LoginScreen(
                            onSuccess = { scope.launch { container.settings.setLoggedIn(true) } },
                        )
                        else -> HabitTrackerApp()
                    }
                }
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

    // Stats slides in from the right and home slides back in from the left,
    // like a forward/back navigation pair.
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            if (targetState == SCREEN_STATS) {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()) togetherWith
                    (slideOutHorizontally { it / 3 } + fadeOut())
            }
        },
        label = "screens",
    ) { current ->
        when (current) {
            SCREEN_STATS -> StatsRoute(onBack = { screen = SCREEN_HOME })
            else -> HomeRoute(onOpenStats = { screen = SCREEN_STATS })
        }
    }
}
