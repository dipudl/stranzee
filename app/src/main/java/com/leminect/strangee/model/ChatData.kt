package com.leminect.strangee.model

data class ChatData(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val imageUrl: String,
    val timestamp: Long,
    val message: String
)
