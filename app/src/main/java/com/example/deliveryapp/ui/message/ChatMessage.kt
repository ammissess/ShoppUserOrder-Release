package com.example.deliveryapp.ui.message

data class ChatMessage(
    val fromUserId: Long,
    val toUserId: Long,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
