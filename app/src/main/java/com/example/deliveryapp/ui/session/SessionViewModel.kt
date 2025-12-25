//package com.example.deliveryapp.ui.session
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.deliveryapp.data.local.DataStoreManager
//import com.example.deliveryapp.data.remote.api.AuthApi
//import com.example.deliveryapp.data.remote.dto.RefreshTokenRequestDto
//import com.example.deliveryapp.data.repository.AuthRepository
//import com.example.deliveryapp.di.NormalAuthApi
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//import kotlinx.coroutines.flow.MutableSharedFlow
//import kotlinx.coroutines.flow.SharedFlow
//import kotlinx.coroutines.flow.asSharedFlow
//
//private const val TAG = "SessionViewModel"
//private const val TOKEN_CHECK_INTERVAL = 60_000L // 1 phút kiểm tra một lần
//
//@HiltViewModel
//class SessionViewModel @Inject constructor(
//    private val dataStore: DataStoreManager,
//    private val authRepository: AuthRepository,
//    @NormalAuthApi private val authApi: AuthApi
//) : ViewModel() {
//
//    // StateFlow kiểm tra nếu có token -> đã đăng nhập
//    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
//    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn
//
//    // StateFlow cho trạng thái token
//    private val _tokenState = MutableStateFlow<TokenState>(TokenState.Unknown)
//    val tokenState: StateFlow<TokenState> = _tokenState
//
//    // Event thông báo session hết hạn (one-time event)
//    private val _sessionExpired = MutableSharedFlow<Unit>()
//    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()
//
//
//
//    init {
//        // Kiểm tra token ban đầu
//        checkLoginStatus()
//
//        // Bắt đầu kiểm tra token định kỳ
//        startTokenRefreshChecker()
//    }
//
//    private fun checkLoginStatus() {
//        viewModelScope.launch {
//            dataStore.accessToken.collect { token ->
//                Log.d(TAG, "Current token: ${token?.take(10)}")
//                _isLoggedIn.value = !token.isNullOrEmpty()
//
//                if (!token.isNullOrEmpty()) {
//                    // Kiểm tra tính hợp lệ của token ngay khi có token
//                    checkTokenValidity()
//                } else {
//                    _tokenState.value = TokenState.Invalid
//                }
//            }
//        }
//    }
//
//    private fun startTokenRefreshChecker() {
//        viewModelScope.launch {
//            while (true) {
//                delay(TOKEN_CHECK_INTERVAL)
//
//                // Chỉ kiểm tra nếu người dùng đã đăng nhập
//                if (_isLoggedIn.value == true) {
//                    checkTokenValidity()
//                }
//            }
//        }
//    }
//
//    // Phương thức mới để kiểm tra tính hợp lệ của access token
//    private suspend fun checkTokenValidity() {
//        try {
//            // Thử gọi API profile để kiểm tra access token
//            val profileResult = authRepository.getProfile()
//
//            if (profileResult is com.example.deliveryapp.utils.Resource.Success) {
//                // Access token vẫn hợp lệ
//                Log.d(TAG, "Access token is valid")
//                _tokenState.value = TokenState.Valid
//            } else {
//                // Access token có thể đã hết hạn, thử refresh
//                Log.d(TAG, "Access token may be expired, trying to refresh")
//                refreshTokenIfNeeded()
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error checking token validity: ${e.message}", e)
//            // Có lỗi khi kiểm tra, thử refresh
//            refreshTokenIfNeeded()
//        }
//    }
//
////    private suspend fun refreshTokenIfNeeded() {
////        try {
////            // Lấy refresh token hiện tại
////            val refreshToken = dataStore.refreshToken.first()
////            if (refreshToken.isNullOrEmpty()) {
////                Log.d(TAG, "No refresh token available")
////                _tokenState.value = TokenState.Invalid
////                _isLoggedIn.value = false
////                return
////            }
////
////            // Thử refresh token
////            val result = authRepository.refreshToken(refreshToken)
////
////            if (result is com.example.deliveryapp.utils.Resource.Success) {
////                // Token đã được refresh thành công
////                Log.d(TAG, "Token refreshed successfully")
////                _tokenState.value = TokenState.Valid
////                _isLoggedIn.value = true
////            } else {
////                // Token không hợp lệ hoặc hết hạn
////                Log.e(TAG, "Token refresh failed: ${(result as? com.example.deliveryapp.utils.Resource.Error)?.message}")
////                _tokenState.value = TokenState.Invalid
////                _isLoggedIn.value = false
////
////                // Đăng xuất người dùng
////                authRepository.logout()
////            }
////        } catch (e: Exception) {
////            Log.e(TAG, "Error refreshing token: ${e.message}", e)
////            // Giữ nguyên trạng thái hiện tại, không đăng xuất người dùng
////        }
////    }
//
//    private suspend fun refreshTokenIfNeeded() {
//        try {
//            val refreshToken = dataStore.refreshToken.first()
//
//            if (refreshToken.isNullOrEmpty()) {
//                _tokenState.value = TokenState.Invalid
//                _isLoggedIn.value = false
//
//                notifySessionExpired()
//                return
//            }
//
//            val result = authRepository.refreshToken(refreshToken)
//
//            if (result is com.example.deliveryapp.utils.Resource.Success) {
//                _tokenState.value = TokenState.Valid
//                _isLoggedIn.value = true
//            } else {
//                _tokenState.value = TokenState.Invalid
//                _isLoggedIn.value = false
//
//                authRepository.logout()
//                notifySessionExpired()
//            }
//
//        } catch (e: Exception) {
//            notifySessionExpired()
//        }
//    }
//
//    private suspend fun notifySessionExpired() {
//        _sessionExpired.emit(Unit)
//    }
//
//    fun notifySessionExpiredFromInterceptor() {
//        viewModelScope.launch {
//            _sessionExpired.emit(Unit)
//        }
//    }
//
//
//    fun logout() {
//        viewModelScope.launch {
//            authRepository.logout()
//            _isLoggedIn.value = false
//            _tokenState.value = TokenState.Invalid
//            notifySessionExpired()
//        }
//    }
//
//
//
//    // Phương thức để kiểm tra token theo yêu cầu (có thể gọi từ UI)
//    fun checkToken() {
//        viewModelScope.launch {
//            checkTokenValidity()
//        }
//    }
//
////    // Phương thức để đăng xuất
////    fun logout() {
////        viewModelScope.launch {
////            authRepository.logout()
////            _isLoggedIn.value = false
////            _tokenState.value = TokenState.Invalid
////        }
////    }
//}
//
//// Enum class để biểu diễn trạng thái token
//sealed class TokenState {
//    object Unknown : TokenState()
//    object Valid : TokenState()
//    object Invalid : TokenState()
//}

