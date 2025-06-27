package com.example.syncplan.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

data class Event(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val createdBy: String,
    val attendees: List<String> = emptyList(),
    val groupId: String?,
    val location: Location? = null,
    val isAvailability: Boolean = false,
    val rsvpResponses: Map<String, RSVPResponse> = emptyMap(),
    val rsvpDeadline: LocalDateTime? = null,
    val maxAttendees: Int? = null,
    val chatId: String? = null,
    val weatherInfo: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class Location(
    val name: String,
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class RSVPResponse(
    val userId: String,
    val status: RSVPStatus,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val note: String = "",
    val guestCount: Int = 0
)

enum class RSVPStatus {
    ATTENDING,
    DECLINED,
    MAYBE,
    PENDING
}

data class AvailabilitySlot(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isAvailable: Boolean = true
)

data class MeetingSuggestion(
    val suggestedDateTime: LocalDateTime,
    val duration: Int,
    val availableUsers: List<String>,
    val score: Double
)

class ExtendedCalendarViewModel : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _selectedEvent = MutableStateFlow<Event?>(null)
    val selectedEvent: StateFlow<Event?> = _selectedEvent.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _availabilitySlots = MutableStateFlow<List<AvailabilitySlot>>(emptyList())
    val availabilitySlots: StateFlow<List<AvailabilitySlot>> = _availabilitySlots.asStateFlow()

    private val _meetingSuggestions = MutableStateFlow<List<MeetingSuggestion>>(emptyList())
    val meetingSuggestions: StateFlow<List<MeetingSuggestion>> = _meetingSuggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun selectEvent(event: Event) {
        _selectedEvent.value = event
    }

    fun selectGroup(groupId: String) {
        _selectedGroupId.value = groupId
    }

    fun addEvent(
        title: String,
        description: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        createdBy: String,
        attendees: List<String> = emptyList(),
        groupId: String? = null,
        location: Location? = null,
        rsvpDeadline: LocalDateTime? = null,
        maxAttendees: Int? = null
    ) {
        val newEvent = Event(
            title = title,
            description = description,
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            createdBy = createdBy,
            attendees = attendees,
            groupId = groupId,
            location = location,
            rsvpDeadline = rsvpDeadline,
            maxAttendees = maxAttendees,
            rsvpResponses = attendees.associateWith { userId ->
                RSVPResponse(
                    userId = userId,
                    status = RSVPStatus.PENDING
                )
            }
        )

        _events.value = _events.value + newEvent
    }

    fun updateEvent(eventId: String, updatedEvent: Event) {
        _events.value = _events.value.map { event ->
            if (event.id == eventId) updatedEvent else event
        }
    }

    fun deleteEvent(eventId: String) {
        _events.value = _events.value.filter { it.id != eventId }
    }

    fun respondToRSVP(eventId: String, userId: String, status: RSVPStatus, note: String = "", guestCount: Int = 0) {
        val event = _events.value.find { it.id == eventId } ?: return
        val response = RSVPResponse(
            userId = userId,
            status = status,
            timestamp = LocalDateTime.now(),
            note = note,
            guestCount = guestCount
        )

        val updatedResponses = event.rsvpResponses.toMutableMap().apply {
            put(userId, response)
        }

        val updatedEvent = event.copy(rsvpResponses = updatedResponses)
        updateEvent(eventId, updatedEvent)
    }

    fun getRSVPStats(eventId: String): Map<RSVPStatus, Int> {
        val event = _events.value.find { it.id == eventId } ?: return emptyMap()
        return event.rsvpResponses.values.groupingBy { it.status }.eachCount()
    }

    fun addAvailabilitySlot(
        userId: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        isAvailable: Boolean = true
    ) {
        val newSlot = AvailabilitySlot(
            userId = userId,
            date = date,
            startTime = startTime,
            endTime = endTime,
            isAvailable = isAvailable
        )

        _availabilitySlots.value = _availabilitySlots.value + newSlot
    }

    fun getEventsForDate(date: LocalDate): List<Event> {
        val groupId = _selectedGroupId.value
        return _events.value.filter { event ->
            event.startDateTime.toLocalDate() == date &&
                    (groupId == null || event.groupId == groupId)
        }.sortedBy { it.startDateTime }
    }

    fun getAvailabilityForDate(date: LocalDate): List<AvailabilitySlot> {
        return _availabilitySlots.value.filter { it.date == date }
            .sortedBy { it.startTime }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
