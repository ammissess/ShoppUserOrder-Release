package com.example.deliveryapp.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.dto.OrderDetailDto
import com.example.deliveryapp.data.remote.dto.PlaceOrderRequestDto
import com.example.deliveryapp.domain.usecase.CancelOrderUseCase
import com.example.deliveryapp.domain.usecase.GetOrderDetailUseCase
import com.example.deliveryapp.domain.usecase.PlaceOrderUseCase
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn


@HiltViewModel
class OrderViewModel @Inject constructor(
    private val placeOrderUseCase: PlaceOrderUseCase,
    private val getOrderDetailUseCase: GetOrderDetailUseCase,
    private val dataStore: DataStoreManager,
    private val cancelOrderUseCase: CancelOrderUseCase, // ✅ Thêm UseCase huy don
) : ViewModel() {

    //huy don
    private val _cancelOrderResult = MutableStateFlow<Resource<String>?>(null)
    val cancelOrderResult: StateFlow<Resource<String>?> = _cancelOrderResult


    private val _placeOrderResult = MutableStateFlow<Resource<String>>(Resource.Loading())
    val placeOrderResult: StateFlow<Resource<String>> = _placeOrderResult

    private val _orderDetail = MutableStateFlow<Resource<OrderDetailDto>>(Resource.Loading())
    val orderDetail: StateFlow<Resource<OrderDetailDto>> = _orderDetail

    private val _isChatEnabled = MutableStateFlow(false)
    val isChatEnabled: StateFlow<Boolean> = _isChatEnabled

    private var currentShipperId: Long? = null
    private var pollingActive = false


    val shipperName: StateFlow<String?> =
        _orderDetail
            .map { resource ->
                when (resource) {
                    is Resource.Success -> resource.data?.shipper_info?.name
                    else -> null
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                null
            )

    fun placeOrder(req: PlaceOrderRequestDto) {
        viewModelScope.launch {
            _placeOrderResult.value = placeOrderUseCase(req)
        }
    }

    fun getCurrentShipperId(): Long? = currentShipperId

    fun loadOrderDetail(id: Long) {
        viewModelScope.launch {
            val result = getOrderDetailUseCase(id)
            _orderDetail.value = result

            if (result is Resource.Success) {
                currentShipperId = result.data?.shipper_id
            }
        }
    }

    //huy don hang
    fun cancelOrder(orderId: Long) {
        viewModelScope.launch {
            _cancelOrderResult.value = Resource.Loading()
            val result = cancelOrderUseCase(orderId)
            _cancelOrderResult.value = result

            // Nếu hủy thành công, load lại chi tiết đơn
            if (result is Resource.Success) {
                delay(500) // Delay nhỏ để backend cập nhật
                loadOrderDetail(orderId)
            }
        }
    }
    /**
     * Polling trạng thái đơn hàng mỗi 5 giây, gọi callback khi thay đổi
     */


    fun pollStatus(
        orderId: Long,
        onStatusChange: (status: String) -> Unit
    ) {
        viewModelScope.launch {
            pollingActive = true
            var lastStatus: String? = null

            while (pollingActive) {
                val result = getOrderDetailUseCase(orderId)

                if (result is Resource.Success) {
                    val dto = result.data ?: continue

                    val newStatus = dto.order_status
                    currentShipperId = dto.shipper_id   // ✅ QUAN TRỌNG

                    if (newStatus != lastStatus) {
                        lastStatus = newStatus
                        onStatusChange(newStatus)
                    }
                }

                delay(5000)
            }
        }
    }



    /**
     * Lưu trạng thái mở chat
     */
    fun openChat(orderId: Long, shipperId: Long, shipperName: String, token: String) {
        viewModelScope.launch {
            _isChatEnabled.value = true
            // ChatScreen sẽ tự xử lý initChat() với token và shipperId
        }
    }

    /**
     * Khi đơn hoàn thành => tắt polling và đóng chat
     */
    fun onOrderCompleted() {
        pollingActive = false
        _isChatEnabled.value = false
    }

    /**
     * Lấy token từ DataStore (dùng trong LaunchedEffect)
     */
    suspend fun getToken(): String {
        return dataStore.accessToken.firstOrNull().orEmpty()
    }
}
