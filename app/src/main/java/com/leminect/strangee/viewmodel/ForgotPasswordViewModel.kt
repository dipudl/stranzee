package com.leminect.strangee.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.leminect.strangee.network.StrangeeApi
import kotlinx.coroutines.launch
import java.net.ConnectException

enum class ForgotPasswordStatus{ SENDING, SENDING_FAILED, SENDING_ERROR, EMAIL_NOT_FOUND, SENT }

class ForgotPasswordViewModel: ViewModel() {
    private val _status = MutableLiveData<ForgotPasswordStatus>()
    val status: LiveData<ForgotPasswordStatus>
        get() = _status

    fun sendResetLink(email: String) {
        viewModelScope.launch {
            try {
                _status.value = ForgotPasswordStatus.SENDING
                val returnedData = StrangeeApi.retrofitService.postForgotPassword(email)
                Log.i("ForgotPasswordViewModel", returnedData.toString())

                if (returnedData.emailSent) {
                    _status.value = ForgotPasswordStatus.SENT
                } else {
                    if(returnedData.userFound) {
                        _status.value = ForgotPasswordStatus.SENDING_FAILED
                    } else {
                        _status.value = ForgotPasswordStatus.EMAIL_NOT_FOUND
                    }
                }
            } catch (t: ConnectException) {
                Log.i("ForgotPasswordViewModel", "CONNECT EXCEPTION ::: $t")
                _status.value = ForgotPasswordStatus.SENDING_ERROR
            } catch (t: Throwable) {
                Log.i("ForgotPasswordViewModel", "Login_Error :: $t")
                _status.value = ForgotPasswordStatus.SENDING_FAILED
            }
        }
    }

    fun onStatusChecked() {
        _status.value = null
    }

    override fun onCleared() {
        super.onCleared()
    }
}