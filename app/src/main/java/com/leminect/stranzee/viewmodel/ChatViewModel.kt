package com.leminect.stranzee.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leminect.stranzee.model.ChatData
import com.leminect.stranzee.network.StrangeeApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.ConnectException

enum class ChatStatus { LOADING, ERROR, FAILED, EMPTY, DONE }

class ChatViewModel(token: String, userId: String): ViewModel() {
    private val _status = MutableLiveData<ChatStatus>()
    val status: LiveData<ChatStatus>
        get() = _status

    private val _chatList = MutableLiveData<List<ChatData>>()
    val chatList: LiveData<List<ChatData>>
        get() = _chatList

    private val _navigateToSelectedChat = MutableLiveData<ChatData>()
    val navigateToSelectedChat: LiveData<ChatData>
        get() = _navigateToSelectedChat

    private val viewModelJob = Job()

    init {
        // getChatList(token, userId)
    }

    fun displaySavedProfile(chatData: ChatData) {
        _navigateToSelectedChat.value = chatData
    }

    fun onDisplayChatComplete() {
        _navigateToSelectedChat.value = null
    }

    fun getChatList(token: String, userId: String) {
        viewModelScope.launch {
            try {
                Log.i("ChatViewModel", "Begin...")
                _status.value = ChatStatus.LOADING

                val returnedValue = StrangeeApi.retrofitService.getChat("Bearer ".plus(token), userId)
                Log.i("ChatViewModel", returnedValue.toString())

                if (returnedValue.isNotEmpty()) {
                    _chatList.value = returnedValue
                    _status.value = ChatStatus.DONE
                } else {
                    _status.value = ChatStatus.EMPTY
                }
            } catch (t: ConnectException) {
                Log.i("ChatViewModel", "Connection_Error ::: $t")
                _status.value = ChatStatus.ERROR
            } catch (t: Throwable) {
                Log.i("ChatViewModel", "Failed ::: $t")
                _status.value = ChatStatus.FAILED
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}