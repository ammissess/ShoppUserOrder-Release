package com.example.deliveryapp.ui.order

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.dto.OrderProductDto
import com.example.deliveryapp.data.remote.dto.PlaceOrderRequestDto
import com.example.deliveryapp.data.remote.dto.ProfileDto
import com.example.deliveryapp.data.repository.AuthRepository
import com.example.deliveryapp.data.repository.OrderRepository
import com.example.deliveryapp.ui.home.CartItem
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CheckoutViewModel"

data class DeliveryInfo(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val name: String? = null,
    val phone: String? = null
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _profileState = MutableStateFlow<Resource<ProfileDto>>(Resource.Loading())
    val profileState: StateFlow<Resource<ProfileDto>> = _profileState

    private val _confirmOrderState = MutableStateFlow<Resource<String>>(Resource.Success(""))
    val confirmOrderState: StateFlow<Resource<String>> = _confirmOrderState

    private val _deliveryInfo = MutableStateFlow(DeliveryInfo())
    val deliveryInfo: StateFlow<DeliveryInfo> = _deliveryInfo

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState

    data class CheckoutUiState(
        val deliveryLat: Double? = null,
        val deliveryLng: Double? = null,
        val deliveryAddress: String? = null
    )

    fun placeOrder() {
        // Thực hiện logic đặt hàng ở đây
        println("Đặt hàng tại: ${_uiState.value.deliveryAddress} (${_uiState.value.deliveryLat}, ${_uiState.value.deliveryLng})")
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = authRepository.getProfile()

            // Tự động load thông tin giao hàng từ profile
            if (_profileState.value is Resource.Success) {
                val profile = (_profileState.value as Resource.Success).data
                profile?.let {
                    // Lấy lat/lng từ DataStore
                    val savedLat = dataStore.latitude.first()
                    val savedLng = dataStore.longitude.first()
                    Log.d(TAG, "Loaded profile: name=${it.name}, address=${it.address}, savedLat=$savedLat, savedLng=$savedLng")

                    // ✅ Lấy giá trị hiện tại của deliveryInfo
                    val current = _deliveryInfo.value

                    // ✅ Chỉ cập nhật nếu chưa có dữ liệu từ map
                    _deliveryInfo.value = DeliveryInfo(
                        name = current.name ?: it.name,  // Ưu tiên name hiện tại
                        phone = current.phone ?: it.phone,  // Ưu tiên phone hiện tại
                        address = current.address,  // ✅ GIỮ NGUYÊN address từ map, KHÔNG dùng profile
                        latitude = current.latitude ?: savedLat,  // Ưu tiên tọa độ hiện tại
                        longitude = current.longitude ?: savedLng
                    )

                    Log.d(TAG, "Updated deliveryInfo after loadProfile: ${_deliveryInfo.value}")
                }
            }
        }
    }

    fun setCart(newCart: List<CartItem>) {
        _cart.value = newCart
        Log.d(TAG, "Cart set with ${newCart.size} items")
    }

    fun updateDeliveryAddress(lat: Double, lng: Double, address: String) {
        Log.d(TAG, "📥 updateDeliveryAddress called: lat=$lat, lng=$lng, address=$address")

        val current = _deliveryInfo.value
        _deliveryInfo.value = current.copy(
            latitude = lat,
            longitude = lng,
            address = address // ✅ Đảm bảo address được cập nhật
        )

        Log.d(TAG, "✅ Updated deliveryInfo: ${_deliveryInfo.value}")

        // Lưu vào DataStore
        viewModelScope.launch {
            dataStore.saveLocation(lat, lng)
            Log.d(TAG, "💾 Saved to DataStore")
        }
    }

    fun updateReceiverInfo(name: String, phone: String) {
        Log.d(TAG, "Update receiver info: name=$name, phone=$phone")
        val current = _deliveryInfo.value
        _deliveryInfo.value = current.copy(
            name = name,
            phone = phone
        )
    }

