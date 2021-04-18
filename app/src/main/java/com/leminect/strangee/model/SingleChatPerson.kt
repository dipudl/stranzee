package com.leminect.strangee.model

import java.io.Serializable

data class SingleChatPerson(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val imageUrl: String,
    var isOnline: Boolean
) : Serializable
