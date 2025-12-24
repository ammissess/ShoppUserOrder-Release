package com.example.deliveryapp.domain.usecase

import com.example.deliveryapp.data.remote.dto.ReviewRequestDto
import com.example.deliveryapp.data.repository.ProductRepository
import com.example.deliveryapp.utils.Resource
import javax.inject.Inject

class SubmitReviewUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(
        productId: Long,
        orderId: Long,
        rate: Int,
        content: String
    ): Resource<String> {
        if (rate < 1 || rate > 5) {
            return Resource.Error("Số sao phải từ 1 đến 5")
        }
        if (content.isBlank()) {
            return Resource.Error("Vui lòng nhập nội dung đánh giá")
        }

        val request = ReviewRequestDto(
            product_id = productId,
            order_id = orderId,
            rate = rate,
            content = content
        )

        //return repository.createReview(request)
        return repository.createReview(productId, orderId, rate, content)
    }
}