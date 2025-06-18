package com.example.syncplan.ui.groups

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.syncplan.viewmodel.GroupViewModel
import com.example.syncplan.viewmodel.ExtendedCalendarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    groupViewModel: GroupViewModel,
    calendarViewModel: ExtendedCalendarViewModel
) {
    val groups by groupViewModel.groups.collectAsState()
    val isLoading by groupViewModel.isLoading.collectAsState()
    val errorMessage by groupViewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Twoje grupy") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                groupViewModel.createGroup(
                    name = "Nowa grupa",
                    description = "Opis grupy",
                    createdBy = "user1",
                    creatorName = "Jan Kowalski",
                    creatorEmail = "jan@example.com"
                )
            }) {
                Icon(Icons.Default.Group, contentDescription = "Dodaj grupę")
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                groups.isEmpty() -> {
                    Text("Brak grup", modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        groups.forEach { group ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                onClick = {
                                    groupViewModel.selectGroup(group)
                                }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(group.name, style = MaterialTheme.typography.titleLarge)
                                    Text(group.description, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            errorMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { groupViewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(msg)
                }
            }
        }
    }
}
