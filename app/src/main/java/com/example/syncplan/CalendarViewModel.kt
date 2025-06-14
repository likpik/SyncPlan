package com.example.sharedplanner.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

data class Event(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val createdBy: String,
    val attendees: List<String> = emptyList(),
    val isAvailability: Boolean = false // true for availability slots, false for events
)

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
    val duration: Int, // in minutes
    val availableUsers: List<String>,
    val score: Double // how well it fits everyone's schedule
)

class CalendarViewModel : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _availabilitySlots = MutableStateFlow<List<AvailabilitySlot>>(emptyList())
    val availabilitySlots: StateFlow<List<AvailabilitySlot>> = _availabilitySlots.asStateFlow()

    private val _meetingSuggestions = MutableStateFlow<List<MeetingSuggestion>>(emptyList())
    val meetingSuggestions: StateFlow<List<MeetingSuggestion>> = _meetingSuggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun addEvent(
        title: String,
        description: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        createdBy: String,
        attendees: List<String> = emptyList()
    ) {
        val newEvent = Event(
            title = title,
            description = description,
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            createdBy = createdBy,
            attendees = attendees
        )

        _events.value = _events.value + newEvent
        generateMeetingSuggestions(attendees)
    }

    fun addAvailabilitySlot(
        userId: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        isAvailable: Boolean = true
    ) {
        // Remove existing slot for the same user and time
        _availabilitySlots.value = _availabilitySlots.value.filter {
            !(it.userId == userId && it.date == date && it.startTime == startTime && it.endTime == endTime)
        }

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
        return _events.value.filter { event ->
            event.startDateTime.toLocalDate() == date
        }.sortedBy { it.startDateTime }
    }

    fun getAvailabilityForDate(date: LocalDate): List<AvailabilitySlot> {
        return _availabilitySlots.value.filter { it.date == date }
            .sortedBy { it.startTime }
    }

    fun generateMeetingSuggestions(userIds: List<String>, durationMinutes: Int = 60) {
        _isLoading.value = true

        val suggestions = mutableListOf<MeetingSuggestion>()
        val today = LocalDate.now()

        // Look for suggestions in the next 14 days
        for (i in 0..13) {
            val checkDate = today.plusDays(i.toLong())
            val availableSlots = getAvailabilityForDate(checkDate)

            // Find overlapping availability for all users
            val userAvailability = userIds.associateWith { userId ->
                availableSlots.filter { it.userId == userId && it.isAvailable }
            }

            // Check each hour from 9 AM to 5 PM
            for (hour in 9..16) {
                val startTime = LocalTime.of(hour, 0)
                val endTime = startTime.plusMinutes(durationMinutes.toLong())

                val availableUsers = userIds.filter { userId ->
                    val userSlots = userAvailability[userId] ?: emptyList()
                    userSlots.any { slot ->
                        slot.startTime <= startTime && slot.endTime >= endTime
                    }
                }

                if (availableUsers.size >= userIds.size * 0.8) { // At least 80% of users available
                    val score = availableUsers.size.toDouble() / userIds.size
                    suggestions.add(
                        MeetingSuggestion(
                            suggestedDateTime = LocalDateTime.of(checkDate, startTime),
                            duration = durationMinutes,
                            availableUsers = availableUsers,
                            score = score
                        )
                    )
                }
            }
        }

        _meetingSuggestions.value = suggestions.sortedByDescending { it.score }
            .take(10) // Show top 10 suggestions
        _isLoading.value = false
    }

    fun deleteEvent(eventId: String) {
        _events.value = _events.value.filter { it.id != eventId }
    }

    fun updateEvent(eventId: String, updatedEvent: Event) {
        _events.value = _events.value.map { event ->
            if (event.id == eventId) updatedEvent else event
        }
    }

    fun getUserAvailabilityStatus(userId: String, dateTime: LocalDateTime): Boolean {
        val date = dateTime.toLocalDate()
        val time = dateTime.toLocalTime()

        val userSlots = _availabilitySlots.value.filter {
            it.userId == userId && it.date == date
        }

        return userSlots.any { slot ->
            time >= slot.startTime && time <= slot.endTime && slot.isAvailable
        }
    }

    fun getWeekDates(baseDate: LocalDate): List<LocalDate> {
        val startOfWeek = baseDate.minusDays(baseDate.dayOfWeek.value.toLong() - 1)
        return (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    fun formatDateTime(dateTime: LocalDateTime): String {
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    }

    fun formatTime(time: LocalTime): String {
        return time.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}