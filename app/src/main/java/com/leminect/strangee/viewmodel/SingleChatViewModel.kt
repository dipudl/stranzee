package com.leminect.strangee.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.leminect.strangee.model.ChatData
import com.leminect.strangee.model.Message
import com.leminect.strangee.network.MessageWithToken
import com.leminect.strangee.network.RoomData
import com.leminect.strangee.network.SocketManager
import com.leminect.strangee.network.StrangeeApi
import com.leminect.strangee.utility.getMimeType
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.net.ConnectException

enum class SingleChatStatus { UPLOADING, UPLOAD_ERROR, UPLOAD_FAILED, UPLOAD_DONE }

class SingleChatViewModel(
    private val token: String,
    userId: String,
    private val strangeeId: String,
) :
    ViewModel() {
    private val statusRoomData: RoomData = RoomData(userId, strangeeId, "status", token)
    private val chatRoomData: RoomData = RoomData(userId, strangeeId, "chat", token)
    private val gson = Gson()

    private val _status = MutableLiveData<SingleChatStatus>()
    val status: LiveData<SingleChatStatus>
        get() = _status

    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean>
        get() = _isOnline

    private val _initialOldMessageLoad = MutableLiveData<Boolean>()
    val initialOldMessageLoad: LiveData<Boolean>
        get() = _initialOldMessageLoad

    private val _messageList = MutableLiveData<List<Message>>()
    val messageList: LiveData<List<Message>>
        get() = _messageList

    private val _imageUploadUrl = MutableLiveData<String>()
    val imageUploadUrl: LiveData<String>
        get() = _imageUploadUrl

    private val viewModelJob = Job()

    init {
        SocketManager.getSocket()?.emit("subscribe", gson.toJson(statusRoomData))
        SocketManager.getSocket()?.emit("subscribe", gson.toJson(chatRoomData))

        SocketManager.getSocket()?.on("statusChange") {
            _isOnline.postValue(it[0].toString() == "online")
        }
        SocketManager.getSocket()?.on("older messages") {
            if(_initialOldMessageLoad.value == null) {
                _initialOldMessageLoad.postValue(true)
            }

            _messageList.postValue((gson.fromJson(it[0].toString(),
                Array<Message>::class.java) as Array<Message>).toList() + (_messageList.value
                ?: listOf<Message>()))

            Log.i("MainActivitySocket", it[0].toString())
        }
        SocketManager.getSocket()?.on("new message") {
            _messageList.postValue((_messageList.value
                ?: listOf<Message>()) + (gson.fromJson(it[0].toString(),
                Array<Message>::class.java)) as Array<Message>)

            Log.i("MainActivitySocket", it[0].toString())
        }
    }

    fun onInitialOldMessageLoadComplete() {
        _initialOldMessageLoad.value = false
    }

    fun sendMessage(message: Message) {
        val messageWithToken = MessageWithToken(token, message)
        SocketManager.getSocket()?.emit("message", gson.toJson(messageWithToken))
    }

    fun onImageUrlUsed() {
        _imageUploadUrl.value = null
    }

    fun uploadImage(imagePath: String) {
        viewModelScope.launch {
            try {
                _status.value = SingleChatStatus.UPLOADING

                val file: File = File(imagePath)
                val requestBody: RequestBody =
                    RequestBody.create(MediaType.parse(file.getMimeType("image/png")), file)
                val part: MultipartBody.Part =
                    MultipartBody.Part.createFormData("imageByUser", file.name, requestBody)

                val returnedData = StrangeeApi.retrofitService.postImage(
                    "Bearer ".plus(token),
                    part
                )

                Log.i("SingleChatViewModel", returnedData)

                if (returnedData.isNotEmpty()) {
                    _imageUploadUrl.value = returnedData
                    _status.value = SingleChatStatus.UPLOAD_DONE
                } else {
                    _status.value = SingleChatStatus.UPLOAD_FAILED
                }
            } catch (t: ConnectException) {
                Log.i("SingleChatViewModel", "CONNECT EXCEPTION ::: $t")
                _status.value = SingleChatStatus.UPLOAD_ERROR
            } catch (t: Throwable) {
                Log.i("SingleChatViewModel", "PIC_UPDATE_Error ::: $t")
                _status.value = SingleChatStatus.UPLOAD_FAILED
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        SocketManager.getSocket()?.emit("unsubscribe", gson.toJson(statusRoomData))
        SocketManager.getSocket()?.emit("unsubscribe", gson.toJson(chatRoomData))
        viewModelJob.cancel()
    }
}