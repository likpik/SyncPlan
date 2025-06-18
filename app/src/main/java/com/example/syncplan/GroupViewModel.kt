package com.example.syncplan.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

data class Group(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val createdBy: String,
    val members: List<GroupMember>,
    val createdAt: Long = System.currentTimeMillis(),
    val color: String = "#2196F3"
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

class GroupViewModel : ViewModel() {
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

    init {
        loadSampleData()
    }

    private fun loadSampleData() {
        val sampleGroups = listOf(
            Group(
                name = "Znajomi ze studiów",
                description = "Grupa do organizowania spotkań koleżeńskich",
                createdBy = "user1",
                members = listOf(
                    GroupMember("user1", "Jan Kowalski", "jan@example.com", MemberRole.Admin),
                    GroupMember("user2", "Anna Nowak", "anna@example.com"),
                    GroupMember("user3", "Piotr Wiśniewski", "piotr@example.com")
                ),
                color = "#4CAF50"
            ),
            Group(
                name = "Praca - Zespół",
                description = "Grupa robocza do planowania projektów",
                createdBy = "user1",
                members = listOf(
                    GroupMember("user1", "Jan Kowalski", "jan@example.com", MemberRole.Admin),
                    GroupMember("user4", "Katarzyna Zielińska", "kasia@company.com"),
                    GroupMember("user5", "Tomasz Kowalczyk", "tomasz@company.com")
                ),
                color = "#FF9800"
            )
        )

        _groups.value = sampleGroups
    }

    fun createGroup(
        name: String,
        description: String,
        createdBy: String,
        creatorName: String,
        creatorEmail: String,
        color: String = "#2196F3"
    ) {
        val creator = GroupMember(
            userId = createdBy,
            name = creatorName,
            email = creatorEmail,
            role = MemberRole.Admin
        )

        val newGroup = Group(
            name = name,
            description = description,
            createdBy = createdBy,
            members = listOf(creator),
            color = color
        )

        _groups.value = _groups.value + newGroup
    }

    fun selectGroup(group: Group) {
        _selectedGroup.value = group
    }

    fun addMemberToGroup(groupId: String, member: GroupMember) {
        _groups.value = _groups.value.map { group ->
            if (group.id == groupId) {
                group.copy(members = group.members + member)
            } else {
                group
            }
        }
    }

    fun removeMemberFromGroup(groupId: String, userId: String) {
        _groups.value = _groups.value.map { group ->
            if (group.id == groupId) {
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

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        val DEFAULT_GROUP_COLORS = listOf(
            "#2196F3", "#4CAF50", "#FF9800", "#9C27B0",
            "#F44336", "#607D8B", "#795548", "#E91E63"
        )
    }
}
