package com.example.gasml.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isFromUser: Boolean = true
)
