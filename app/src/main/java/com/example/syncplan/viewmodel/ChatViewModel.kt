package com.example.syncplan.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val type: MessageType = MessageType.TEXT,
    val readBy: Set<String> = emptySet(),
    val rsvpStatus: RSVPStatus? = null,
    val eventTitle: String? = null,
    val eventId: String? = null
)

enum class MessageType {
    TEXT,
    SYSTEM,
    RSVP_UPDATE,
    WEATHER_UPDATE,
    BILL_SPLIT,
    EVENT_UPDATE
}

data class ChatInfo(
    val id: String = UUID.randomUUID().toString(),
    val eventId: String? = null,
    val eventTitle: String? = null,
    val groupId: String? = null,
    val groupName: String? = null,
    val participants: List<String> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastMessage: ChatMessage? = null,
    val unreadCount: Int = 0
)

class ChatViewModel : ViewModel() {

    private val _chats = MutableStateFlow<List<ChatInfo>>(emptyList())
    val chats: StateFlow<List<ChatInfo>> = _chats.asStateFlow()

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentUserId = MutableStateFlow("current_user") // TODO: Get from AuthViewModel
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    fun setCurrentUser(userId: String) {
        _currentUserId.value = userId
    }

    fun updateChatParticipants(chatId: String, newParticipants: List<String>) {
        _chats.value = _chats.value.map { chat ->
            if (chat.id == chatId) {
                chat.copy(participants = newParticipants)
            } else {
                chat
            }
        }
    }


    fun createDirectChat(chatName: String, participants: List<String>, creatorId: String): String {
        val chatId = UUID.randomUUID().toString()
        // Upewnij się, że twórca jest na liście uczestników
        val allParticipants = (participants + creatorId).distinct()

        val chatInfo = ChatInfo(
            id = chatId,
            groupName = chatName, // Używamy tego pola do przechowywania nazwy
            participants = allParticipants,
            // eventId i groupId pozostają null
        )

        _chats.value = _chats.value + chatInfo
        _messages.value = _messages.value + (chatId to emptyList())

        // Wykorzystaj istniejącą funkcję do wysłania wiadomości systemowej
        sendSystemMessage(
            chatId = chatId,
            content = "Czat \"$chatName\" został utworzony."
        )

        return chatId
    }

    fun markSystemMessagesAsRead() {
        val currentUserId = _currentUserId.value
        _messages.value = _messages.value.mapValues { (chatId, messagesList) ->
            val chat = _chats.value.find { it.id == chatId }
            val participants = chat?.participants ?: emptyList()

            messagesList.map { message ->
                if (message.type == MessageType.SYSTEM && !message.readBy.contains(currentUserId)) {
                    message.copy(readBy = participants.toSet())
                } else {
                    message
                }
            }
        }
    }


    fun createGroupChat(group: Group): String {
        val chatId = UUID.randomUUID().toString()
        val chatInfo = ChatInfo(
            id = chatId,
            groupId = group.id,
            groupName = group.name,
            participants = group.members.map { it.userId }
        )

        _chats.value = _chats.value + chatInfo
        _messages.value = _messages.value + (chatId to emptyList())

        // Wyślij powitalną wiadomość systemową
        sendSystemMessage(
            chatId = chatId,
            content = "Czat dla grupy \"${group.name}\" został utworzony."
        )

        return chatId
    }

    fun sendMessage(chatId: String, content: String, senderName: String) {
        val senderId = _currentUserId.value
        val message = ChatMessage(
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            type = MessageType.TEXT
        )

        addMessageToChat(chatId, message)
        updateChatLastMessage(chatId, message)

        // Oznacz jako przeczytane przez nadawcę
        markMessageAsRead(message.id, senderId)
    }

    fun sendSystemMessage(chatId: String, content: String) {
        val message = ChatMessage(
            chatId = chatId,
            senderId = "system",
            senderName = "System",
            content = content,
            type = MessageType.SYSTEM
        )

        addMessageToChat(chatId, message)
        updateChatLastMessage(chatId, message)
    }

