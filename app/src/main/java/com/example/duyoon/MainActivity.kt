package com.example.duyoon

import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.duyoon.ui.screens.*
import com.example.duyoon.ui.theme.ThimmaTheme
import com.example.duyoon.ui.viewmodel.ThimmaViewModel

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object People : Screen("people", R.string.nav_people, Icons.Default.Person)
    object History : Screen("history", R.string.nav_history, Icons.Default.List)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}

class MainActivity : AppCompatActivity() {
    private val viewModel: ThimmaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        setContent {
            val langCode by viewModel.languageCode.collectAsState()
            val isArabic = langCode.equals("ar", ignoreCase = true)
            val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection
            ) {
                ThimmaTheme {
                    val navController = rememberNavController()
                    val items = listOf(Screen.Home, Screen.People, Screen.History, Screen.Settings)

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route

                            if (currentRoute in items.map { it.route }) {
                                NavigationBar {
                                    items.forEach { screen ->
                                        NavigationBarItem(
                                            icon = { Icon(screen.icon, contentDescription = null) },
                                            label = { Text(stringResource(screen.titleRes)) },
                                            selected = currentRoute == screen.route,
                                            onClick = {
                                                if (currentRoute != screen.route) {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToAddDebt = { navController.navigate("add_debt") },
                                    onNavigateToPerson = { personId -> navController.navigate("person_detail/$personId") },
                                    onNavigateToHistory = { navController.navigate(Screen.History.route) }
                                )
                            }
                            composable(Screen.People.route) {
                                PeopleScreen(
                                    viewModel = viewModel,
                                    onNavigateToPersonDetail = { personId -> navController.navigate("person_detail/$personId") }
                                )
                            }
                            composable(Screen.History.route) {
                                HistoryScreen(viewModel = viewModel)
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(viewModel = viewModel)
                            }
                            composable("add_debt") {
                                AddDebtScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = "add_debt_for_person/{personId}",
                                arguments = listOf(navArgument("personId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val personId = backStackEntry.arguments?.getLong("personId") ?: 0L
                                AddDebtScreen(
                                    initialPersonId = personId,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = "person_detail/{personId}",
                                arguments = listOf(navArgument("personId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val personId = backStackEntry.arguments?.getLong("personId") ?: 0L
                                PersonDetailScreen(
                                    personId = personId,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onNavigateToAddDebtForPerson = { id -> navController.navigate("add_debt_for_person/$id") },
                                    onNavigateToEditDebt = { debtId -> navController.navigate("edit_debt/$debtId") }
                                )
                            }
                            composable(
                                route = "edit_debt/{debtId}",
                                arguments = listOf(navArgument("debtId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val debtId = backStackEntry.arguments?.getLong("debtId") ?: 0L
                                EditDebtScreen(
                                    debtId = debtId,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
