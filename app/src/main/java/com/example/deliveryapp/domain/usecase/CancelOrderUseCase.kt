
package com.example.deliveryapp.domain.usecase

import com.example.deliveryapp.data.repository.OrderRepository
import com.example.deliveryapp.utils.Resource
import javax.inject.Inject

class CancelOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: Long): Resource<String> {
        return orderRepository.cancelOrder(orderId)
    }
}