package com.example.deliveryapp.data.repository

import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.api.ProductApi
import com.example.deliveryapp.data.remote.dto.*
import com.example.deliveryapp.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

//class ProductRepository(private val api: ProductApi) {
class ProductRepository @Inject constructor(
    private val api: ProductApi,
    private val dataStore: DataStoreManager
) {
    suspend fun getProducts(page: Int = 1, limit: Int = 20): Resource<List<ProductDto>> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.getProducts(page, limit)
                if (resp.isSuccessful) {
                    Resource.Success(resp.body()?.products ?: emptyList())
                } else {
                    Resource.Error("Error: ${resp.code()}")
                }
            } catch (e: IOException) {
                Resource.Error("Network error")
            } catch (e: HttpException) {
                Resource.Error("Server error")
            }
        }

    suspend fun getProductById(id: Long): Resource<ProductDto> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.getProductById(id)
                if (resp.isSuccessful) {
                    resp.body()?.product?.let { Resource.Success(it) }
                        ?: Resource.Error("Empty body")
                } else {
                    Resource.Error("Error: ${resp.code()}")
                }
            } catch (e: Exception) {
                Resource.Error("Error: ${e.message}")
            }
        }

    suspend fun getProductDetail(id: Long): Resource<ProductDto> {
        return try {
            val response = api.getProductById(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.product)
            } else {
                Resource.Error(response.message() ?: "Unknown error")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

//    // ⭐ Tạo review
//    suspend fun createReview(request: ReviewRequestDto): Resource<Unit> =
//        withContext(Dispatchers.IO) {
//            try {
//                val resp = api.createReview(request)
//                if (resp.isSuccessful) Resource.Success(Unit)
//                else Resource.Error(resp.errorBody()?.string() ?: "Error create review")
//            } catch (e: Exception) {
//                Resource.Error("Error: ${e.message}")
//            }
//        }
//
//    // ⭐ Lấy danh sách review
//    suspend fun getReviews(productId: Long): Resource<List<ReviewResponseDto>> =
//        withContext(Dispatchers.IO) {
//            try {
//                val resp = api.getReviews(productId)
//                if (resp.isSuccessful) {
//                    Resource.Success(resp.body()?.reviews ?: emptyList())
//                } else {
//                    Resource.Error(resp.errorBody()?.string() ?: "Error load reviews")
//                }
//            } catch (e: Exception) {
//                Resource.Error("Error: ${e.message}")
//            }
//        }

    suspend fun getReviews(productId: Long): Resource<List<ReviewResponseDto>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getReviews(productId)
                if (response.isSuccessful) {
                    Resource.Success(response.body()?.reviews ?: emptyList())
                } else {
                    Resource.Error(response.errorBody()?.string() ?: "Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Network error")
            }
        }

    suspend fun createReview(
        productId: Long,
        orderId: Long,
        rate: Int,
        content: String,
        images: List<java.io.File> = emptyList()
    ): Resource<String> {
        return try {
            val token = dataStore.accessToken.first()
            if (token.isNullOrEmpty()) {
                return Resource.Error("Chưa đăng nhập")
            }

            val productIdBody = productId.toString().toRequestBody("text/plain".toMediaType())
            val orderIdBody = orderId.toString().toRequestBody("text/plain".toMediaType())
            val rateBody = rate.toString().toRequestBody("text/plain".toMediaType())
            val contentBody = content.toRequestBody("text/plain".toMediaType())

            val imageParts = images.mapIndexed { index, file ->
                val reqFile = file.asRequestBody("image/*".toMediaType())
                MultipartBody.Part.createFormData("images", "image_$index.jpg", reqFile)
            }

            val response = api.createReview(
                productId = productIdBody,
                orderId = orderIdBody,
                rate = rateBody,
                content = contentBody,
                images = imageParts
            )

            if (response.isSuccessful) {
                Resource.Success("Đánh giá thành công")
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error(errorBody ?: "Không thể gửi đánh giá")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi kết nối")
        }
    }
}