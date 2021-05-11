package com.leminect.stranzee.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.leminect.stranzee.model.User
import com.leminect.stranzee.viewmodel.FindViewModel

class FindViewModelFactory(private val token: String, private val user: User) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FindViewModel::class.java)) {
            return FindViewModel(token, user) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}