    fun sendRSVPUpdateMessage(
        chatId: String,
        userName: String,
        status: String,
        eventId: String,
        eventTitle: String
    ) {
        val rsvpStatus = RSVPStatus.valueOf(status)
        val content = when (rsvpStatus) {
            RSVPStatus.ATTENDING -> "$userName potwierdził/a udział w wydarzeniu"
            RSVPStatus.DECLINED -> "$userName nie będzie uczestniczyć w wydarzeniu"
            RSVPStatus.MAYBE -> "$userName być może weźmie udział w wydarzeniu"
            RSVPStatus.PENDING -> "$userName jeszcze nie wie, czy weźmie udział"
        }

        val message = ChatMessage(
            chatId = chatId,
            senderId = "system",
            senderName = "System",
            content = content,
            type = MessageType.RSVP_UPDATE,
            rsvpStatus = RSVPStatus.valueOf(status),
            eventId = eventId,
            eventTitle = eventTitle
        )

        addMessageToChat(chatId, message)
        updateChatLastMessage(chatId, message)
    }

    fun sendWeatherUpdateMessage(chatId: String, weatherInfo: String) {
        val content = "🌤️ Prognoza pogody: $weatherInfo"

        val message = ChatMessage(
            chatId = chatId,
            senderId = "system",
            senderName = "System",
            content = content,
            type = MessageType.WEATHER_UPDATE
        )

        addMessageToChat(chatId, message)
        updateChatLastMessage(chatId, message)
    }

    fun sendBillSplitMessage(chatId: String, billSummary: String) {
        val content = "💰 Rachunek został podzielony:\n$billSummary"

        val message = ChatMessage(
            chatId = chatId,
            senderId = "system",
            senderName = "System",
            content = content,
            type = MessageType.BILL_SPLIT
        )

        addMessageToChat(chatId, message)
        updateChatLastMessage(chatId, message)
    }

    fun sendEventUpdateMessage(chatId: String, eventTitle: String, updateType: String) {
        val content = when (updateType) {
            "UPDATED" -> "📅 Wydarzenie \"$eventTitle\" zostało zaktualizowane"
            "CANCELLED" -> "❌ Wydarzenie \"$eventTitle\" zostało anulowane"
            "TIME_CHANGED" -> "⏰ Zmieniono czas wydarzenia \"$eventTitle\""
            "LOCATION_CHANGED" -> "📍 Zmieniono lokalizację wydarzenia \"$eventTitle\""
            else -> "📅 Zaktualizowano wydarzenie \"$eventTitle\""
        }

        val message = ChatMessage(
            chatId = chatId,
            senderId = "system",
            senderName = "System",
            content = content,
            type = MessageType.EVENT_UPDATE
        )

        addMessageToChat(chatId, message)
        updateChatLastMessage(chatId, message)
    }

    fun selectChat(chatId: String) {
        _selectedChatId.value = chatId

        // Oznacz wszystkie wiadomości jako przeczytane przez bieżącego użytkownika
        markAllMessagesAsRead(chatId, _currentUserId.value)

        // Zresetuj licznik nieprzeczytanych wiadomości
        updateUnreadCount(chatId, 0)
    }

    fun getChatMessages(chatId: String): List<ChatMessage> {
        return _messages.value[chatId]?.sortedBy { it.timestamp } ?: emptyList()
    }

    fun getChatInfo(chatId: String): ChatInfo? {
        return _chats.value.find { it.id == chatId }
    }

    fun getEventChats(eventId: String): List<ChatInfo> {
        return _chats.value.filter { it.eventId == eventId }
    }

    fun getUserChats(userId: String): List<ChatInfo> {
        return _chats.value.map { chat ->
            val messages = _messages.value[chat.id] ?: emptyList()
            val unreadCount = messages.count {
                it.type != MessageType.SYSTEM && !it.readBy.contains(userId)
            }
            chat.copy(unreadCount = unreadCount)
        }
    }


    fun getUnreadChatsCount(userId: String): Int {
        return _chats.value.filter { chat ->
            chat.participants.contains(userId)
        }.count { chat ->
            val chatMessages = _messages.value[chat.id] ?: emptyList()
            val unreadCount = chatMessages.count { message ->
                !message.readBy.contains(userId) &&
                        message.senderId != userId &&
                        message.type != MessageType.SYSTEM // Wyłącz wiadomości systemowe
            }
            unreadCount > 0
        }
    }

    fun deleteMessage(messageId: String, chatId: String) {
        val currentMessages = _messages.value[chatId] ?: return
        val updatedMessages = currentMessages.filter { it.id != messageId }

        _messages.value = _messages.value.toMutableMap().apply {
            put(chatId, updatedMessages)
        }

        // Jeśli usunięta wiadomość była ostatnią, zaktualizuj ostatnią wiadomość w czacie
        val lastMessage = updatedMessages.maxByOrNull { it.timestamp }
        updateChatLastMessage(chatId, lastMessage)
    }

