package com.leminect.stranzee.model

import com.leminect.stranzee.adapter.convertMillsToAge
import com.squareup.moshi.Json
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

data class Strangee(
    @Json(name = "_id") val userId: String,
    val firstName: String,
    val lastName: String,
    val imageUrl: String,
    val country: String,
    val gender: String,
    val interestedIn: List<String>,
    val birthday: Long,
    val aboutMe: String,
    var saved: Boolean,
) : Serializable {
    fun formatTime(): String {
        val sdf: SimpleDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.US)
        val localDateTime: String = sdf.format(birthday);
        return localDateTime
    }
    fun getAge(): String {
        return convertMillsToAge(birthday).toString()
    }
}
