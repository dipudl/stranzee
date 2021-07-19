package com.leminect.stranzee.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.leminect.stranzee.network.AuthBackData
import com.leminect.stranzee.network.LoginDetail
import com.leminect.stranzee.network.StrangeeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.UnknownHostException

enum class LoginStatus { LOGGING_IN, LOGIN_ERROR, LOGIN_PASSED, LOGIN_FAILED }

class LoginViewModel: ViewModel() {
    private val _status = MutableLiveData<LoginStatus>()
    val status: LiveData<LoginStatus>
        get() = _status

    private val _loginBackData = MutableLiveData<AuthBackData>()
    val loginBackData: LiveData<AuthBackData>
        get() = _loginBackData

    private val viewModelJob = Job()

    private var initialFcmToken: String? = null

    init {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get new FCM registration token
                initialFcmToken = task.result
            } else {
                Log.i("SignUpViewModel",
                    "Fetching FCM registration token failed at start: ${task.exception}")
            }
        }
    }

    fun startLoginProcess(email: String, password: String) {
        if(initialFcmToken != null) {
            loginUser(email, password, initialFcmToken!!)
        } else {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get new FCM registration token
                    val fcmToken = task.result


                    loginUser(email, password, fcmToken)
                } else {
                    Log.i("LoginViewModel",
                        "Fetching FCM registration token failed: ${task.exception}")
                    _status.value = LoginStatus.LOGIN_ERROR
                }
            }
        }
    }

    private fun loginUser(email: String, password: String, fcmToken: String) {
        viewModelScope.launch {
            try {
                _status.value = LoginStatus.LOGGING_IN
                val returnedData = StrangeeApi.retrofitService.postLogin(LoginDetail(email, password, fcmToken))

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
            } catch (t: UnknownHostException) {
                Log.i("LoginViewModel", "HOST EXCEPTION ::: $t")
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