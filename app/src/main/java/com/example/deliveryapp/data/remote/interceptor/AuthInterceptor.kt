package com.example.deliveryapp.data.remote.interceptor

import android.util.Log
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.api.AuthApi
import com.example.deliveryapp.data.remote.dto.RefreshTokenRequestDto
import com.example.deliveryapp.di.RawAuthApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

private const val TAG = "AuthInterceptor"

class AuthInterceptor @Inject constructor(
    private val dataStore: DataStoreManager,
    @RawAuthApi private val authApi: AuthApi
) : Interceptor {

    companion object {
        private val LOCK = Any()

        // ✅ Callback để thông báo khi cần logout
        @Volatile
        private var logoutCallback: (() -> Unit)? = null

        fun setLogoutCallback(callback: () -> Unit) {
            logoutCallback = callback
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1️⃣ Lấy access token hiện tại
        val accessToken = runBlocking { dataStore.accessToken.first() }

        // 🔒 Lưu token dùng cho request này (để so sánh khi refresh)
        val tokenUsedInRequest = accessToken

        // 2️⃣ Nếu KHÔNG có token → request public
        if (accessToken.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        // 3️⃣ Gắn Authorization
        val request = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = chain.proceed(request)

        // 4️⃣ Không phải 401 => trả luôn
        if (response.code != 401) return response

        // 5️⃣ 401 => Đóng response cũ trước khi retry
        response.close()

        // 6️⃣ Khóa đồng bộ: chỉ 1 luồng được refresh
        synchronized(LOCK) {

            // 6.1) Kiểm tra lại token trong store: nếu đã khác token cũ => có luồng khác refresh xong
            val currentTokenInStore = runBlocking { dataStore.accessToken.first() }
            if (!currentTokenInStore.isNullOrBlank() && currentTokenInStore != tokenUsedInRequest) {
                Log.d(TAG, "Token already refreshed by another request. Retrying with new token.")

                val newRequest = originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer $currentTokenInStore")
                    .build()

                return chain.proceed(newRequest)
            }

            // 6.2) Chưa ai refresh => mình refresh
            val refreshToken = runBlocking { dataStore.refreshToken.first() }

            if (refreshToken.isNullOrBlank()) {
                Log.e(TAG, "No refresh token. Force logout.")
                handleLogout()
                return response
            }

            Log.d(TAG, "Refreshing token...")

            val refreshResp = runBlocking {
                try {
                    authApi.refreshAccessToken(RefreshTokenRequestDto(refreshToken))
                } catch (e: Exception) {
                    Log.e(TAG, "Refresh exception: ${e.message}", e)
                    null
                }
            }

            // 6.3) ✅ Refresh thành công
            if (refreshResp != null && refreshResp.isSuccessful && refreshResp.body() != null) {
                val newAccess = refreshResp.body()!!.accessToken
                val newRefresh = refreshResp.body()!!.refreshToken ?: refreshToken

                runBlocking { dataStore.saveTokens(newAccess, newRefresh) }
                Log.d(TAG, "Token refreshed successfully")

                // Retry request với token mới
                val newRequest = originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer $newAccess")
                    .build()

                return chain.proceed(newRequest)
            } else {
                // 6.4) ❌ Refresh thất bại => logout
                Log.e(TAG, "Refresh failed. Force logout.")
                handleLogout()
                return response
            }
        }
    }

    private fun handleLogout() {
        runBlocking {
            dataStore.clearTokens()
            Log.d(TAG, "Tokens cleared")
        }

        // Gọi callback để navigate về login
        logoutCallback?.invoke()
    }
}