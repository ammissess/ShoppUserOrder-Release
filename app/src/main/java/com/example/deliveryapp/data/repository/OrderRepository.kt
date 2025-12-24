package com.example.deliveryapp.data.repository

import android.util.Log
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.api.OrderApi
import com.example.deliveryapp.data.remote.api.OrderSummaryDto
import com.example.deliveryapp.data.remote.dto.OrderDetailDto
import com.example.deliveryapp.data.remote.dto.PlaceOrderRequestDto
import com.example.deliveryapp.data.remote.dto.RefreshTokenRequestDto
import com.example.deliveryapp.utils.Constants
import com.example.deliveryapp.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import kotlinx.coroutines.delay
import javax.inject.Inject


private const val TAG = "OrderRepository"

class OrderRepository @Inject constructor(
    private val orderApi: OrderApi,
    private val dataStore: DataStoreManager
) {
//    suspend fun placeOrder(req: PlaceOrderRequestDto): Resource<String> =
//        withContext(Dispatchers.IO) {
//            try {
//                val resp = orderApi.placeOrder(req)
//                if (resp.isSuccessful) {
//                    Resource.Success(resp.body()?.message ?: "Order placed")
//                } else {
//                    Resource.Error("Error: ${resp.code()}")
//                }
//            } catch (e: IOException) {
//                Resource.Error("Network error")
//            } catch (e: HttpException) {
//                Resource.Error("Server error")
//            }
//        }

    // ✅ Thêm hàm cancelOrder Huy don hang
    suspend fun cancelOrder(orderId: Long): Resource<String> {
        return try {
            val response = orderApi.cancelOrder(orderId)
            if (response.isSuccessful) {
                val message = response.body()?.message ?: "Đã hủy đơn hàng thành công"
                Resource.Success(message)
            } else {
                // Xử lý lỗi từ backend
                val errorBody = response.errorBody()?.string()
                val errorMessage = when {
                    errorBody?.contains("not pending") == true ->
                        "Không thể hủy đơn hàng, đơn hàng không ở trạng thái chờ xử lý"
                    errorBody?.contains("not your") == true ->
                        "Đơn hàng này không thuộc về bạn"
                    else ->
                        "Không thể hủy đơn hàng"
                }
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi kết nối khi hủy đơn hàng")
        }
    }
        suspend fun placeOrder(req: PlaceOrderRequestDto): Resource<String> =
            withContext(Dispatchers.IO) {
                try {
                    val resp = orderApi.placeOrder(req)
                    if (resp.isSuccessful) {
                        Resource.Success(resp.body()?.message ?: "Đặt hàng thành công")
                    } else {
                        Resource.Error("Error: ${resp.code()}")
                    }
                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Unknown error")
                }
            }


    suspend fun getOrders(): Resource<List<OrderSummaryDto>> = withContext(Dispatchers.IO) {
        try {
            val resp = orderApi.getOrders()
            if (resp.isSuccessful) {
                Resource.Success(resp.body()?.orders ?: emptyList())
            } else {
                Resource.Error("Error: ${resp.code()}")
            }
        } catch (e: Exception) {
            Resource.Error("Error: ${e.message}")
        }
    }

    suspend fun getOrderDetail(id: Long): Resource<OrderDetailDto> = withContext(Dispatchers.IO) {
        try {
            val resp = orderApi.getOrderDetail(id)
            if (resp.isSuccessful) {
                resp.body()?.let { Resource.Success(it) } ?: Resource.Error("Empty body")
            } else {
                Resource.Error("Error: ${resp.code()}")
            }
        } catch (e: Exception) {
            Resource.Error("Error: ${e.message}")
        }
    }

    // Trong OrderRepository
    suspend fun pollOrderStatus(
        orderId: Long,
        onStatusChange: (String) -> Unit
    ): Resource<OrderDetailDto> {
        while (true) {
            val result = getOrderDetail(orderId)
            if (result is Resource.Success) {
                //val status = result.data?.order?.order_status
                val status = result.data?.order_status
                if (status != null) {
                    onStatusChange(status)
                    if (status == "received") {
                        break
                    }
                }
            }
            delay(5000L)
            // Poll 5s
        }
        return getOrderDetail(orderId)
    }

}

//    suspend fun placeOrderWithRefreshToken(req: PlaceOrderRequestDto, refreshToken: String): Resource<String> = withContext(Dispatchers.IO) {
//        try {
//            Log.d(TAG, "Attempting to place order with refresh token")
//
//            // Sử dụng access token hiện tại trước
//            val currentAccessToken = dataStore.accessToken.first()
//            if (!currentAccessToken.isNullOrEmpty()) {
//                Log.d(TAG, "Using current access token")
//                val client = OkHttpClient.Builder()
//                    .addInterceptor { chain ->
//                        val request = chain.request().newBuilder()
//                            .addHeader("Authorization", "Bearer $currentAccessToken")
//                            .build()
//                        chain.proceed(request)
//                    }
//                    .build()
//
//                val retrofit = Retrofit.Builder()
//                    .baseUrl(Constants.BASE_URL)
//                    .client(client)
//                    .addConverterFactory(GsonConverterFactory.create())
//                    .build()
//
//                val customOrderApi = retrofit.create(OrderApi::class.java)
//
//                try {
//                    val resp = customOrderApi.placeOrder(req)
//                    if (resp.isSuccessful) {
//                        return@withContext Resource.Success(resp.body()?.message ?: "Đặt hàng thành công")
//                    }
//                    // Nếu không thành công và không phải 401, trả về lỗi
//                    if (resp.code() != 401) {
//                        return@withContext Resource.Error("Error: ${resp.code()}")
//                    }
//                    // Nếu 401, tiếp tục thử refresh token
//                } catch (e: Exception) {
//                    Log.e(TAG, "Error with current token: ${e.message}")
//                    // Tiếp tục thử refresh token
//                }
//            }
//
//            // Thử refresh token để lấy token mới
//            Log.d(TAG, "Attempting to refresh token")
//            val client = OkHttpClient.Builder().build()
//            val retrofit = Retrofit.Builder()
//                .baseUrl(Constants.BASE_URL)
//                .client(client)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build()
//
//            val authApi = retrofit.create(com.example.deliveryapp.data.remote.api.AuthApi::class.java)
//
//            val refreshReq = RefreshTokenRequestDto(refreshToken = refreshToken)
//            val refreshResp = authApi.refreshAccessToken(refreshReq)
//
//            if (!refreshResp.isSuccessful) {
//                Log.e(TAG, "Refresh token failed: ${refreshResp.code()}")
//                return@withContext Resource.Error("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại")
//            }
//
//            val newTokens = refreshResp.body() ?: return@withContext Resource.Error("Không nhận được token mới")
//
//            // Lưu token mới
//            dataStore.saveTokens(newTokens.accessToken, newTokens.refreshToken)
//            Log.d(TAG, "Saved new tokens, now placing order with new token")
//
//            // Tạo client mới với token mới
//            val newClient = OkHttpClient.Builder()
//                .addInterceptor { chain ->
//                    val request = chain.request().newBuilder()
//                        .addHeader("Authorization", "Bearer ${newTokens.accessToken}")
//                        .build()
//                    chain.proceed(request)
//                }
//                .build()
//
//            val newRetrofit = Retrofit.Builder()
//                .baseUrl(Constants.BASE_URL)
//                .client(newClient)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build()
//
//            val newOrderApi = newRetrofit.create(OrderApi::class.java)
//
//            // Thử đặt hàng lại với token mới
//            val orderResp = newOrderApi.placeOrder(req)
//
//            if (orderResp.isSuccessful) {
//                Resource.Success(orderResp.body()?.message ?: "Đặt hàng thành công")
//            } else {
//                Log.e(TAG, "Order failed even with new token: ${orderResp.code()}")
//                Resource.Error("Không thể đặt hàng: ${orderResp.code()}")
//            }
//        } catch (e: IOException) {
//            Log.e(TAG, "Network error: ${e.message}", e)
//            Resource.Error("Lỗi mạng")
//        } catch (e: HttpException) {
//            Log.e(TAG, "Server error: ${e.message}", e)
//            Resource.Error("Lỗi server")
//        } catch (e: Exception) {
//            Log.e(TAG, "Unexpected error: ${e.message}", e)
//            Resource.Error("Lỗi không xác định: ${e.message}")
//        }
//    }
//}