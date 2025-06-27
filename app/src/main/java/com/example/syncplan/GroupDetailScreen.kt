package com.example.syncplan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.syncplan.viewmodel.GroupViewModel
import com.example.syncplan.viewmodel.Group
import com.example.syncplan.viewmodel.GroupMember
import com.example.syncplan.viewmodel.MemberRole
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: Group,
    groupViewModel: GroupViewModel,
    onNavigateBack: () -> Unit
) {
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedMemberForRole by remember { mutableStateOf<GroupMember?>(null) }

    val currentUserId = "user1" // TODO: Get from current user session
    val currentUserRole = group.members.find { it.userId == currentUserId }?.role
    val canManageMembers = currentUserRole == MemberRole.Admin

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wróć")
                    }
                },
                actions = {
                    if (canManageMembers) {
                        IconButton(onClick = { showAddMemberDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Dodaj członka")
                        }
                        IconButton(onClick = { /* TODO: Edit group */ }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edytuj grupę")
                        }
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Opuść grupę")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nagłówek grupy
            item {
                GroupHeader(group = group)
            }

            // Statystyki grupy
            item {
                GroupStats(group = group)
            }

            // Sekcja członków
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Członkowie (${group.members.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (canManageMembers) {
                        TextButton(onClick = { showAddMemberDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dodaj")
                        }
                    }
                }
            }

            // Lista członków
            items(group.members) { member ->
                MemberCard(
                    member = member,
                    currentUserId = currentUserId,
                    canManageMembers = canManageMembers,
                    onRoleChange = { selectedMemberForRole = member },
                    onRemoveMember = {
                        groupViewModel.removeMemberFromGroup(group.id, member.userId)
                    }
                )
            }

            // Przycisk akcji
            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { /* TODO: Navigate to group calendar */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zobacz kalendarz grupy")
                }
            }
        }
    }

    // Dialog dodawania członka
    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onAddMember = { name, email ->
                val newMember = GroupMember(
                    userId = "temp_${System.currentTimeMillis()}",
                    name = name,
                    email = email,
                    role = MemberRole.Member
                )
                groupViewModel.addMemberToGroup(group.id, newMember)
                showAddMemberDialog = false
            }
        )
    }

    // Dialog zmiany roli
    selectedMemberForRole?.let { member ->
        RoleChangeDialog(
            member = member,
            onDismiss = { selectedMemberForRole = null },
            onRoleChange = { newRole ->
                groupViewModel.updateMemberRole(group.id, member.userId, newRole)
                selectedMemberForRole = null
            }
        )
    }

    // Dialog potwierdzenia opuszczenia grupy
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Opuść grupę") },
            text = { Text("Czy na pewno chcesz opuścić grupę \"${group.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        groupViewModel.removeMemberFromGroup(group.id, currentUserId)
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Opuść")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
fun GroupHeader(group: Group) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(group.color)).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(group.color))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (group.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = group.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Utworzona ${formatDate(group.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GroupStats(group: Group) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            icon = Icons.Default.Person,
            value = group.members.size.toString(),
            label = "Członków",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.AdminPanelSettings,
            value = group.members.count { it.role == MemberRole.Admin }.toString(),
            label = "Adminów",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.CalendarToday,
            value = "0", // TODO: Get events count
            label = "Wydarzeń",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MemberCard(
    member: GroupMember,
    currentUserId: String,
    canManageMembers: Boolean,
    onRoleChange: () -> Unit,
    onRemoveMember: () -> Unit
) {
    val isCurrentUser = member.userId == currentUserId

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Member info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(Ty)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = member.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {
                            if (canManageMembers && !isCurrentUser) {
                                onRoleChange()
                            }
                        },
                        label = {
                            Text(
                                text = when (member.role) {
                                    MemberRole.Admin -> "Administrator"
                                    MemberRole.Member -> "Członek"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (member.role == MemberRole.Admin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (member.role == MemberRole.Admin)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dołączył ${formatDate(member.joinedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            if (canManageMembers && !isCurrentUser) {
                IconButton(onClick = onRemoveMember) {
                    Icon(
                        Icons.Default.PersonRemove,
                        contentDescription = "Usuń członka",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onAddMember: (name: String, email: String) -> Unit
) {
    var memberName by remember { mutableStateOf("") }
    var memberEmail by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Dodaj członka",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = memberName,
                    onValueChange = { memberName = it },
                    label = { Text("Imię i nazwisko") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = memberEmail,
                    onValueChange = { memberEmail = it },
                    label = { Text("Adres email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (memberName.isNotBlank() && memberEmail.isNotBlank()) {
                                onAddMember(memberName, memberEmail)
                            }
                        },
                        enabled = memberName.isNotBlank() && memberEmail.isNotBlank()
                    ) {
                        Text("Dodaj")
                    }
                }
            }
        }
    }
}

@Composable
fun RoleChangeDialog(
    member: GroupMember,
    onDismiss: () -> Unit,
    onRoleChange: (MemberRole) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Zmień rolę",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Wybierz nową rolę dla ${member.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    MemberRole.values().forEach { role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = member.role == role,
                                onClick = { onRoleChange(role) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = when (role) {
                                        MemberRole.Admin -> "Administrator"
                                        MemberRole.Member -> "Członek"
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = when (role) {
                                        MemberRole.Admin -> "Może zarządzać grupą i członkami"
                                        MemberRole.Member -> "Może uczestniczyć w wydarzeniach"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}