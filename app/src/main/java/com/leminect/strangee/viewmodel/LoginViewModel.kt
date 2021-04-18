package com.leminect.strangee.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leminect.strangee.model.User
import com.leminect.strangee.network.AuthBackData
import com.leminect.strangee.network.CheckRegistrationInput
import com.leminect.strangee.network.LoginDetail
import com.leminect.strangee.network.StrangeeApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.ConnectException

enum class LoginStatus { LOGGING_IN, LOGIN_ERROR, LOGIN_PASSED, LOGIN_FAILED }

class LoginViewModel: ViewModel() {
    private val _status = MutableLiveData<LoginStatus>()
    val status: LiveData<LoginStatus>
        get() = _status

    private val _loginBackData = MutableLiveData<AuthBackData>()
    val loginBackData: LiveData<AuthBackData>
        get() = _loginBackData

    private val viewModelJob = Job()

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                _status.value = LoginStatus.LOGGING_IN
                val returnedData = StrangeeApi.retrofitService.postLogin(LoginDetail(email, password))

                Log.i("LoginViewModel", returnedData.toString())

                if (returnedData.token.isNotEmpty()) {
                    _loginBackData.value = returnedData
                    _status.value = LoginStatus.LOGIN_PASSED
                } else {
                    _status.value = LoginStatus.LOGIN_FAILED
                }
            } catch (t: ConnectException) {
                Log.i("LoginViewModel", "CONNECT EXCEPTION ::: $t")
                _status.value = LoginStatus.LOGIN_ERROR
            } catch (t: Throwable) {
                Log.i("LoginViewModel", "Login_Error :: $t")
                _status.value = LoginStatus.LOGIN_FAILED
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}