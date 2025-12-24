package com.example.deliveryapp.data.remote.api

import com.example.deliveryapp.data.remote.dto.ProductDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.deliveryapp.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ProductApi {
    @GET("products")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ProductsListResponse>


    @GET("products/{id}")
    suspend fun getProductById(@Path("id") id: Long): Response<ProductWrapper>

    // ⭐ Thêm API Review
//    @POST("create-review")
//    suspend fun createReview(@Body req: ReviewRequestDto): Response<Unit>
    @Multipart
    @POST("customer/create-review")
    suspend fun createReview(
        @Part("product_id") productId: RequestBody,
        @Part("order_id") orderId: RequestBody,
        @Part("rate") rate: RequestBody,
        @Part("content") content: RequestBody,
        @Part images: List<MultipartBody.Part> = emptyList()
    ): Response<MessageResponse>


    @GET("products/{id}/reviews")
    suspend fun getReviews(@Path("id") productId: Long): Response<ReviewsListResponse>
}

data class ProductsListResponse(
    val products: List<ProductDto>,
    val pagination: Pagination?
)

data class Pagination(
    val page: Int,
    val limit: Int,
    val total: Int,
    val total_pages: Int
)

data class ProductWrapper(
    val product: ProductDto
)
data class ReviewsListResponse(
    val page: Int,
    val limit: Int,
    val totalCount: Int,
    val totalPage: Int,
    val reviews: List<ReviewResponseDto>
)