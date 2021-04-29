package com.leminect.strangee.model

import java.io.Serializable

data class SingleChatPerson(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val imageUrl: String,
    val isOnline: Boolean,
    val country: String,
    val gender: String,
    val interestedIn: List<String>,
    val birthday: Long,
    val aboutMe: String,
    var saved: Boolean,
) : Serializable