package com.example.deliveryapp.ui.session

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.api.AuthApi
import com.example.deliveryapp.data.remote.dto.RefreshTokenRequestDto
import com.example.deliveryapp.data.repository.AuthRepository
import com.example.deliveryapp.di.NormalAuthApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SessionViewModel"

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val dataStore: DataStoreManager,
    private val authRepository: AuthRepository,
    @NormalAuthApi private val authApi: AuthApi
) : ViewModel() {

    // StateFlow kiểm tra nếu có token -> đã đăng nhập
    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn

    // Event thông báo session hết hạn (one-time event)
    private val _sessionExpired = MutableSharedFlow<Unit>()
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    init {
        // ✅ Chỉ kiểm tra token khi khởi động app
        checkLoginStatusOnStartup()
    }

    /**
     * Kiểm tra trạng thái đăng nhập khi app khởi động
     * - Nếu có token → thử refresh để kiểm tra tính hợp lệ
     * - Nếu không có token → chưa đăng nhập
     */
    private fun checkLoginStatusOnStartup() {
        viewModelScope.launch {
            dataStore.accessToken.collect { accessToken ->
                Log.d(TAG, "Current token on startup: ${accessToken?.take(10)}")

                if (accessToken.isNullOrEmpty()) {
                    // Không có token → chưa đăng nhập
                    _isLoggedIn.value = false
                } else {
                    // Có token → kiểm tra tính hợp lệ
                    validateTokenOnStartup()
                }
            }
        }
    }

    /**
     * Kiểm tra token khi khởi động app
     * - Gọi API profile để test access token
     * - Nếu thất bại → thử refresh token
     */
    private suspend fun validateTokenOnStartup() {
        try {
            // Thử gọi API profile
            val profileResult = authRepository.getProfile()

            if (profileResult is com.example.deliveryapp.utils.Resource.Success) {
                // ✅ Token hợp lệ
                Log.d(TAG, "Token valid on startup")
                _isLoggedIn.value = true
            } else {
                // ❌ Token hết hạn → thử refresh
                Log.d(TAG, "Token expired on startup, trying refresh")
                attemptRefreshOnStartup()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating token: ${e.message}", e)
            attemptRefreshOnStartup()
        }
    }

    /**
     * Thử refresh token khi khởi động
     * - Nếu thành công → đăng nhập
     * - Nếu thất bại → yêu cầu đăng nhập lại
     */
    private suspend fun attemptRefreshOnStartup() {
        try {
            val refreshToken = dataStore.refreshToken.first()

            if (refreshToken.isNullOrEmpty()) {
                Log.d(TAG, "No refresh token available")
                _isLoggedIn.value = false
                return
            }

            // Thử refresh token
            val result = authRepository.refreshToken(refreshToken)

            if (result is com.example.deliveryapp.utils.Resource.Success) {
                // ✅ Refresh thành công
                Log.d(TAG, "Token refreshed successfully on startup")
                _isLoggedIn.value = true
            } else {
                // ❌ Refresh thất bại → đăng xuất
                Log.e(TAG, "Refresh failed on startup")
                _isLoggedIn.value = false
                authRepository.logout()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token on startup: ${e.message}", e)
            _isLoggedIn.value = false
        }
    }

    /**
     * Được gọi từ AuthInterceptor khi refresh token thất bại
     */
    fun notifySessionExpiredFromInterceptor() {
        viewModelScope.launch {
            Log.d(TAG, "Session expired notification from interceptor")
            _isLoggedIn.value = false
            _sessionExpired.emit(Unit)
        }
    }

    /**
     * Đăng xuất thủ công (do người dùng)
     */
    fun logout() {
        viewModelScope.launch {
            Log.d(TAG, "Manual logout")
            authRepository.logout()
            _isLoggedIn.value = false
            _sessionExpired.emit(Unit)
        }
    }
}

// Enum class để biểu diễn trạng thái token (không còn cần thiết)
sealed class TokenState {
    object Unknown : TokenState()
    object Valid : TokenState()
    object Invalid : TokenState()
}
