package com.tiffintracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tiffintracker.R
import com.tiffintracker.ui.bills.BillsScreen
import com.tiffintracker.ui.calendar.CalendarScreen
import com.tiffintracker.ui.provider.AddEditProviderScreen
import com.tiffintracker.ui.provider.ProviderListScreen
import com.tiffintracker.ui.settings.SettingsScreen
import com.tiffintracker.ui.stats.StatsScreen
import com.tiffintracker.ui.today.TodayScreen

sealed class Screen(val route: String) {
    data object Today : Screen("today")
    data object Calendar : Screen("calendar")
    data object Bills : Screen("bills")
    data object Stats : Screen("stats")
    data object Settings : Screen("settings")
    data object Providers : Screen("providers")
    data object AddProvider : Screen("provider/add")
    data object EditProvider : Screen("provider/edit/{providerId}") {
        fun createRoute(providerId: Long) = "provider/edit/$providerId"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiffinApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Today, R.string.nav_today, Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem(Screen.Calendar, R.string.nav_calendar, Icons.Filled.DateRange, Icons.Outlined.DateRange),
        BottomNavItem(Screen.Bills, R.string.nav_bills, Icons.Filled.Receipt, Icons.Outlined.Receipt),
        BottomNavItem(Screen.Stats, R.string.nav_stats, Icons.Filled.BarChart, Icons.Outlined.BarChart),
        BottomNavItem(Screen.Settings, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.screen.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.labelRes)
                                )
                            },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Today.route) {
                TodayScreen(
                    onNavigateToAddProvider = { navController.navigate(Screen.AddProvider.route) }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen()
            }
            composable(Screen.Bills.route) {
                BillsScreen()
            }
            composable(Screen.Stats.route) {
                StatsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(Screen.Providers.route) {
                ProviderListScreen(
                    onAddProvider = { navController.navigate(Screen.AddProvider.route) },
                    onEditProvider = { id ->
                        navController.navigate(Screen.EditProvider.createRoute(id))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AddProvider.route) {
                AddEditProviderScreen(
                    providerId = null,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.EditProvider.route,
                arguments = listOf(navArgument("providerId") { type = NavType.LongType })
            ) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getLong("providerId")
                AddEditProviderScreen(
                    providerId = providerId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
