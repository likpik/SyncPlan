package com.example.syncplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.syncplan.ui.auth.LoginScreen
import com.example.syncplan.ui.calendar.*
import com.example.syncplan.ui.groups.GroupsScreen
import com.example.syncplan.ui.profile.ProfileScreen
import com.example.syncplan.ui.event.EventDetailDialog
import com.example.syncplan.viewmodel.*
import com.example.syncplan.services.NotificationService
import com.example.syncplan.utils.BillSplitCalculator

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Calendar : Screen("calendar", "Kalendarz", Icons.Filled.CalendarToday)
    object Groups : Screen("groups", "Grupy", Icons.Filled.Group)
    object Chat : Screen("chat", "Czaty", Icons.AutoMirrored.Filled.Chat)
    object Profile : Screen("profile", "Profil", Icons.Filled.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncPlanApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    // ViewModels
    val authViewModel: AuthViewModel = viewModel()
    val calendarViewModel: ExtendedCalendarViewModel = viewModel()
    val groupViewModel: GroupViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val billSplitCalculator: BillSplitCalculator = viewModel()

    // Services
    val notificationService = remember { NotificationService(context) }

    // State
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    var showSplash by remember { mutableStateOf(true) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var showEventDetail by remember { mutableStateOf(false) }

    // Initialize chat with current user
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            chatViewModel.setCurrentUser(user.id)
        }
    }

    if (showSplash) {
        SplashScreen {
            showSplash = false
        }
    } else {
        if (!isLoggedIn) {
            LoginScreen(authViewModel = authViewModel)
        } else {
            val items = listOf(Screen.Calendar, Screen.Groups, Screen.Chat, Screen.Profile)

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        items.forEach { screen ->
                            val unreadCount = if (screen == Screen.Chat) {
                                chatViewModel.getUnreadChatsCount(currentUser?.id ?: "")
                            } else 0

                            NavigationBarItem(
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (unreadCount > 0) {
                                                Badge {
                                                    Text(text = unreadCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(screen.icon, contentDescription = screen.title)
                                    }
                                },
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
                            groupViewModel = groupViewModel,
                            chatViewModel = chatViewModel,
                            onEventClick = { event ->
                                selectedEvent = event
                                showEventDetail = true
                            }
                        )
                    }

                    composable(Screen.Groups.route) {
                        GroupsScreen(
                            groupViewModel = groupViewModel,
                            calendarViewModel = calendarViewModel,
                            onGroupClick = { group ->
                                navController.navigate("groupDetails/${group.id}")
                            }
                        )
                    }

                    composable(
                        route = "calendar/{groupId}",
                        arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId")
                        CalendarScreen(
                            calendarViewModel = calendarViewModel,
                            groupViewModel = groupViewModel,
                            chatViewModel = chatViewModel,
                            groupId = groupId,
                            onEventClick = { event ->
                                selectedEvent = event
                                showEventDetail = true
                            },
                            modifier = Modifier,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "groupDetails/{groupId}",
                        arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                        val group = groupViewModel.getGroupById(groupId)

                        if (group != null) {
                            GroupDetailScreen(
                                group = group,
                                groupViewModel = groupViewModel,
                                userSession = currentUser!!,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToCalendar = { groupId -> navController.navigate("calendar/$groupId") }
                            )
                        } else {
                            // Ekran błędu gdy grupa nie zostanie znaleziona
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Nie znaleziono grupy",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Text(
                                        text = "Grupa o ID: $groupId nie istnieje",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { navController.popBackStack() }) {
                                        Text("Wróć do listy grup")
                                    }
                                }
                            }
                        }
                    }

                    composable(Screen.Chat.route) {
                        ChatListScreen(
                            chatViewModel = chatViewModel,
                            currentUserId = currentUser?.id ?: "",
                            onChatClick = { chatId ->
                                chatViewModel.selectChat(chatId)
                            }
                        )
                    }

                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            authViewModel = authViewModel,
                            calendarViewModel = calendarViewModel
                        )
                    }
                }

                // Event Detail Dialog
                if (showEventDetail && selectedEvent != null) {
                    EventDetailDialog(
                        event = selectedEvent!!,
                        currentUserId = currentUser?.id ?: "",
                        currentUserName = currentUser?.name ?: "Nieznany użytkownik",
                        calendarViewModel = calendarViewModel,
                        chatViewModel = chatViewModel,
                        billSplitCalculator = billSplitCalculator,
                        onDismiss = {
                            showEventDetail = false
                            selectedEvent = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatListScreen(
    chatViewModel: ChatViewModel,
    currentUserId: String,
    onChatClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val userChats = chatViewModel.getUserChats(currentUserId)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Twoje czaty",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (userChats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Brak aktywnych czatów",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(userChats.size) { index ->
                    com.example.syncplan.ui.chat.ChatListItem(
                        chatInfo = userChats[index],
                        onClick = { onChatClick(userChats[index].id) }
                    )
                }
            }
        }
    }
}
