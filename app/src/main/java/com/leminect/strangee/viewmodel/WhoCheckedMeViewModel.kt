package com.leminect.strangee.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leminect.strangee.model.Strangee
import com.leminect.strangee.network.StrangeeApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.ConnectException

enum class WhoCheckedMeStatus { LOADING, ERROR, FAILED, EMPTY, DONE }

class WhoCheckedMeViewModel(token: String, userId: String): ViewModel() {
    private val _status = MutableLiveData<WhoCheckedMeStatus>()
    val status: LiveData<WhoCheckedMeStatus>
        get() = _status

    private val _whoCheckedMeList = MutableLiveData<List<Strangee>>()
    val whoCheckedMeList: LiveData<List<Strangee>>
        get() = _whoCheckedMeList

    private val _navigateToSelectedProfile = MutableLiveData<Strangee>()
    val navigateToSelectedProfile: LiveData<Strangee>
        get() = _navigateToSelectedProfile

    private val viewModelJob = Job()

    init {
        getWhoCheckedMe(token, userId)
    }

    fun displaySavedProfile(strangee: Strangee) {
        _navigateToSelectedProfile.value = strangee
    }

    fun onDisplaySavedProfileComplete() {
        _navigateToSelectedProfile.value = null
    }

    fun getWhoCheckedMe(token: String, userId: String) {
        viewModelScope.launch {
            try {
                Log.i("WhoCheckedMeViewModel", "Begin...")
                _status.value = WhoCheckedMeStatus.LOADING

                val returnedValue = StrangeeApi.retrofitService.getWhoCheckedMe("Bearer ".plus(token), userId)
                Log.i("WhoCheckedMeViewModel", returnedValue.toString())

                if (returnedValue.isNotEmpty()) {
                    _whoCheckedMeList.value = returnedValue
                    _status.value = WhoCheckedMeStatus.DONE
                } else {
                    _status.value = WhoCheckedMeStatus.EMPTY
                }
            } catch (t: ConnectException) {
                Log.i("WhoCheckedMeViewModel", "Connection_Error ::: $t")
                _status.value = WhoCheckedMeStatus.ERROR
            } catch (t: Throwable) {
                Log.i("WhoCheckedMeViewModel", "Failed ::: $t")
                _status.value = WhoCheckedMeStatus.FAILED
            }
        }
    }

    fun removeWhoCheckedMe(token: String, strangeeProfileId: String) {
        for (i in 0 until (_whoCheckedMeList.value?.size ?: 0)) {
            if (_whoCheckedMeList.value?.get(i)?.userId == strangeeProfileId) {
                _whoCheckedMeList.value = _whoCheckedMeList.value!!.filter { it.userId != strangeeProfileId }
                break
            }
        }
        if(_whoCheckedMeList.value?.isEmpty() == true) {
            _status.value = WhoCheckedMeStatus.EMPTY
        }

        viewModelScope.launch {
            try {
                Log.i("FindViewModel", "Remove Begin...")

                val returnedValue = StrangeeApi.retrofitService.removeWhoCheckedMe("Bearer ".plus(token), strangeeProfileId)
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