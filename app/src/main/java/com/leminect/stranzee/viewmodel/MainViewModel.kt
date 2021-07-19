package com.leminect.stranzee.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leminect.stranzee.network.StrangeeApi
import com.leminect.stranzee.network.TokenCheck
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.UnknownHostException

enum class LOGOUT { LOGGING_OUT, LOGOUT_ERROR, LOGOUT_FAILED, LOGOUT_SUCCESSFUL }

class MainViewModel(
    private val token: String,
    private val refreshToken: String,
    private val userId: String,
) : ViewModel() {

    private val _tokenCheckData = MutableLiveData<TokenCheck>()
    val tokenCheckData: LiveData<TokenCheck>
        get() = _tokenCheckData

    private val _logout = MutableLiveData<LOGOUT>()
    val logout: LiveData<LOGOUT>
        get() = _logout

    init {
        checkToken()
    }

    private fun checkToken() {
        viewModelScope.launch {
            try {
                Log.i("MainViewModel", "Begin...")

                val tokenData = TokenCheck(
                    authorized = true,
                    restartOnTokenChange = true, // server ignore this, value returned from server is used
                    token = token,
                    refreshToken = refreshToken
                )
                val returnedData = StrangeeApi.retrofitService.postTokenCheck(tokenData)
                Log.i("MainViewModel", returnedData.toString())

                if (returnedData.token.isNotEmpty()) {
                    _tokenCheckData.value = returnedData
                }
            } catch (t: ConnectException) {
                Log.i("MainViewModel", "Connection_Error ::: $t")
            } catch (t: Throwable) {
                Log.i("MainViewModel", "Failed ::: $t")
            }
        }
    }

    fun startLogoutProcess(token: String, userId: String) {
        viewModelScope.launch {
            _logout.value = LOGOUT.LOGGING_OUT
            try {
                val result = StrangeeApi.retrofitService.refreshFcmToken("Bearer ".plus(token), userId, null)

                if(result) {
                    _logout.value = LOGOUT.LOGOUT_SUCCESSFUL
                } else {
                    _logout.value = LOGOUT.LOGOUT_FAILED
                }
            } catch (t: ConnectException) {
                Log.i("MainViewModel", "Connection_Error ::: $t")
                _logout.value = LOGOUT.LOGOUT_ERROR
            } catch (t: UnknownHostException) {
                Log.i("LoginViewModel", "HOST EXCEPTION ::: $t")
                _logout.value = LOGOUT.LOGOUT_ERROR
            } catch (t: Throwable) {
                Log.i("MainViewModel", "Failed ::: $t")
                _logout.value = LOGOUT.LOGOUT_FAILED
            }
        }
    }

    fun onTokenDataChecked() {
        _tokenCheckData.value = null
    }

    fun onLogoutComplete() {
        _logout.value = null
    }

    override fun onCleared() {
        super.onCleared()
    }
}