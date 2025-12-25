package com.example.deliveryapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MessageDto(
    val id: Long,
    val order_id: Long,
    val from_user_id: Long,
    val to_user_id: Long,


//    val sender_id: Long,      // ✅ Đổi từ from_user_id
//    val receiver_id: Long,    // ✅ Đổi từ to_user_id



    val content: String,
    val is_read: Boolean,
    val created_at: String
)

data class MessagesResponse(
    //val messages: List<MessageDto>
    val data: List<MessageDto>
)