package com.example.syncplan.ui.calendar

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.syncplan.viewmodel.ExtendedCalendarViewModel
import com.example.syncplan.viewmodel.GroupViewModel
import com.example.syncplan.viewmodel.Event
import com.example.syncplan.viewmodel.AvailabilitySlot
import com.example.syncplan.viewmodel.ChatViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    calendarViewModel: ExtendedCalendarViewModel,
    groupViewModel: GroupViewModel,
    chatViewModel: ChatViewModel,
    onEventClick: (Event) -> Unit,
    groupId: String? = null,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedDate by calendarViewModel.selectedDate.collectAsState()
    val allEvents by calendarViewModel.events.collectAsState()
    val events = if (groupId != null) {
        val filtered = allEvents.filter { it.groupId == groupId }
        Log.d("CalendarDebug", "groupId=$groupId, matchedEvents=${filtered.size}")
        filtered
    } else {
        allEvents
    }
    val availabilitySlots by calendarViewModel.availabilitySlots.collectAsState()
    val groups by groupViewModel.groups.collectAsState()
    val meetingSuggestions by calendarViewModel.meetingSuggestions.collectAsState()

    LaunchedEffect(groupId) {
        Log.d("CalendarDebug", "LaunchedEffect fired with groupId=$groupId")
        groupId?.let {
            calendarViewModel.selectGroup(it)
        }
    }

    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAvailabilityDialog by remember { mutableStateOf(false) }
    var currentMonth by remember { mutableStateOf(LocalDate.now()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (groupId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onBackClick() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                }
                Text(
                    text = "Kalendarz grupy",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
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

        val dayEvents = events.filter { it.startDateTime.toLocalDate() == selectedDate }
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
                onEventAdded = { title, description, startDateTime, endDateTime, attendees, selectedGroupId, location ->
                    Log.d("CalendarDebug", "selectedGroupId = ${selectedGroupId}")
                    calendarViewModel.addEvent(
                        title = title,
                        description = description,
                        startDateTime = startDateTime,
                        endDateTime = endDateTime,
                        createdBy = "current_user",
                        attendees = attendees,
                        groupId = selectedGroupId,
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
fun CalendarWeekView(
    selectedDate: LocalDate,
    currentMonth: LocalDate,
    events: List<Event>,
    availabilitySlots: List<AvailabilitySlot>,
    onDateSelected: (LocalDate) -> Unit
) {
    Column {
        // Week days header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val dayNames = listOf("Pon", "Wt", "Śr", "Czw", "Pt", "Sob", "Nie")
            dayNames.forEach { dayName ->
                Text(
                    text = dayName,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid
        for (week in 0..5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (day in 0..6) {
                    val date = currentMonth.withDayOfMonth(1)
                        .minusDays(currentMonth.withDayOfMonth(1).dayOfWeek.value.toLong() - 1)
                        .plusDays((week * 7 + day).toLong())

                    if (date.month == currentMonth.month) {
                        CalendarDayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            hasEvents = events.any { it.startDateTime.toLocalDate() == date },
                            hasAvailability = availabilitySlots.any { it.date == date },
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    hasEvents: Boolean,
    hasAvailability: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> Color.Transparent
                }
            )
            .border(
                width = 1.dp,
                color = if (hasEvents) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = if (date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (hasEvents) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )
                }

                if (hasAvailability) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else Color(0xFF4CAF50),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}
