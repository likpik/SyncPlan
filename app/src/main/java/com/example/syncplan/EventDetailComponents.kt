package com.example.syncplan.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import com.example.syncplan.viewmodel.Event
import com.example.syncplan.viewmodel.RSVPStatus
import com.example.syncplan.viewmodel.RSVPResponse
import com.example.syncplan.viewmodel.ExtendedCalendarViewModel
import com.example.syncplan.viewmodel.ChatViewModel
import com.example.syncplan.viewmodel.GroupViewModel
import com.example.syncplan.utils.BillSplitCalculator
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailDialog(
    event: Event,
    groupViewModel: GroupViewModel,
    currentUserId: String,
    currentUserName: String,
    calendarViewModel: ExtendedCalendarViewModel,
    chatViewModel: ChatViewModel,
    billSplitCalculator: BillSplitCalculator,
    onDismiss: () -> Unit,
    onNavigateToGroupChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val groupChatId = event.groupId?.let { groupViewModel.getGroupById(it)?.chatId }
    val tabs = listOf("Szczegóły", "Potwierdź udział", "Rachunek")
    LaunchedEffect(Unit) {
        groupChatId?.let { chatId ->
            chatViewModel.markRSVPUpdatesAsRead(chatId, currentUserId)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                EventDetailHeader(
                    event = event,
                    onClose = onDismiss
                )

                if (groupChatId != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToGroupChat(groupChatId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Przejdź do czatu grupy")
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            text = { Text(title) },
                            selected = selectedTab == index,
                            onClick = { selectedTab = index }
                        )
                    }
                }


            }

                when (selectedTab) {
                    0 -> EventDetailsTab(event = event)
                    1 -> RSVPTab(
                        event = event,
                        currentUserId = currentUserId,
                        calendarViewModel = calendarViewModel,
                        chatViewModel = chatViewModel,
                        groupViewModel = groupViewModel,
                        currentUserName = currentUserName
                    )

                    2 -> BillSplitTab(
                        event = event,
                        billSplitCalculator = billSplitCalculator,
                        chatViewModel = chatViewModel
                    )
                }
            }
        }
    }


@Composable
fun EventDetailHeader(
    event: Event,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = event.startDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Zamknij")
        }
    }
}

@Composable
fun EventDetailsTab(
    event: Event,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EventInfoCard(
                title = "Opis",
                content = event.description.ifBlank { "Brak opisu" },
                icon = Icons.Default.Description
            )
        }

        item {
            EventInfoCard(
                title = "Czas trwania",
                content = "${event.startDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${event.endDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                icon = Icons.Default.Schedule
            )
        }

        event.location?.let { location ->
            item {
                EventInfoCard(
                    title = "Lokalizacja",
                    content = "${location.name}\n${location.address}",
                    icon = Icons.Default.LocationOn
                )
            }
        }

        item {
            EventInfoCard(
                title = "Organizator",
                content = event.createdBy,
                icon = Icons.Default.Person
            )
        }

        item {
            EventInfoCard(
                title = "Uczestnicy",
                content = "${event.attendees.size} osób",
                icon = Icons.Default.Group
            )
        }
    }
}

