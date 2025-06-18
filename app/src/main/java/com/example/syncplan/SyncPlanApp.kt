package com.example.syncplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Calendar : Screen("calendar", "Kalendarz", Icons.Filled.CalendarToday)
    object Groups : Screen("groups", "Grupy", Icons.Filled.Group)
    object Chat : Screen("chat", "Czaty", Icons.Filled.Chat)
    object Profile : Screen("profile", "Profil", Icons.Filled.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSyncPlanApp() {
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
        if (!isLoggedIn) { //ZMIENIĆ
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
                        EnhancedCalendarScreen(
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
                            calendarViewModel = calendarViewModel
                        )
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
fun EnhancedCalendarScreen(
    calendarViewModel: ExtendedCalendarViewModel,
    groupViewModel: GroupViewModel,
    chatViewModel: ChatViewModel,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate by calendarViewModel.selectedDate.collectAsState()
    val events by calendarViewModel.events.collectAsState()
    val availabilitySlots by calendarViewModel.availabilitySlots.collectAsState()
    val groups by groupViewModel.groups.collectAsState()
    val meetingSuggestions by calendarViewModel.meetingSuggestions.collectAsState()

    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAvailabilityDialog by remember { mutableStateOf(false) }
    var currentMonth by remember { mutableStateOf(LocalDate.now()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Calendar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Poprzedni miesiąc")
            }

            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("pl"))),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Następny miesiąc")
            }
        }

        // Calendar Week View
        CalendarWeekView(
            selectedDate = selectedDate,
            currentMonth = currentMonth,
            events = events,
            availabilitySlots = availabilitySlots,
            onDateSelected = { date -> calendarViewModel.selectDate(date) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddEventDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Wydarzenie")
            }

            OutlinedButton(
                onClick = { showAvailabilityDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Dostępność")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Events List
        Text(
            text = "Wydarzenia - ${selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("pl")))}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val dayEvents = calendarViewModel.getEventsForDate(selectedDate)
        val dayAvailability = calendarViewModel.getAvailabilityForDate(selectedDate)

        LazyColumn {
            items(dayEvents) { event ->
                EventCard(
                    event = event,
                    onEventClick = { onEventClick(event) }
                )
            }

            if (dayAvailability.isNotEmpty()) {
                item {
                    Text(
                        text = "Dostępność",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(dayAvailability) { slot ->
                    AvailabilityCard(availabilitySlot = slot)
                }
            }

            if (dayEvents.isEmpty() && dayAvailability.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Brak wydarzeń na ten dzień",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Dialogs
        if (showAddEventDialog) {
            AddEventDialog(
                selectedDate = selectedDate,
                groups = groups,
                onDismiss = { showAddEventDialog = false },
                onEventAdded = { title, description, startDateTime, endDateTime, attendees, location ->
                    calendarViewModel.addEvent(
                        title = title,
                        description = description,
                        startDateTime = startDateTime,
                        endDateTime = endDateTime,
                        createdBy = "current_user",
                        attendees = attendees,
                        location = location
                    )
                    showAddEventDialog = false
                }
            )
        }

        if (showAvailabilityDialog) {
            AvailabilityDialog(
                selectedDate = selectedDate,
                onDismiss = { showAvailabilityDialog = false },
                onAvailabilitySet = { date, startTime, endTime, isAvailable ->
                    calendarViewModel.addAvailabilitySlot(
                        userId = "current_user",
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        isAvailable = isAvailable
                    )
                    showAvailabilityDialog = false
                }
            )
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
