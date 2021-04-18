package com.leminect.strangee.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leminect.strangee.network.StrangeeApi
import com.leminect.strangee.utility.getMimeType
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.net.ConnectException

enum class ProfileUpdateStatus { UPDATING, UPDATE_ERROR, UPDATE_FAILED, UPDATE_DONE }

class ProfileViewModel : ViewModel() {

    private val _status = MutableLiveData<ProfileUpdateStatus>()
    val status: LiveData<ProfileUpdateStatus>
        get() = _status

    private val viewModelJob = Job()

    fun updateProfileImage(token: String, profileImageUri: Uri, userId: String) {
        viewModelScope.launch {
            try {
                _status.value = ProfileUpdateStatus.UPDATING

                val file: File = File(profileImageUri.path!!)
                val requestBody: RequestBody =
                    RequestBody.create(MediaType.parse(file.getMimeType("image/png")), file)
                val part: MultipartBody.Part =
                    MultipartBody.Part.createFormData("profileImage", file.name, requestBody)

                val returnedData = StrangeeApi.retrofitService.postProfileImage(
                    "Bearer ".plus(token),
                    RequestBody.create(MediaType.parse("text/plain"), userId),
                    part
                )

                Log.i("ProfileViewModel", returnedData.toString())

                if (returnedData) {
                    _status.value = ProfileUpdateStatus.UPDATE_DONE
                } else {
                    _status.value = ProfileUpdateStatus.UPDATE_FAILED
                }
            } catch (t: ConnectException) {
                Log.i("ProfileViewModel", "CONNECT EXCEPTION ::: $t")
                _status.value = ProfileUpdateStatus.UPDATE_ERROR
            } catch (t: Throwable) {
                Log.i("ProfileViewModel", "PIC_UPDATE_Error ::: $t")
                _status.value = ProfileUpdateStatus.UPDATE_FAILED
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}