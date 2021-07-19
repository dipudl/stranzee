package com.leminect.stranzee.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.leminect.stranzee.model.User
import com.leminect.stranzee.network.CheckRegistrationInput
import com.leminect.stranzee.network.AuthBackData
import com.leminect.stranzee.network.StrangeeApi
import com.leminect.stranzee.utility.getMimeType
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

enum class SignUpStatus { CHECKING, CHECK_ERROR, CHECK_PASSED, CHECK_FAILED, SIGNING_UP, SIGN_UP_ERROR, SIGN_UP_PASSED, SIGN_UP_FAILED }

class SignUpViewModel : ViewModel() {
    private val _status = MutableLiveData<SignUpStatus>()
    val status: LiveData<SignUpStatus>
        get() = _status

    private val _signUpBackData = MutableLiveData<AuthBackData>()
    val signUpBackData: LiveData<AuthBackData>
        get() = _signUpBackData

    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

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

    fun checkAndSignUpUser(input: CheckRegistrationInput, user: User) {
        viewModelScope.launch {
            try {
                _status.value = SignUpStatus.CHECKING
                val checkUserRegistration = StrangeeApi.retrofitService.postCheckRegistration(input)

                Log.i("SignUpViewModel", checkUserRegistration.toString())

                if (checkUserRegistration.userNotExist) {
                    _status.value = SignUpStatus.CHECK_PASSED

                    if(initialFcmToken != null) {
                        //sign up user
                        signUpUser(user, initialFcmToken!!)
                    } else {
                        // Generate FCM token and sign up user
                        generateFcmToken(user)
                    }

                } else {
                    _status.value = SignUpStatus.CHECK_FAILED
                }
            } catch (t: Throwable) {
                Log.i("SignUpViewModel", "CHECK_Error ::: $t")
                _status.value = SignUpStatus.CHECK_ERROR
            }
        }
    }

    private fun generateFcmToken(user: User) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get new FCM registration token
                val fcmToken = task.result

                uiScope.launch {
                    signUpUser(user, fcmToken)
                }
            } else {
                Log.i("SignUpViewModel",
                    "Fetching FCM registration token failed: ${task.exception}")
                _status.value = SignUpStatus.SIGN_UP_FAILED
            }
        }
    }

    private suspend fun signUpUser(user: User, fcmToken: String) {
        withContext(Dispatchers.Main) {
            try {
                _status.value = SignUpStatus.SIGNING_UP

                val file: File = File(user.imageUrl)
                val requestBody: RequestBody =
                    RequestBody.create(MediaType.parse(file.getMimeType("image/png")), file)
                val part: MultipartBody.Part =
                    MultipartBody.Part.createFormData("profileImage", file.name, requestBody)

                var interestArrayString = ""
                for (i in user.interestedIn.indices) {
                    interestArrayString += user.interestedIn[i]

                    if (i < user.interestedIn.size - 1)
                        interestArrayString += ","
                }

                val signUpServerData = StrangeeApi.retrofitService.postSignUp(part,
                    RequestBody.create(MediaType.parse("text/plain"), user.email),
                    RequestBody.create(MediaType.parse("text/plain"), user.password),
                    RequestBody.create(MediaType.parse("text/plain"), user.firstName),
                    RequestBody.create(MediaType.parse("text/plain"), user.lastName),
                    RequestBody.create(MediaType.parse("text/plain"), user.country),
                    RequestBody.create(MediaType.parse("text/plain"), user.gender),
                    RequestBody.create(MediaType.parse("text/plain"), user.aboutMe),
                    RequestBody.create(MediaType.parse("text/plain"), interestArrayString),
                    RequestBody.create(MediaType.parse("text/plain"), user.birthday.toString()),
                    RequestBody.create(MediaType.parse("text/plain"), fcmToken)
                )

                Log.i("SignUpViewModel", signUpServerData.toString())

                if (signUpServerData.token.isNotEmpty()) {
                    _signUpBackData.value = signUpServerData
                    _status.value = SignUpStatus.SIGN_UP_PASSED
                } else {
                    _status.value = SignUpStatus.SIGN_UP_FAILED
                }

            } catch (t: Throwable) {
                Log.i("SignUpViewModel", "SIGN_UP_Error ::: $t")
                _status.value = SignUpStatus.SIGN_UP_ERROR
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

}