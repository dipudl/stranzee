package com.leminect.strangee.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.leminect.strangee.viewmodel.MainViewModel

class MainViewModelFactory(
    private val token: String,
    private val refreshToken: String,
    private val userId: String,
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(token, refreshToken, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}