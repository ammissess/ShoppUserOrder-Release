// ui/product/ProductReviewViewModel.kt
package com.example.deliveryapp.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.deliveryapp.data.remote.dto.ProductDto
import com.example.deliveryapp.data.remote.dto.ReviewResponseDto
import com.example.deliveryapp.data.repository.ProductRepository
import com.example.deliveryapp.domain.usecase.SubmitReviewUseCase
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductReviewViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val submitReviewUseCase: SubmitReviewUseCase
) : ViewModel() {

    private val _product = MutableStateFlow<Resource<ProductDto>>(Resource.Loading())
    val product: StateFlow<Resource<ProductDto>> = _product

    private val _reviews = MutableStateFlow<Resource<List<ReviewResponseDto>>>(Resource.Loading())
    val reviews: StateFlow<Resource<List<ReviewResponseDto>>> = _reviews

    private val _submitReviewResult = MutableStateFlow<Resource<String>?>(null)
    val submitReviewResult: StateFlow<Resource<String>?> = _submitReviewResult

    private val _hasUserReviewed = MutableStateFlow(false)
    val hasUserReviewed: StateFlow<Boolean> = _hasUserReviewed

    fun loadProductAndReviews(productId: Long) {
        viewModelScope.launch {
            // Load product detail
            _product.value = Resource.Loading()
            _product.value = productRepository.getProductDetail(productId)

            // Load reviews
            _reviews.value = Resource.Loading()
            _reviews.value = productRepository.getReviews(productId)
        }
    }

    fun checkIfAlreadyReviewed(productId: Long, orderId: Long) {
        viewModelScope.launch {
            val reviewsResult = productRepository.getReviews(productId)
            if (reviewsResult is Resource.Success) {
                val reviews = reviewsResult.data ?: emptyList()
                _hasUserReviewed.value = reviews.any { it.order_id == orderId }
            }
        }
    }

    fun submitReview(
        navController: NavController,
        productId: Long,
        orderId: Long,
        rating: Int,
        content: String
    ) {
        viewModelScope.launch {
            _submitReviewResult.value = Resource.Loading()
            val result = submitReviewUseCase(productId, orderId, rating, content)
            _submitReviewResult.value = result

            if (result is Resource.Success) {
                // ✅ Đánh dấu là đã đánh giá
                _hasUserReviewed.value = true

                // ✅ Tải lại danh sách review mới nhất
                _reviews.value = productRepository.getReviews(productId)

                // ✅ Báo cho HomeScreen reload lại sản phẩm
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("refresh_products", true)
            }
        }
    }




    fun resetSubmitState() {
        _submitReviewResult.value = null
    }
}