package com.example.deliveryapp.ui.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.remote.dto.ProductDto
import com.example.deliveryapp.data.remote.dto.ReviewRequestDto
import com.example.deliveryapp.data.remote.dto.ReviewResponseDto
import com.example.deliveryapp.data.repository.ProductRepository
import com.example.deliveryapp.domain.usecase.GetProductDetailUseCase
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val getProduct: GetProductDetailUseCase,
    private val repo: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // State cho chi tiết sản phẩm
    private val _product = MutableStateFlow<Resource<ProductDto>>(Resource.Loading())
    val product: StateFlow<Resource<ProductDto>> = _product

    // State cho reviews
    private val _reviews = MutableStateFlow<Resource<List<ReviewResponseDto>>>(Resource.Loading())
    val reviews: StateFlow<Resource<List<ReviewResponseDto>>> = _reviews

    // Loading indicator & error
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        val id = savedStateHandle.get<Long>("productId") ?: 0L
        if (id > 0) {
            loadProduct(id)
        } else {
            _product.value = Resource.Error("Không tìm thấy ID sản phẩm")
            _errorMessage.value = "ID sản phẩm không hợp lệ"
        }
    }

    /** Load chi tiết sản phẩm theo ID */
    fun loadProduct(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _product.value = getProduct(id)
                if (_product.value is Resource.Success) {
                    loadReviews(id) // load thêm reviews
                } else {
                    _errorMessage.value = (_product.value as? Resource.Error)?.message ?: "Lỗi tải sản phẩm"
                }
            } catch (e: Exception) {
                _product.value = Resource.Error(e.message ?: "Lỗi tải sản phẩm")
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Reload sản phẩm */
    fun reloadProduct(id: Long) = loadProduct(id)

    /** Load reviews cho sản phẩm */
    fun loadReviews(productId: Long) {
        viewModelScope.launch {
            _reviews.value = Resource.Loading()
            _reviews.value = repo.getReviews(productId)
        }
    }

    /** Gửi review (rating + comment) */
//    fun submitReview(orderId: Long, productId: Long, rate: Int, content: String) {
//        viewModelScope.launch {
//            repo.createReview(
//                ReviewRequestDto(product_id = productId, order_id = orderId, rate = rate, content = content)
//            )
//            loadReviews(productId) // refresh review sau khi submit
//        }
//    }
    fun submitReview(orderId: Long, productId: Long, rate: Int, content: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repo.createReview(orderId, productId, rate, content)
                if (result is Resource.Success) {
                    loadReviews(productId) // refresh after success
                } else {
                    _errorMessage.value = (result as? Resource.Error)?.message ?: "Failed to submit review"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}