//    fun confirmOrder(cart: List<CartItem>, paymentMethod: String) {
//        viewModelScope.launch {
//            _confirmOrderState.value = Resource.Loading()
//
//            try {
//                val deliveryInfo = _deliveryInfo.value
//
//                // Kiểm tra tọa độ
//                if (deliveryInfo.latitude == null || deliveryInfo.longitude == null) {
//                    _confirmOrderState.value = Resource.Error("Vui lòng chọn địa chỉ giao hàng")
//                    return@launch
//                }
//
//                // Kiểm tra thông tin người nhận
//                if (deliveryInfo.name.isNullOrBlank() || deliveryInfo.phone.isNullOrBlank()) {
//                    _confirmOrderState.value = Resource.Error("Vui lòng điền đầy đủ thông tin người nhận")
//                    return@launch
//                }
//
//                val refreshToken = dataStore.refreshToken.first()
//                if (refreshToken.isNullOrEmpty()) {
//                    _confirmOrderState.value = Resource.Error("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại")
//                    return@launch
//                }
//
//                val products = cart.map {
//                    OrderProductDto(
//                        product_id = it.product.id,
//                        quantity = it.quantity.toLong()
//                    )
//                }
//
//                val request = PlaceOrderRequestDto(
//                    latitude = deliveryInfo.latitude,
//                    longitude = deliveryInfo.longitude,
//                    products = products
//                )
//
//                Log.d(TAG, "Calling placeOrderWithRefreshToken with lat=${deliveryInfo.latitude}, lng=${deliveryInfo.longitude}")
//                //val result = orderRepository.placeOrderWithRefreshToken(request, refreshToken)
//
////                if (result is Resource.Error) {
////                    Log.e(TAG, "Order failed: ${result.message}")
////
////                    if (result.message?.contains("401") == true ||
////                        result.message?.contains("phiên") == true ||
////                        result.message?.contains("token") == true) {
////                        authRepository.logout()
////                        _confirmOrderState.value = Resource.Error("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại")
////                    } else {
////                        _confirmOrderState.value = result
////                    }
////                } else {
////                    _confirmOrderState.value = result
////
////                    if (result is Resource.Success) {
////                        Log.d(TAG, "Order placed successfully, clearing cart")
////                        _cart.value = emptyList()
////                    }
////                }
//
//
//
//            } catch (e: Exception) {
//                Log.e(TAG, "Error in confirmOrder: ${e.message}", e)
//                _confirmOrderState.value = Resource.Error(e.message ?: "Lỗi không xác định")
//            }
//        }
//    }

    fun confirmOrder(cart: List<CartItem>, paymentMethod: String) {
        viewModelScope.launch {
            _confirmOrderState.value = Resource.Loading()

            val deliveryInfo = _deliveryInfo.value

            if (deliveryInfo.latitude == null || deliveryInfo.longitude == null) {
                _confirmOrderState.value = Resource.Error("Vui lòng chọn địa chỉ giao hàng")
                return@launch
            }

            if (deliveryInfo.name.isNullOrBlank() || deliveryInfo.phone.isNullOrBlank()) {
                _confirmOrderState.value = Resource.Error("Vui lòng điền đầy đủ thông tin người nhận")
                return@launch
            }

            val products = cart.map {
                OrderProductDto(
                    product_id = it.product.id,
                    quantity = it.quantity.toLong()
                )
            }

            val request = PlaceOrderRequestDto(
                latitude = deliveryInfo.latitude,
                longitude = deliveryInfo.longitude,
                products = products
            )

            // 👇 DÁN ĐOẠN CODE Ở ĐÂY
            Log.d(
                TAG,
                "Calling placeOrder with lat=${deliveryInfo.latitude}, lng=${deliveryInfo.longitude}"
            )

            val result = orderRepository.placeOrder(request)

            _confirmOrderState.value = result

            if (result is Resource.Success) {
                Log.d(TAG, "Order placed successfully, clearing cart")
                _cart.value = emptyList()
            }
        }
    }


}