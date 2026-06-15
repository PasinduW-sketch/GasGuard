package com.example.gasml.model

import com.google.firebase.Timestamp

data class Conversation(
    val chatId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastTimestamp: Timestamp = Timestamp.now(),
    val lastSenderId: String = "",
    val unreadCount: Map<String, Long> = emptyMap()
)
