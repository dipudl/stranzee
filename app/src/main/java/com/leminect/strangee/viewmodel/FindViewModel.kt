package com.leminect.strangee.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leminect.strangee.model.Strangee
import com.leminect.strangee.model.User
import com.leminect.strangee.network.SaveStrangeeBackData
import com.leminect.strangee.network.SaveStrangeePostData
import com.leminect.strangee.network.StrangeeApi
import com.leminect.strangee.network.StrangeeBackData
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class FindStatus { LOADING, ERROR, FILTER_NOT_FOUND, DONE }

class FindViewModel(token: String, user: User) : ViewModel() {
    private val _status = MutableLiveData<FindStatus>()
    val status: LiveData<FindStatus>
        get() = _status

    private val _strangeeList = MutableLiveData<List<Strangee>>()
    val strangeeList: LiveData<List<Strangee>>
        get() = _strangeeList

    private val _lastCreatedAt = MutableLiveData<String>()
    val lastCreatedAt: LiveData<String>
        get() = _lastCreatedAt

    private val _scrollPaginationEnabled = MutableLiveData<Boolean>()
    val scrollPaginationEnabled: LiveData<Boolean>
        get() = _scrollPaginationEnabled

    private val _saveBackData = MutableLiveData<SaveStrangeeBackData>()
    val saveBackData: LiveData<SaveStrangeeBackData>
        get() = _saveBackData

    private val _filterEnabled = MutableLiveData<Boolean>(false)
    val filterEnabled: LiveData<Boolean>
        get() = _filterEnabled

    private val _navigateToSelectedStrangee = MutableLiveData<Strangee>()
    val navigateToSelectedStrangee: LiveData<Strangee>
        get() = _navigateToSelectedStrangee

    private val viewModelJob = Job()

    init {
        getStrangeeList(token, user)
    }

    fun setFilterEnabled(enabled: Boolean) {
        _filterEnabled.value = enabled
    }

    fun displayStrangeeProfile(strangee: Strangee) {
        _navigateToSelectedStrangee.value = strangee
    }

    fun onDisplayStrangeeProfileComplete() {
        _navigateToSelectedStrangee.value = null
    }

    fun getStrangeeList(token: String, user: User, changeStatus: Boolean = true) {
        if (_filterEnabled.value == true) {
            _strangeeList.value = listOf<Strangee>()
            _scrollPaginationEnabled.value = null
            _lastCreatedAt.value = null
        }

        _scrollPaginationEnabled.value = false
        viewModelScope.launch {
            try {
                Log.i("FindViewModel", "Begin...")
                if (changeStatus) _status.value = FindStatus.LOADING

                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                val backResult = StrangeeApi.retrofitService.getStrangee("Bearer ".plus(token),
                    moshi.adapter(User::class.java).toJson(user),
                    _lastCreatedAt.value ?: "0",
                    _filterEnabled.value ?: false)
                Log.i("FindViewModel", backResult.toString())

                if (backResult.data.isNotEmpty() && backResult.createdAt.isNotEmpty()) {
                    _strangeeList.value = (_strangeeList.value ?: listOf()) + backResult.data
                    if (changeStatus) _status.value = FindStatus.DONE
                    _lastCreatedAt.value = backResult.createdAt

                    Log.i("FindViewModel", "CreatedOn: ${backResult.createdAt}")
                }

                _scrollPaginationEnabled.value = backResult.data.isNotEmpty()
            } catch (t: JsonDataException) {
                Log.i("FindViewModel", "JSON_EXC_Error ::: $t")
                if (changeStatus) {
                    if (_filterEnabled.value == true)
                        _status.value = FindStatus.FILTER_NOT_FOUND
                    else
                        _status.value = FindStatus.ERROR
                }
                _scrollPaginationEnabled.value = false
            } catch (t: Throwable) {
                Log.i("FindViewModel", "FIND_Error ::: $t")
                if (changeStatus) _status.value = FindStatus.ERROR
                _scrollPaginationEnabled.value = true
            }
        }
    }

    fun saveProfile(token: String, strangee: Strangee) {
        viewModelScope.launch {
            try {
                Log.i("FindViewModel", "Save...")

                val sss = SaveStrangeePostData(strangee.userId, strangee.saved)
                Log.i("FindViewModelSSS", sss.toString())

                val saveResult =
                    StrangeeApi.retrofitService.saveStrangee("Bearer ".plus(token), sss)
                Log.i("FindViewModel", saveResult.toString())

                if (saveResult.userId.isNotEmpty()) {
                    _saveBackData.value = saveResult
                }
            } catch (t: Throwable) {
                Log.i("FindViewModel", "SAVE_Error ::: $t")
            }
        }
    }

    fun findAndUpdateListIndex(matchId: String, saveStatus: Boolean): Int {
        for (i in 0 until (_strangeeList.value?.size ?: 0)) {
            if (_strangeeList.value?.get(i)?.userId == matchId) {
                _strangeeList.value!![i].saved = saveStatus
                return i
            }
        }
        return -1
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}