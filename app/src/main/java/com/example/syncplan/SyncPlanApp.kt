package com.example.sharedplanner

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sharedplanner.ui.auth.LoginScreen
import com.example.sharedplanner.ui.calendar.CalendarScreen
import com.example.sharedplanner.ui.groups.GroupsScreen
import com.example.sharedplanner.ui.profile.ProfileScreen
import com.example.sharedplanner.viewmodel.AuthViewModel
import com.example.sharedplanner.viewmodel.CalendarViewModel
import com.example.sharedplanner.viewmodel.GroupViewModel
import com.example.syncplan.SplashScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Calendar : Screen("calendar", "Kalendarz", Icons.Filled.CalendarToday)
    object Groups : Screen("groups", "Grupy", Icons.Filled.Group)
    object Profile : Screen("profile", "Profil", Icons.Filled.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncPlanApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val calendarViewModel: CalendarViewModel = viewModel()
    val groupViewModel: GroupViewModel = viewModel()

    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen {
            showSplash = false
        }
    } else {
        if (isLoggedIn) { // if (!is LoggedIn) PAMIETAC O ZAPRZECZENIU
            LoginScreen(authViewModel = authViewModel)
        } else {
            val items = listOf(Screen.Calendar, Screen.Groups, Screen.Profile)

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        items.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Calendar.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Calendar.route) {
                        CalendarScreen(
                            calendarViewModel = calendarViewModel,
                            groupViewModel = groupViewModel
                        )
                    }
                    composable(Screen.Groups.route) {
                        GroupsScreen(
                            groupViewModel = groupViewModel,
                            calendarViewModel = calendarViewModel
                        )
                    }
                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            authViewModel = authViewModel,
                            calendarViewModel = calendarViewModel
                        )
                    }
                }
            }
        }
    }
}