import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.syncplan.ui.chat.ChatListItem
import com.example.syncplan.viewmodel.AuthViewModel
import com.example.syncplan.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel,
    onNavigateToChat: (String) -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val allChats by chatViewModel.chats.collectAsState()
    val userChats = currentUser?.let { user ->
        chatViewModel.getUserChats(user.id)
    } ?: emptyList()

    var showCreateChatDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Wszystkie Czaty") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateChatDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Utwórz nowy czat")
            }
        }
    ) { padding ->
        if (userChats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nie masz jeszcze żadnych czatów.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(userChats) { chatInfo ->
                    ChatListItem(
                        chatInfo = chatInfo,
                        onClick = { onNavigateToChat(chatInfo.id) }
                    )
                }
            }
        }
    }

    if (showCreateChatDialog) {
        CreateDirectChatDialog(
            onDismiss = { showCreateChatDialog = false },
            onCreateChat = { chatName, participants ->
                currentUser?.id?.let { creatorId ->
                    chatViewModel.createDirectChat(chatName, participants, creatorId)
                    showCreateChatDialog = false
                }
            }
        )
    }
}

@Composable
fun CreateDirectChatDialog(
    onDismiss: () -> Unit,
    onCreateChat: (name: String, participants: List<String>) -> Unit
) {
    var chatName by remember { mutableStateOf("") }
    // W pełnej implementacji tutaj powinna być lista użytkowników do wyboru.
    // Na potrzeby demonstracji, uczestnicy będą dodawani ręcznie.
    var participants by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Utwórz nowy czat") },
        text = {
            Column {
                OutlinedTextField(
                    value = chatName,
                    onValueChange = { chatName = it },
                    label = { Text("Nazwa czatu") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Uczestnicy (ID oddzielone przecinkami):", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = participants,
                    onValueChange = { participants = it },
                    label = { Text("user2,user3") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val participantIds = participants.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onCreateChat(chatName, participantIds)
                },
                enabled = chatName.isNotBlank()
            ) {
                Text("Utwórz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
