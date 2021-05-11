package com.leminect.stranzee.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.leminect.stranzee.viewmodel.SavedViewModel

class SavedViewModelFactory(private val token: String, private val userId: String) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedViewModel::class.java)) {
            return SavedViewModel(token, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}