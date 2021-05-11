package com.leminect.stranzee.model

data class ChatData(
    val strangeeId: String,
    val firstName: String,
    val lastName: String,
    val imageUrl: String,
    val timestamp: Long,
    val isRead: Boolean,
    val message: String,
    val isOnline: Boolean,
    val country: String,
    val gender: String,
    val interestedIn: List<String>,
    val birthday: Long,
    val aboutMe: String,
    var saved: Boolean,
)
