package com.example.syncplan.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.syncplan.viewmodel.ChatMessage
import com.example.syncplan.viewmodel.ChatViewModel
import com.example.syncplan.viewmodel.MessageType
import com.example.syncplan.viewmodel.ChatInfo
import com.example.syncplan.viewmodel.RSVPStatus
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    chatViewModel: ChatViewModel,
    currentUserName: String = "Ja",
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val messages by chatViewModel.messages.collectAsState()
    val currentUserId by chatViewModel.currentUserId.collectAsState()
    val chatMessages = messages[chatId] ?: emptyList()
    val chatInfo = chatViewModel.getChatInfo(chatId)
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }
    LaunchedEffect(chatId, currentUserId) {
        chatViewModel.markAllMessagesAsRead(chatId, currentUserId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = chatInfo?.groupName ?: chatInfo?.eventTitle ?: "Czat",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(chatMessages) { message ->
                    ChatMessageItem(
                        message = message,
                        isCurrentUser = message.senderId == currentUserId,
                        currentUserName = currentUserName
                    )
                }
            }

            ChatInputField(
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onSendMessage = {
                    if (messageText.isNotBlank()) {
                        chatViewModel.sendMessage(
                            chatId = chatId,
                            content = messageText.trim(),
                            senderName = currentUserName
                        )
                        messageText = ""
                    }
                },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isCurrentUser: Boolean,
    currentUserName: String,
    modifier: Modifier = Modifier
) {
    val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = when {
        message.type == MessageType.SYSTEM -> MaterialTheme.colorScheme.surfaceVariant
        isCurrentUser -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val textColor = when {
        message.type == MessageType.SYSTEM -> MaterialTheme.colorScheme.onSurfaceVariant
        isCurrentUser -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        when (message.type) {
            MessageType.SYSTEM -> {
                SystemMessageBubble(
                    message = message,
                    backgroundColor = bubbleColor,
                    textColor = textColor
                )
            }
            MessageType.RSVP_UPDATE -> {
                RSVPUpdateBubble(
                    message = message,
                    backgroundColor = Color(0xFFE8F5E8),
                    textColor = Color(0xFF2E7D32)
                )
            }
            MessageType.WEATHER_UPDATE -> {
                WeatherUpdateBubble(
                    message = message,
                    backgroundColor = Color(0xFFE3F2FD),
                    textColor = Color(0xFF1976D2)
                )
            }
            MessageType.BILL_SPLIT -> {
                BillSplitBubble(
                    message = message,
                    backgroundColor = Color(0xFFFFF3E0),
                    textColor = Color(0xFFE65100)
                )
            }
            MessageType.EVENT_UPDATE -> {
                EventUpdateBubble(
                    message = message,
                    backgroundColor = Color(0xFFF3E5F5),
                    textColor = Color(0xFF7B1FA2)
                )
            }
            else -> {
                RegularMessageBubble(
                    message = message,
                    isCurrentUser = isCurrentUser,
                    backgroundColor = bubbleColor,
                    textColor = textColor,
                    currentUserName = currentUserName
                )
            }
        }
    }
}

@Composable
fun RegularMessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean,
    backgroundColor: Color,
    textColor: Color,
    currentUserName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 280.dp),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isCurrentUser) 16.dp else 4.dp,
            bottomEnd = if (isCurrentUser) 4.dp else 16.dp
        ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            if (!isCurrentUser && message.senderId != "system") {
                Text(
                    text = message.senderName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Text(
                text = message.content,
                fontSize = 14.sp,
                color = textColor
            )

            Text(
                text = message.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.7f),
                textAlign = if (isCurrentUser) TextAlign.End else TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SystemMessageBubble(
    message: ChatMessage,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(0.8f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message.content,
                fontSize = 13.sp,
                color = textColor,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = message.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun RSVPUpdateBubble(
    message: ChatMessage,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {

    val (icon, color) = when (message.rsvpStatus) {
        RSVPStatus.ATTENDING -> "✅" to Color(0xFF4CAF50)
        RSVPStatus.DECLINED -> "❌" to Color(0xFFF44336)
        RSVPStatus.MAYBE -> "❓" to Color(0xFFFF9800)
        RSVPStatus.PENDING, null -> "⏳" to Color(0xFF9E9E9E)
    }


    Card(
        modifier = modifier.fillMaxWidth(0.9f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {

                message.eventTitle?.let {
                    Text(
                        text = "Wydarzenie: $it",
                        fontSize = 12.sp,
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    color = color,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = message.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun WeatherUpdateBubble(
    message: ChatMessage,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(0.9f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🌤️",
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = message.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun BillSplitBubble(
    message: ChatMessage,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(0.9f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💰",
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = message.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun EventUpdateBubble(
    message: ChatMessage,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(0.9f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📅",
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = message.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputField(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                placeholder = { Text("Napisz wiadomość...") },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            IconButton(
                onClick = onSendMessage,
                enabled = messageText.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Wyślij",
                    tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatListItem(
    chatInfo: ChatInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (chatInfo.unreadCount > 0)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chatInfo.groupName?.firstOrNull()?.uppercase()
                        ?: chatInfo.eventTitle?.firstOrNull()?.uppercase()
                        ?: "C",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // NAZWA CZATU - wyróżniona
                Text(
                    text = chatInfo.groupName ?: chatInfo.eventTitle ?: "Czat",
                    fontWeight = FontWeight.SemiBold, // Zmiana z Medium na SemiBold
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface // Dodaj wyraźny kolor
                )

                // OSTATNIA WIADOMOŚĆ - pod nazwą
                chatInfo.lastMessage?.let { lastMessage ->
                    Spacer(modifier = Modifier.height(2.dp)) // Dodaj odstęp
                    val (rsvpIcon, rsvpColor) = when (lastMessage.rsvpStatus) {
                        RSVPStatus.ATTENDING -> "✅" to Color(0xFF4CAF50)
                        RSVPStatus.DECLINED -> "❌" to Color(0xFFF44336)
                        RSVPStatus.MAYBE -> "❓" to Color(0xFFFF9800)
                        RSVPStatus.PENDING, null -> "⏳" to Color(0xFF9E9E9E)
                    }

                    Text(
                        text = when (lastMessage.type) {
                            MessageType.SYSTEM -> "🔔 ${lastMessage.content}"
                            MessageType.RSVP_UPDATE -> "$rsvpIcon ${lastMessage.content}"
                            MessageType.WEATHER_UPDATE -> "🌤️ ${lastMessage.content}"
                            MessageType.BILL_SPLIT -> "💰 ${lastMessage.content}"
                            MessageType.EVENT_UPDATE -> "📅 ${lastMessage.content}"
                            else -> "${lastMessage.senderName}: ${lastMessage.content}"
                        },
                        fontSize = 13.sp,
                        color = if (lastMessage.type == MessageType.RSVP_UPDATE) rsvpColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } ?: run {
                    // Jeśli brak ostatniej wiadomości
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Brak wiadomości",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                chatInfo.lastMessage?.let { lastMessage ->
                    Text(
                        text = lastMessage.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (chatInfo.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chatInfo.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onError,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
