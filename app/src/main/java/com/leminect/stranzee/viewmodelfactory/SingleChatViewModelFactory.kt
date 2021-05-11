package com.leminect.stranzee.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.leminect.stranzee.viewmodel.SingleChatViewModel

class SingleChatViewModelFactory(private val token: String, private val userId: String, private val strangeeId: String) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SingleChatViewModel::class.java)) {
            return SingleChatViewModel(token, userId, strangeeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}