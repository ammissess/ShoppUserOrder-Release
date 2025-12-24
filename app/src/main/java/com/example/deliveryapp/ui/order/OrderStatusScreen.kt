package com.example.deliveryapp.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.deliveryapp.utils.Resource
import kotlinx.coroutines.launch

@Composable
fun OrderStatusScreen(
    orderId: Long,
    navController: NavController,
    viewModel: OrderViewModel = hiltViewModel()
) {
    val state by viewModel.orderDetail.collectAsState()
    val isChatEnabled by viewModel.isChatEnabled.collectAsState()
    val coroutineScope = rememberCoroutineScope()


    // Khi vào màn hình: tải chi tiết đơn hàng
    LaunchedEffect(orderId) {
        viewModel.loadOrderDetail(orderId)
    }

    LaunchedEffect(orderId) {
        viewModel.pollStatus(orderId) { status ->
            when (status) {
                "received" -> {
                    val shipperId = viewModel.getCurrentShipperId()
                    if (shipperId != null) {
                        launch {
                            val token = viewModel.getToken()
                            viewModel.openChat(
                                orderId = orderId,
                                shipperId = shipperId,
                                shipperName = "Shipper",
                                token = token
                            )
                        }
                    }
                }

                "completed" -> {
                    viewModel.onOrderCompleted()
                }
            }
        }
    }




    when (state) {
        is Resource.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is Resource.Error -> {
            Text((state as Resource.Error).message ?: "Lỗi khi tải đơn hàng")
        }


        is Resource.Success -> {
            val dto = state.data!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Text("Đơn hàng #${dto.id} - ${dto.order_status}")
                Spacer(modifier = Modifier.height(8.dp))
                Spacer(modifier = Modifier.height(16.dp))

                if (isChatEnabled) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Chat với Shipper")
                    }
                }
            }
        }

    }
}
