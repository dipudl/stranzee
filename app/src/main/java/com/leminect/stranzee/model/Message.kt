package com.leminect.stranzee.model

data class Message(
    val userId: String,
    val strangeeId: String,
    val text: String?,
    val type: String,
    val imageUrl: String?,
    val timestamp: Long,
    val _id: String?,
    val createdAt: String? = null
)