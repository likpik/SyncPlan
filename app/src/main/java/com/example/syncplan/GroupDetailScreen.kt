package com.example.syncplan

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.syncplan.viewmodel.ChatViewModel
import com.example.syncplan.viewmodel.ExtendedCalendarViewModel
import com.example.syncplan.viewmodel.GroupViewModel
import com.example.syncplan.viewmodel.Group
import com.example.syncplan.viewmodel.GroupMember
import com.example.syncplan.viewmodel.MemberRole
import com.example.syncplan.viewmodel.User
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: Group,
    groupViewModel: GroupViewModel,
    calendarViewModel: ExtendedCalendarViewModel,
    chatViewModel: ChatViewModel,
    userSession: User,
    onNavigateBack: () -> Unit,
    onNavigateToCalendar: (String) -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var showSmsInviteDialog by remember { mutableStateOf(false) }
    var selectedMemberForRole by remember { mutableStateOf<GroupMember?>(null) }
    var memberToRemove by remember { mutableStateOf<GroupMember?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val smsService = remember { SmsService(context) }
    val currentUserId = userSession.id
    val currentUserRole = group.members.find { it.userId == currentUserId }?.role
    val canManageMembers = currentUserRole == MemberRole.Admin
    val eventsCount by calendarViewModel.getEventsCountForGroup(group.id).collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission launcher for SMS
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showSmsInviteDialog = true
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Uprawnienie do wysyłania SMS jest wymagane",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    LaunchedEffect(eventsCount) {
        Log.d("GroupDetail", "GroupId=${group.id}, eventsCount=$eventsCount")
    }

    fun showError(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    fun showSuccess(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    fun requestSmsPermissionAndShowDialog() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED -> {
                showSmsInviteDialog = true
            }
            else -> {
                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(group.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                },
                actions = {
                    if (canManageMembers) {
                        IconButton(onClick = { showAddMemberDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Dodaj członka")
                        }
                        IconButton(onClick = { requestSmsPermissionAndShowDialog() }) {
                            Icon(Icons.Default.Sms, contentDescription = "Zaproś SMS")
                        }
                        IconButton(onClick = { showEditGroupDialog = true }) {
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
                GroupStats(group = group, eventsCount = eventsCount)
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
                        Row {
                            TextButton(onClick = { showAddMemberDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dodaj")
                            }
                            TextButton(onClick = { requestSmsPermissionAndShowDialog() }) {
                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SMS")
                            }
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
                    onRemoveMember = { memberToRemove = member }
                )
            }

            // Przycisk akcji
            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onNavigateToCalendar(group.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zobacz kalendarz grupy")
                }
            }
            if (group.chatId != null) {
                item {
                    Button(
                        onClick = { onNavigateToChat(group.chatId) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Przejdź do czatu grupowego")
                    }
                }
            }
        }
    }

    // Loading overlay
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // Dialog dodawania członka
    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onAddMember = { name, email ->
                isLoading = true
                scope.launch {
                    try {
                        val newMember = GroupMember(
                            userId = "temp_${System.currentTimeMillis()}",
                            name = name,
                            email = email,
                            role = MemberRole.Member
                        )
                        groupViewModel.addMemberToGroup(group.id, newMember, chatViewModel)
                        showAddMemberDialog = false
                        showSuccess("Członek został dodany do grupy")
                    } catch (e: Exception) {
                        showError("Błąd podczas dodawania członka: ${e.message}")
                    } finally {
                        isLoading = false
                    }
                }
            }
        )
    }

    // Dialog zaproszenia SMS
    if (showSmsInviteDialog) {
        SmsInviteDialog(
            group = group,
            inviterName = userSession.name,
            onDismiss = { showSmsInviteDialog = false },
            onSendInvite = { phoneNumber ->
                isLoading = true
                scope.launch {
                    try {
                        val invitationCode = smsService.generateInvitationCode()
                        val result = smsService.sendGroupInvitation(
                            phoneNumber = phoneNumber,
                            groupName = group.name,
                            inviterName = userSession.name,
                            invitationCode = invitationCode
                        )

                        if (result.isSuccess) {
                            showSmsInviteDialog = false
                            showSuccess("Zaproszenie SMS zostało wysłane")
                        } else {
                            showError("Błąd podczas wysyłania SMS: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        showError("Błąd podczas wysyłania zaproszenia: ${e.message}")
                    } finally {
                        isLoading = false
                    }
                }
            }
        )
    }

    // Dialog edycji grupy
    if (showEditGroupDialog) {
        EditGroupDialog(
            group = group,
            onDismiss = { showEditGroupDialog = false },
            onSave = { name, description, color ->
                isLoading = true
                scope.launch {
                    try {
                        groupViewModel.updateGroup(group.id, name, description, color)
                        showEditGroupDialog = false
                        showSuccess("Grupa została zaktualizowana")
                    } catch (e: Exception) {
                        showError("Błąd podczas edycji grupy: ${e.message}")
                    } finally {
                        isLoading = false
                    }
                }
            }
        )
    }

    // Dialog zmiany roli
    selectedMemberForRole?.let { member ->
        RoleChangeDialog(
            member = member,
            onDismiss = { selectedMemberForRole = null },
            onRoleChange = { newRole ->
                isLoading = true
                scope.launch {
                    try {
                        groupViewModel.updateMemberRole(group.id, member.userId, newRole)
                        selectedMemberForRole = null
                        showSuccess("Rola została zmieniona")
                    } catch (e: Exception) {
                        showError("Błąd podczas zmiany roli: ${e.message}")
                    } finally {
                        isLoading = false
                    }
                }
            }
        )
    }

    // Dialog potwierdzenia usunięcia członka
    memberToRemove?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Usuń członka") },
            text = { Text("Czy na pewno chcesz usunąć ${member.name} z grupy?") },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                groupViewModel.removeMemberFromGroup(group.id, member.userId, chatViewModel)
                                memberToRemove = null
                                showSuccess("Członek został usunięty z grupy")
                            } catch (e: Exception) {
                                showError("Błąd podczas usuwania członka: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text("Anuluj")
                }
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
                        isLoading = true
                        scope.launch {
                            try {
                                groupViewModel.removeMemberFromGroup(group.id, userSession.id, chatViewModel)
                                showDeleteConfirmDialog = false
                                onNavigateBack()
                            } catch (e: Exception) {
                                showError("Błąd podczas opuszczania grupy: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
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
fun SmsInviteDialog(
    group: Group,
    inviterName: String,
    onDismiss: () -> Unit,
    onSendInvite: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val smsService = remember { SmsService(context) }

    fun validatePhoneNumber(phone: String): String? {
        val cleanNumber = phone.replace("\\s+".toRegex(), "")
        return when {
            cleanNumber.isBlank() -> "Numer telefonu jest wymagany"
            !cleanNumber.matches(Regex("^\\+?[1-9]\\d{1,14}$")) -> "Nieprawidłowy format numeru telefonu"
            else -> null
        }
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zaproś przez SMS",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Wyślij zaproszenie do grupy \"${group.name}\" przez SMS",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        phoneError = validatePhoneNumber(it)
                    },
                    label = { Text("Numer telefonu") },
                    placeholder = { Text("+48 123 456 789") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    supportingText = {
                        phoneError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Podgląd wiadomości SMS:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🎉 Zaproszenie do grupy!\n\n$inviterName zaprosił Cię do grupy \"${group.name}\" w aplikacji SyncPlan.\n\nKod zaproszenia: [KOD]\n\nPobierz aplikację i wpisz kod zaproszenia aby dołączyć do grupy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status check
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (smsService.canSendSms()) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (smsService.canSendSms()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (smsService.canSendSms()) "Gotowe do wysłania SMS" else "Brak uprawnień SMS",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (smsService.canSendSms()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

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
                            val validation = validatePhoneNumber(phoneNumber)
                            if (validation == null) {
                                val formattedNumber = smsService.formatPhoneNumber(phoneNumber)
                                onSendInvite(formattedNumber)
                            } else {
                                phoneError = validation
                            }
                        },
                        enabled = phoneNumber.isNotBlank() && phoneError == null && smsService.canSendSms()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wyślij SMS")
                    }
                }
            }
        }
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
fun GroupStats(group: Group, eventsCount: Int) {
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
            value = eventsCount.toString(),
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
    var emailError by remember { mutableStateOf<String?>(null) }

    fun validateEmail(email: String): String? {
        return if (email.isBlank()) {
            "Email jest wymagany"
        } else if (!EmailValidator.isValid(email)) {
            "Nieprawidłowy format email"
        } else {
            null
        }
    }

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
                    singleLine = true,
                    isError = memberName.isBlank()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = memberEmail,
                    onValueChange = {
                        memberEmail = it
                        emailError = validateEmail(it)
                    },
                    label = { Text("Adres email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null,
                    supportingText = {
                        emailError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
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
                            val emailValidation = validateEmail(memberEmail)
                            if (memberName.isNotBlank() && emailValidation == null) {
                                onAddMember(memberName, memberEmail)
                            } else {
                                emailError = emailValidation
                            }
                        },
                        enabled = memberName.isNotBlank() && memberEmail.isNotBlank() && emailError == null
                    ) {
                        Text("Dodaj")
                    }
                }
            }
        }
    }
}

@Composable
fun EditGroupDialog(
    group: Group,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, color: String) -> Unit
) {
    var groupName by remember { mutableStateOf(group.name) }
    var groupDescription by remember { mutableStateOf(group.description) }
    var groupColor by remember { mutableStateOf(group.color) }

    val predefinedColors = listOf(
        "#2196F3", "#4CAF50", "#FF9800", "#9C27B0",
        "#F44336", "#00BCD4", "#FF5722", "#795548"
    )

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
                    text = "Edytuj grupę",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Nazwa grupy") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = groupDescription,
                    onValueChange = { groupDescription = it },
                    label = { Text("Opis grupy") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Kolor grupy",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    predefinedColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .then(
                                    if (groupColor == color) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.outline,
                                            CircleShape
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { groupColor = color },
                                modifier = Modifier.size(32.dp)
                            ) {
                                if (groupColor == color) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (groupName.isNotBlank()) {
                                onSave(groupName, groupDescription, groupColor)
                            }
                        },
                        enabled = groupName.isNotBlank()
                    ) {
                        Text("Zapisz")
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