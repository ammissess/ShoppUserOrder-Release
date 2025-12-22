package com.example.deliveryapp.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.remote.dto.OrderDetailDto
import com.example.deliveryapp.domain.usecase.GetOrderDetailUseCase
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(private val getOrderDetail: GetOrderDetailUseCase) : ViewModel() {

    private val _orderLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val orderLocation: StateFlow<Pair<Double, Double>?> = _orderLocation

    private val _orderStatus = MutableStateFlow<Resource<OrderDetailDto>>(Resource.Loading())
    val orderStatus: StateFlow<Resource<OrderDetailDto>> = _orderStatus

    fun startTracking(orderId: Long) {
        viewModelScope.launch {
            while (isActive) {
                loadOrderDetail(orderId)
                delay(5000) // Poll every 5s
            }
        }
    }

    private suspend fun loadOrderDetail(id: Long) {
        _orderStatus.value = getOrderDetail(id)
        if (_orderStatus.value is Resource.Success<OrderDetailDto>) {
            val dto = (_orderStatus.value as Resource.Success<OrderDetailDto>).data!!
            //_orderLocation.value = Pair(dto.latitude, dto.longitude)
            // logic location vẽ map?
            _orderLocation.value = null
        }
    }
}