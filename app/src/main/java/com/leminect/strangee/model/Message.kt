package com.leminect.strangee.model

data class Message(
    val userId: String,
    val text: String?,
    val type: String,
    val imageUrl: String?,
    val timestamp: Long,
    val id: String?
)