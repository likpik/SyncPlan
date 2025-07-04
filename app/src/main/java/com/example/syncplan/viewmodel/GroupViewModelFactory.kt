package com.example.syncplan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GroupViewModelFactory(private val chatViewModel: ChatViewModel) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupViewModel(chatViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
