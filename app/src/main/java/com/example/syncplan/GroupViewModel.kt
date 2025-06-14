package com.example.sharedplanner.viewmodel

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
    val color: String = "#2196F3" // Material Blue
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
        // Sample data for demonstration
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

    fun inviteToGroup(
        groupId: String,
        groupName: String,
        invitedBy: String,
        invitedByName: String,
        invitedEmail: String
    ) {
        val invitation = GroupInvitation(
            groupId = groupId,
            groupName = groupName,
            invitedBy = invitedBy,
            invitedByName = invitedByName,
            invitedEmail = invitedEmail
        )

        _invitations.value = _invitations.value + invitation
    }

    fun respondToInvitation(invitationId: String, accept: Boolean, userInfo: GroupMember? = null) {
        val invitation = _invitations.value.find { it.id == invitationId }
        if (invitation != null) {
            val newStatus = if (accept) InvitationStatus.Accepted else InvitationStatus.Declined

            _invitations.value = _invitations.value.map { inv ->
                if (inv.id == invitationId) {
                    inv.copy(status = newStatus)
                } else {
                    inv
                }
            }

            if (accept && userInfo != null) {
                addMemberToGroup(invitation.groupId, userInfo)
            }
        }
    }

    fun deleteGroup(groupId: String) {
        _groups.value = _groups.value.filter { it.id != groupId }
        if (_selectedGroup.value?.id == groupId) {
            _selectedGroup.value = null
        }
    }

    fun updateGroup(groupId: String, name: String, description: String, color: String) {
        _groups.value = _groups.value.map { group ->
            if (group.id == groupId) {
                group.copy(name = name, description = description, color = color)
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

    fun isUserAdmin(groupId: String, userId: String): Boolean {
        val group = getGroupById(groupId)
        return group?.members?.find { it.userId == userId }?.role == MemberRole.Admin
    }

    fun getPendingInvitations(): List<GroupInvitation> {
        return _invitations.value.filter { it.status == InvitationStatus.Pending }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        val DEFAULT_GROUP_COLORS = listOf(
            "#2196F3", // Blue
            "#4CAF50", // Green
            "#FF9800", // Orange
            "#9C27B0", // Purple
            "#F44336", // Red
            "#607D8B", // Blue Grey
            "#795548", // Brown
            "#E91E63"  // Pink
        )
    }
}