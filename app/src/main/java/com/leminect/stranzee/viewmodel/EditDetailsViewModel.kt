package com.leminect.stranzee.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leminect.stranzee.model.User
import com.leminect.stranzee.network.StrangeeApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.ConnectException

enum class EditDetailsStatus { UPDATING, UPDATE_ERROR, UPDATE_FAILED, UPDATE_DONE }

class EditDetailsViewModel: ViewModel() {

    private val _status = MutableLiveData<EditDetailsStatus>()
    val status: LiveData<EditDetailsStatus>
        get() = _status

    private val viewModelJob = Job()

    fun editDetails(token: String, user: User) {
        viewModelScope.launch {
            try {
                _status.value = EditDetailsStatus.UPDATING
                val returnedData = StrangeeApi.retrofitService.postEditDetails("Bearer ".plus(token), user)

                Log.i("EditDetailsViewModel", returnedData.toString())

                if (returnedData) {
                    _status.value = EditDetailsStatus.UPDATE_DONE
                } else {
                    _status.value = EditDetailsStatus.UPDATE_FAILED
                }
            } catch (t: ConnectException) {
                Log.i("EditDetailsViewModel", "CONNECT EXCEPTION ::: $t")
                _status.value = EditDetailsStatus.UPDATE_ERROR
            } catch (t: Throwable) {
                Log.i("EditDetailsViewModel", "Login_Error :: $t")
                _status.value = EditDetailsStatus.UPDATE_FAILED
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}