@Composable
fun RSVPTab(
    event: Event,
    currentUserId: String,
    calendarViewModel: ExtendedCalendarViewModel,
    chatViewModel: ChatViewModel,
    groupViewModel: GroupViewModel,
    currentUserName: String,
    modifier: Modifier = Modifier
) {
    val events by calendarViewModel.events.collectAsState()
    val currentEvent = events.find { it.id == event.id } ?: event
    val rsvpStats = calendarViewModel.getRSVPStats(currentEvent.id)
    val currentUserResponse = currentEvent.rsvpResponses[currentUserId]
    val groupChatId = event.groupId?.let { groupViewModel.getGroupById(it)?.chatId }

    var showFeedback by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RSVPStatsCard(rsvpStats = rsvpStats)
        }
        item {
            Text(
                text = "Twoja odpowiedź",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item {
            RSVPResponseSection(
                currentResponse = currentUserResponse?.status ?: RSVPStatus.PENDING,
                onResponseChanged = { status, note, guestCount ->
                    calendarViewModel.respondToRSVP(
                        eventId = event.id,
                        userId = currentUserId,
                        status = status,
                        note = note,
                        guestCount = guestCount
                    )
                    groupChatId?.let { chatId ->
                        chatViewModel.sendRSVPUpdateMessage(
                            chatId = chatId,
                            userName = currentUserName,
                            status = status.name,
                            eventId = event.id,
                            eventTitle = event.title
                        )
                    }
                    // 3. Feedback dla użytkownika (opcjonalnie)
                    showFeedback = true
                }
            )
        }
        item {
            if (showFeedback) {
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = {
                        TextButton(onClick = { showFeedback = false }) { Text("OK") }
                    }
                ) {
                    Text("Odpowiedź została zapisana i wysłana do czatu grupy.")
                }
            }
        }

        item {
            Text(
                text = "Odpowiedzi uczestników",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(currentEvent.rsvpResponses.values.toList()) { response ->
            RSVPResponseItem(response = response)
        }
    }
}


@Composable
fun BillSplitTab(
    event: Event,
    billSplitCalculator: BillSplitCalculator,
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val splitResults by billSplitCalculator.splitResults.collectAsState()

    LaunchedEffect(event) {
        val participantNames = event.attendees.associateWith { it }
        billSplitCalculator.setParticipantsFromEventAttendees(event.attendees, participantNames)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        BillSplitInterface(
            billSplitCalculator = billSplitCalculator,
            onSummaryGenerated = { summary ->
                event.chatId?.let { chatId ->
                    chatViewModel.sendBillSplitMessage(chatId, summary)
                }
            }
        )
    }
}

@Composable
fun EventInfoCard(
    title: String,
    content: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp, top = 2.dp)
            )

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = content,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RSVPStatsCard(
    rsvpStats: Map<RSVPStatus, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statystyki odpowiedzi",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(RSVPStatus.values()) { status ->
                    RSVPStatItem(
                        status = status,
                        count = rsvpStats[status] ?: 0
                    )
                }
            }
        }
    }
}

@Composable
fun RSVPStatItem(
    status: RSVPStatus,
    count: Int,
    modifier: Modifier = Modifier
) {
    val (statusText, color) = when (status) {
        RSVPStatus.ATTENDING -> "Biorą udział" to Color(0xFF4CAF50)
        RSVPStatus.DECLINED -> "Nie mogą" to Color(0xFFF44336)
        RSVPStatus.MAYBE -> "Może" to Color(0xFFFF9800)
        RSVPStatus.PENDING -> "Oczekuje" to Color(0xFF9E9E9E)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Text(
            text = statusText,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun RSVPResponseSection(
    currentResponse: RSVPStatus,
    onResponseChanged: (RSVPStatus, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStatus by remember { mutableStateOf(currentResponse) }
    var note by remember { mutableStateOf("") }
    var guestCount by remember { mutableStateOf(0) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            RSVPStatus.values().forEach { status ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status }
                    )

                    Text(
                        text = when (status) {
                            RSVPStatus.ATTENDING -> "Biorę udział"
                            RSVPStatus.DECLINED -> "Nie mogę"
                            RSVPStatus.MAYBE -> "Może"
                            RSVPStatus.PENDING -> "Jeszcze nie wiem"
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }


            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Dodatkowa notatka (opcjonalnie)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                maxLines = 3
            )

            Button(
                onClick = {
                    onResponseChanged(selectedStatus, note, guestCount)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Zapisz odpowiedź")
            }
        }
    }
}

@Composable
fun RSVPResponsesList(
    responses: List<RSVPResponse>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(responses) { response ->
            RSVPResponseItem(response = response)
        }
    }
}

@Composable
fun RSVPResponseItem(
    response: RSVPResponse,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (response.status) {
        RSVPStatus.ATTENDING -> "Bierze udział" to Color(0xFF4CAF50)
        RSVPStatus.DECLINED -> "Nie może" to Color(0xFFF44336)
        RSVPStatus.MAYBE -> "Możliwe" to Color(0xFFFF9800)
        RSVPStatus.PENDING -> "Oczekuje" to Color(0xFF9E9E9E)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = response.userId,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = statusColor
                )

                if (response.note.isNotBlank()) {
                    Text(
                        text = response.note,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (response.status == RSVPStatus.ATTENDING && response.guestCount > 0) {
                Text(
                    text = "+${response.guestCount}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BillSplitInterface(
    billSplitCalculator: BillSplitCalculator,
    onSummaryGenerated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Kalkulator rachunków",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Dodaj pozycje i podziel rachunek między uczestników",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Button(
                onClick = {
                    val summary = billSplitCalculator.generateSummaryText()
                    onSummaryGenerated(summary)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Wyślij podsumowanie do czatu")
            }
        }
    }
}
