package com.example.syncplan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*



data class Group(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val createdBy: String,
    val members: List<GroupMember>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val color: String = "#2196F3",
    val chatId: String? = null
)

data class GroupMember(
    val userId: String,
    val name: String,
    val email: String,
    val role: MemberRole = MemberRole.Member,
    val joinedAt: Long = System.currentTimeMillis()
)

enum class MemberRole {
    Admin,
    Member
}

data class GroupInvitation(
    val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val groupName: String,
    val invitedBy: String,
    val invitedByName: String,
    val invitedEmail: String,
    val status: InvitationStatus = InvitationStatus.Pending,
    val createdAt: Long = System.currentTimeMillis()
)

enum class InvitationStatus {
    Pending,
    Accepted,
    Declined
}

data class GroupActivityLog(
    val id: String,
    val groupId: String,
    val userId: String,
    val userName: String,
    val action: ActivityAction,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ActivityAction {
    MEMBER_ADDED,
    MEMBER_REMOVED,
    ROLE_CHANGED,
    GROUP_CREATED,
    GROUP_UPDATED,
    EVENT_CREATED,
    EVENT_UPDATED,
    EVENT_DELETED
}

data class GroupEvent(
    val id: String,
    val groupId: String,
    val title: String,
    val description: String,
    val startTime: Long,
    val endTime: Long,
    val location: String? = null,
    val createdBy: String,
    val attendees: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

class GroupViewModel(private val chatViewModel: ChatViewModel) : ViewModel() {
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup.asStateFlow()

    private val _invitations = MutableStateFlow<List<GroupInvitation>>(emptyList())
    val invitations: StateFlow<List<GroupInvitation>> = _invitations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _events = MutableStateFlow<List<GroupEvent>>(emptyList())
    val events: StateFlow<List<GroupEvent>> = _events.asStateFlow()

    private val _activityLogs = MutableStateFlow<List<GroupActivityLog>>(emptyList())

    init {
        loadSampleData()
    }

    private fun loadSampleData() {
        val sampleGroups = listOf(
            Group(
                name = "Znajomi ze studiów",
                description = "Grupa do organizowania spotkań koleżeńskich",
                createdBy = "current_user",
                members = listOf(
                    GroupMember("current_user", "Ja", "current@example.com", MemberRole.Admin),
                    GroupMember("user1", "Jan Kowalski", "jan@example.com"),
                    GroupMember("user2", "Anna Nowak", "anna@example.com"),
                    GroupMember("user3", "Piotr Wiśniewski", "piotr@example.com")
                ),
                color = "#4CAF50"
            ),
            Group(
                name = "Praca - Zespół",
                description = "Grupa robocza do planowania projektów",
                createdBy = "current_user",
                members = listOf(
                    GroupMember("current_user", "Ja", "current@example.com"),
                    GroupMember("user1", "Jan Kowalski", "jan@example.com", MemberRole.Admin),
                    GroupMember("user4", "Katarzyna Zielińska", "kasia@company.com"),
                    GroupMember("user5", "Tomasz Kowalczyk", "tomasz@company.com")
                ),
                color = "#FF9800"
            )
        )

        val finalGroups = sampleGroups.map { group ->
            val chatId = chatViewModel.createGroupChat(group)
            group.copy(chatId = chatId)
        }

        _groups.value = finalGroups
    }
    fun createGroup(
        name: String,
        description: String,
        createdBy: String,
        creatorName: String,
        creatorEmail: String,
        initialMembers: List<GroupMember>,
        chatViewModel: ChatViewModel,
        color: String = "#2196F3"
    ) {
        val creator = GroupMember(
            userId = createdBy,
            name = creatorName,
            email = creatorEmail,
            role = MemberRole.Admin
        )
        val allMembers = initialMembers + listOf(creator)

        val initialGroup = Group(
            name = name,
            description = description,
            createdBy = createdBy,
            members = allMembers,
            color = color
        )

        val newChatId = chatViewModel.createGroupChat(initialGroup)

        val finalGroup = initialGroup.copy(chatId = newChatId)

        _groups.value = _groups.value + finalGroup

    }

    fun selectGroup(group: Group) {
        _selectedGroup.value = group
    }

    fun addMemberToGroup(groupId: String, member: GroupMember, chatViewModel: ChatViewModel) {
        _groups.value = _groups.value.map { group ->
            if (group.id == groupId) {
                val updatedGroup = group.copy(members = group.members + member)
                updatedGroup.chatId?.let { chatId ->
                    chatViewModel.addParticipantToChat(chatId, member.userId)
                }
                updatedGroup
            } else {
                group
            }
        }
    }

    fun removeMemberFromGroup(groupId: String, userId: String, chatViewModel: ChatViewModel) {
        _groups.value = _groups.value.map { group ->
            if (group.id == groupId) {
                // Usuń użytkownika z powiązanego czatu
                group.chatId?.let { chatId ->
                    chatViewModel.removeParticipantFromChat(chatId, userId)
                }
                // Zwróć grupę z usuniętym członkiem
                group.copy(members = group.members.filter { it.userId != userId })
            } else {
                group
            }
        }
    }

    fun updateMemberRole(groupId: String, userId: String, newRole: MemberRole) {
        _groups.value = _groups.value.map { group ->
            if (group.id == groupId) {
                val updatedMembers = group.members.map { member ->
                    if (member.userId == userId) {
                        member.copy(role = newRole)
                    } else {
                        member
                    }
                }
                group.copy(members = updatedMembers)
            } else {
                group
            }
        }
    }

    fun getGroupById(groupId: String): Group? {
        return _groups.value.find { it.id == groupId }
    }

    fun getUserGroups(userId: String): List<Group> {
        return _groups.value.filter { group ->
            group.members.any { it.userId == userId }
        }
    }



    fun updateCurrentUserInSampleGroups(userId: String, userName: String, userEmail: String) {
        val updatedGroups = _groups.value.map { group ->
            val updatedMembers = group.members.map { member ->
                if (member.userId == "current_user") {
                    member.copy(
                        userId = userId,
                        name = userName,
                        email = userEmail
                    )
                } else {
                    member
                }
            }

            val updatedGroup = group.copy(
                members = updatedMembers,
                createdBy = if (group.createdBy == "current_user") userId else group.createdBy
            )

            // Zaktualizuj również czat grupowy
            updatedGroup.chatId?.let { chatId ->
                chatViewModel.updateChatParticipants(chatId, updatedMembers.map { it.userId })
            }

            updatedGroup
        }

        _groups.value = updatedGroups
    }


    // Update group details
    suspend fun updateGroup(groupId: String, name: String, description: String, color: String) {
        try {
            _isLoading.value = true
            _errorMessage.value = null

            // Simulate API call delay
            kotlinx.coroutines.delay(1000)

            val currentGroups = _groups.value.toMutableList()
            val groupIndex = currentGroups.indexOfFirst { it.id == groupId }

            if (groupIndex != -1) {
                val group = currentGroups[groupIndex]
                val updatedGroup = group.copy(
                    name = name,
                    description = description,
                    color = color,
                    updatedAt = System.currentTimeMillis()
                )

                currentGroups[groupIndex] = updatedGroup
                _groups.value = currentGroups

                // Log activity
                logActivity(
                    groupId = groupId,
                    userId = "system", // TODO: Get current user
                    userName = "System",
                    action = ActivityAction.GROUP_UPDATED,
                    details = "Grupa została zaktualizowana"
                )
            } else {
                throw Exception("Grupa nie została znaleziona")
            }
        } catch (e: Exception) {
            _errorMessage.value = e.message
            throw e
        } finally {
            _isLoading.value = false
        }
    }

    // Get events count for a specific group
    fun getEventsCount(groupId: String): Flow<Int> {
        return _events.map { eventsList ->
            eventsList.count { event -> event.groupId == groupId }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Private helper function to log activities
    private fun logActivity(
        groupId: String,
        userId: String,
        userName: String,
        action: ActivityAction,
        details: String
    ) {
        viewModelScope.launch {
            val currentLogs = _activityLogs.value.toMutableList()
            val newLog = GroupActivityLog(
                id = "log_${System.currentTimeMillis()}",
                groupId = groupId,
                userId = userId,
                userName = userName,
                action = action,
                details = details
            )
            currentLogs.add(newLog)
            _activityLogs.value = currentLogs
        }
    }

    companion object {
        val DEFAULT_GROUP_COLORS = listOf(
            "#2196F3", "#4CAF50", "#FF9800", "#9C27B0",
            "#F44336", "#607D8B", "#795548", "#E91E63"
        )
    }
}
