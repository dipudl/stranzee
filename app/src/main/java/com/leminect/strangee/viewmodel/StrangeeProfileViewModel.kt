package com.leminect.strangee.viewmodel

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.leminect.strangee.model.Strangee
import com.leminect.strangee.network.*
import io.socket.emitter.Emitter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.ConnectException

enum class StrangeeProfileStatus { REPORTING, REPORT_DONE, ERROR, FAILED, DONE }

class StrangeeProfileViewModel(token: String, userId: String, private val strangeeId: String) : ViewModel() {
    private val timer: CountDownTimer
    private val roomData: RoomData = RoomData(userId, strangeeId, "status", token)

    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean>
        get() = _isOnline

    private val _status = MutableLiveData<StrangeeProfileStatus>()
    val status: LiveData<StrangeeProfileStatus>
        get() = _status

    private val _saveBackData = MutableLiveData<SaveStrangeeBackData>()
    val saveBackData: LiveData<SaveStrangeeBackData>
        get() = _saveBackData

    private val _blockedBackData = MutableLiveData<Boolean>(false)
    val blockedBackData: LiveData<Boolean>
        get() = _blockedBackData

    private val _blockingProcessBackData = MutableLiveData<BlockProfileBackData>()
    val blockingProcessBackData: LiveData<BlockProfileBackData>
        get() = _blockingProcessBackData

    private val viewModelJob = Job()

    init {
        checkBlocked(token)

        timer = object : CountDownTimer(5000, 5000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                addWhoCheckedMe(token)
            }
        }
        timer.start()

        SocketManager.getSocket()?.emit("subscribe", Gson().toJson(roomData))
        SocketManager.getSocket()?.on("statusChange") {
//            Log.i("MainActivitySocket", it[0].toString())

            _isOnline.postValue(it[0].toString() == "online")
        }
    }

    fun addWhoCheckedMe(token: String) {
        viewModelScope.launch {
            try {
                Log.i("SProfileViewModel", "Adding WhoCheckedMe...")

                val reportResult =
                    StrangeeApi.retrofitService.postWhoCheckedMe("Bearer ".plus(token), strangeeId)
                Log.i("SProfileViewModel", reportResult.toString())
            } catch (t: ConnectException) {
                Log.i("SProfileViewModel", "WhoCheckedMe_Error ::: $t")
                _status.value = StrangeeProfileStatus.ERROR
            } catch (t: Throwable) {
                Log.i("SProfileViewModel", "WhoCheckedMe_Failed ::: $t")
                _status.value = StrangeeProfileStatus.FAILED
            }
        }
    }

    fun onPopupComplete() {
        _status.value = StrangeeProfileStatus.DONE
    }

    fun reportUser(token: String, userId: String, message: String) {
        _status.value = StrangeeProfileStatus.REPORTING

        viewModelScope.launch {
            try {
                Log.i("SProfileViewModel", "Reporting...")

                val reportResult =
                    StrangeeApi.retrofitService.postReportProfile("Bearer ".plus(token),
                        userId,
                        strangeeId,
                        message)
                Log.i("SProfileViewModel", reportResult.toString())

                if (reportResult) {
                    _status.value = StrangeeProfileStatus.REPORT_DONE
                } else {
                    _status.value = StrangeeProfileStatus.FAILED
                }
            } catch (t: ConnectException) {
                Log.i("SProfileViewModel", "REPORT_Error ::: $t")
                _status.value = StrangeeProfileStatus.ERROR
            } catch (t: Throwable) {
                Log.i("SProfileViewModel", "REPORT_Failed ::: $t")
                _status.value = StrangeeProfileStatus.FAILED
            }
        }
    }

    fun blockUser(token: String) {
        val blockStatus: Boolean = blockedBackData.value == true

        Log.i("SProfileViewModel", "BLOCK_Status ::: $blockStatus")

        viewModelScope.launch {
            try {
                Log.i("SProfileViewModel", "Blocking... $blockStatus")

                val blockingResult =
                    StrangeeApi.retrofitService.postBlockProfile("Bearer ".plus(token),
                        strangeeId,
                        blockStatus)
                Log.i("SProfileViewModel", blockingResult.toString())

                _blockingProcessBackData.value = blockingResult
                _blockedBackData.value = blockingResult.blockedStatus
            } catch (t: ConnectException) {
                Log.i("SProfileViewModel", "BLOCKING_Error ::: $t")
                _blockingProcessBackData.value = BlockProfileBackData(strangeeId, true, blockStatus)
            } catch (t: Throwable) {
                Log.i("SProfileViewModel", "BLOCKING_Failed ::: $t")
                _blockingProcessBackData.value = BlockProfileBackData(strangeeId, true, blockStatus)
            }
        }
    }

    private fun checkBlocked(token: String) {
        viewModelScope.launch {
            try {
                Log.i("SProfileViewModel", "Checking blocked...")

                val blockedResult =
                    StrangeeApi.retrofitService.getIsBlocked("Bearer ".plus(token), strangeeId)
                Log.i("SProfileViewModel", "Blocked: $blockedResult")

                _blockedBackData.value = blockedResult
            } catch (t: ConnectException) {
                Log.i("SProfileViewModel", "BLOCKING_Error ::: $t")
                _status.value = StrangeeProfileStatus.ERROR
            } catch (t: Throwable) {
                Log.i("SProfileViewModel", "BLOCKING_Failed ::: $t")
            }
        }
    }

    fun saveOrUnSaveProfile(token: String, strangee: Strangee) {
        viewModelScope.launch {
            try {
                Log.i("SProfileViewModel", "Save...")

                val sss = SaveStrangeePostData(strangee.userId, strangee.saved)
                Log.i("SProfileViewModelSSS", sss.toString())

                val saveResult =
                    StrangeeApi.retrofitService.saveStrangee("Bearer ".plus(token), sss)
                Log.i("SProfileViewModel", saveResult.toString())

                _saveBackData.value = saveResult
            } catch (t: ConnectException) {
                Log.i("SProfileViewModel", "SAVE_Error ::: $t")
                _saveBackData.value = SaveStrangeeBackData(strangee.userId, true, strangee.saved)
            } catch (t: Throwable) {
                Log.i("SProfileViewModel", "SAVE_Failed ::: $t")
                _saveBackData.value = SaveStrangeeBackData(strangee.userId, true, strangee.saved)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        SocketManager.getSocket()?.emit("unsubscribe", Gson().toJson(roomData))
        timer.cancel()
        viewModelJob.cancel()
    }
}