package com.leminect.strangee.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.leminect.strangee.viewmodel.StrangeeProfileViewModel

class StrangeeProfileViewModelFactory(private val token: String, private val userId: String, private val strangeeId: String) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StrangeeProfileViewModel::class.java)) {
            return StrangeeProfileViewModel(token, userId, strangeeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}