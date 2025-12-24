package com.example.deliveryapp.data.remote.dto

data class ReviewRequestDto(
    val product_id: Long,
    val order_id: Long,   // cần gắn orderId để backend check đã mua và delivered
    val rate: Int,
    val content: String
)

//data class ReviewResponseDto(
//    val user_id: Long,
//    val user_name: String,
//    val rate: Int,
//    val content: String,
//    val created_at: String,
//    val images: List<ReviewImageDto> = emptyList()
//)
//
//data class ReviewImageDto(
//    val image_id: Long,
//    val url: String
//)


data class ReviewResponseDto(
    val id: Long = 0,
    val product_id: Long = 0,
    val user_id: Long = 0,
    val user_name: String = "",
    val order_id: Long = 0,
    val rate: Int = 0,
    val content: String = "",
    val created_at: String = "",
    val images: List<ReviewImageDto> = emptyList(),

    )

data class ReviewImageDto(
    val image_id: Long = 0,
    val url: String = ""
)
