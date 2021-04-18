package com.leminect.strangee.model

import com.squareup.moshi.Json
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

data class User(
    val firstName: String,
    val lastName: String,
    val imageUrl: String,
    val country: String,
    val gender: String,
    val interestedIn: List<String>,
    val birthday: Long,
    val aboutMe: String,
    val email: String,
    /* *password - used only for sign up post request
       *not sent via server(not even the hash) or from here in other requests
       *so remains null in other processes */
    val password: String = "",
    @Json(name = "_id") val userId: String = "",
) : Serializable {
    fun formatTime(): String {
        val sdf: SimpleDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.US)
        val localDateTime: String = sdf.format(birthday);
        return localDateTime
    }
}