    fun addParticipantToChat(chatId: String, userId: String) {
        val chat = _chats.value.find { it.id == chatId } ?: return

        if (!chat.participants.contains(userId)) {
            val updatedParticipants = chat.participants + userId
            val updatedChat = chat.copy(participants = updatedParticipants)

            _chats.value = _chats.value.map { c ->
                if (c.id == chatId) updatedChat else c
            }

            // Wyślij wiadomość systemową o dodaniu uczestnika
            sendSystemMessage(chatId, "Nowy uczestnik dołączył do czatu")
        }
    }

    fun removeParticipantFromChat(chatId: String, userId: String) {
        val chat = _chats.value.find { it.id == chatId } ?: return

        if (chat.participants.contains(userId)) {
            val updatedParticipants = chat.participants.filter { it != userId }
            val updatedChat = chat.copy(participants = updatedParticipants)

            _chats.value = _chats.value.map { c ->
                if (c.id == chatId) updatedChat else c
            }

            // Wyślij wiadomość systemową o usunięciu uczestnika
            sendSystemMessage(chatId, "Uczestnik opuścił czat")
        }
    }

    private fun addMessageToChat(chatId: String, message: ChatMessage) {
        val currentMessages = _messages.value[chatId] ?: emptyList()
        val updatedMessages = currentMessages + message

        _messages.value = _messages.value.toMutableMap().apply {
            put(chatId, updatedMessages)
        }

        // Zwiększ licznik nieprzeczytanych wiadomości dla uczestników (oprócz nadawcy)
        val chat = _chats.value.find { it.id == chatId }
        chat?.let {
            val unreadCount = if (message.senderId != _currentUserId.value) it.unreadCount + 1 else it.unreadCount
            updateUnreadCount(chatId, unreadCount)
        }
    }

    private fun updateChatLastMessage(chatId: String, message: ChatMessage?) {
        _chats.value = _chats.value.map { chat ->
            if (chat.id == chatId) {
                chat.copy(lastMessage = message)
            } else {
                chat
            }
        }
    }

    private fun updateUnreadCount(chatId: String, count: Int) {
        _chats.value = _chats.value.map { chat ->
            if (chat.id == chatId) {
                chat.copy(unreadCount = count)
            } else {
                chat
            }
        }
    }

    private fun markMessageAsRead(messageId: String, userId: String) {
        _messages.value = _messages.value.mapValues { (_, messagesList) ->
            messagesList.map { message ->
                if (message.id == messageId) {
                    message.copy(readBy = message.readBy + userId)
                } else {
                    message
                }
            }
        }
    }

    fun markRSVPUpdatesAsRead(chatId: String, userId: String) {
        _messages.value = _messages.value.mapValues { (id, messagesList) ->
            if (id == chatId) {
                messagesList.map { msg ->
                    if (msg.type == MessageType.RSVP_UPDATE && !msg.readBy.contains(userId)) {
                        msg.copy(readBy = msg.readBy + userId)
                    } else msg
                }
            } else messagesList
        }
    }

    fun markAllMessagesAsRead(chatId: String, userId: String) {
        val currentMessages = _messages.value[chatId] ?: return
        val updatedMessages = currentMessages.map { message ->
            if (!message.readBy.contains(userId)) {
                message.copy(readBy = message.readBy + userId)
            } else {
                message
            }
        }

        _messages.value = _messages.value.toMutableMap().apply {
            put(chatId, updatedMessages)
        }
    }

    fun isMessageRead(messageId: String, userId: String): Boolean {
        _messages.value.values.flatten().find { it.id == messageId }?.let { message ->
            return message.readBy.contains(userId)
        }
        return false
    }

    fun getMessageReadStatus(messageId: String): Set<String> {
        _messages.value.values.flatten().find { it.id == messageId }?.let { message ->
            return message.readBy
        }
        return emptySet()
    }

    fun searchMessages(chatId: String, query: String): List<ChatMessage> {
        val chatMessages = _messages.value[chatId] ?: return emptyList()
        return chatMessages.filter { message ->
            message.content.contains(query, ignoreCase = true) ||
                    message.senderName.contains(query, ignoreCase = true)
        }.sortedByDescending { it.timestamp }
    }

    fun getMessagesFromDate(chatId: String, date: LocalDateTime): List<ChatMessage> {
        val chatMessages = _messages.value[chatId] ?: return emptyList()
        return chatMessages.filter { message ->
            message.timestamp.toLocalDate() == date.toLocalDate()
        }.sortedBy { it.timestamp }
    }
}
