package com.leminect.stranzee.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leminect.stranzee.model.Strangee
import com.leminect.stranzee.network.StrangeeApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.ConnectException

enum class SavedStatus { LOADING, ERROR, FAILED, EMPTY, DONE }

class SavedViewModel(token: String, userId: String): ViewModel() {
    private val _status = MutableLiveData<SavedStatus>()
    val status: LiveData<SavedStatus>
        get() = _status

    private val _savedList = MutableLiveData<List<Strangee>>()
    val savedList: LiveData<List<Strangee>>
        get() = _savedList

    private val _navigateToSelectedProfile = MutableLiveData<Strangee>()
    val navigateToSelectedProfile: LiveData<Strangee>
        get() = _navigateToSelectedProfile

    private val viewModelJob = Job()

    init {
        getSavedList(token, userId)
    }

    fun displaySavedProfile(strangee: Strangee) {
        _navigateToSelectedProfile.value = strangee
    }

    fun onDisplaySavedProfileComplete() {
        _navigateToSelectedProfile.value = null
    }

    fun getSavedList(token: String, userId: String) {
        viewModelScope.launch {
            try {
                Log.i("FindViewModel", "Begin...")
                _status.value = SavedStatus.LOADING

                val returnedValue = StrangeeApi.retrofitService.getSaved("Bearer ".plus(token), userId)
                Log.i("SavedViewModel", returnedValue.toString())

                if (returnedValue.isNotEmpty()) {
                    _savedList.value = returnedValue
                    _status.value = SavedStatus.DONE
                } else {
                    _status.value = SavedStatus.EMPTY
                }
            } catch (t: ConnectException) {
                Log.i("SavedViewModel", "Connection_Error ::: $t")
                _status.value = SavedStatus.ERROR
            } catch (t: Throwable) {
                Log.i("SavedViewModel", "Failed ::: $t")
                _status.value = SavedStatus.FAILED
            }
        }
    }

    fun removeSavedProfile(token: String, savedProfileId: String) {
        for (i in 0 until (_savedList.value?.size ?: 0)) {
            if (_savedList.value?.get(i)?.userId == savedProfileId) {
                _savedList.value = _savedList.value!!.filter { it.userId != savedProfileId }
                break
            }
        }
        if(_savedList.value?.isEmpty() == true) {
            _status.value = SavedStatus.EMPTY
        }

        viewModelScope.launch {
            try {
                Log.i("FindViewModel", "Remove Begin...")

                val returnedValue = StrangeeApi.retrofitService.removeSavedProfile("Bearer ".plus(token), savedProfileId)
                Log.i("SavedViewModel", returnedValue.toString())

            } catch (t: Throwable) {
                Log.i("SavedViewModel", "Remove Failed ::: $t")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}