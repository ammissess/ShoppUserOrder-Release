package com.example.deliveryapp.ui.order

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.deliveryapp.ui.home.formatPrice
import com.example.deliveryapp.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    navController: NavController,
    orderId: Long,
    viewModel: OrderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showCancelDialog by remember { mutableStateOf(false) }

    // Tải thông tin đơn hàng
    LaunchedEffect(orderId) {
        viewModel.loadOrderDetail(orderId)
    }

    val orderDetail by viewModel.orderDetail.collectAsState()
    val cancelOrderResult by viewModel.cancelOrderResult.collectAsState()

    // Logic xử lý kết quả hủy đơn
    LaunchedEffect(cancelOrderResult) {
        cancelOrderResult?.let { result ->
            when (result) {
                is Resource.Success -> {
                    Toast.makeText(
                        context,
                        result.message ?: "Đã hủy đơn hàng",
                        Toast.LENGTH_SHORT
                    ).show()

                    // CHỈ load lại, KHÔNG gọi cancelOrder nữa
                    viewModel.loadOrderDetail(orderId)
                }

                is Resource.Error -> {
                    Toast.makeText(
                        context,
                        result.message ?: "Không thể hủy đơn",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> Unit
            }
        }
    }


    // Dialog xác nhận hủy đơn
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Xác nhận hủy đơn", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn hủy đơn hàng #$orderId? Thao tác này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelOrder(orderId)
                }) {
                    Text("Xác nhận hủy", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Quay lại") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chi tiết đơn hàng #$orderId", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        when (val state = orderDetail) {
            is Resource.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Text("Lỗi: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is Resource.Success -> {
                val detail = state.data ?: return@Scaffold
                val status = detail.order_status

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8F8F8))
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // 1. TRẠNG THÁI ĐƠN HÀNG
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (status) {
                                    "cancelled" -> MaterialTheme.colorScheme.errorContainer
                                    "delivered" -> Color(0xFFE8F5E9)
                                    else -> Color.White
                                }
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                val statusText = when (status) {
                                    "pending" -> "⏳ Chờ xử lý"
                                    "processing" -> "📦 Đang chuẩn bị"
                                    "shipping" -> "🚚 Đang giao hàng"
                                    "delivered" -> "✅ Giao thành công"
                                    "cancelled" -> "❌ Đã hủy"
                                    else -> status
                                }

                                Text("Trạng thái đơn hàng", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = when (status) {
                                        "cancelled" -> MaterialTheme.colorScheme.error
                                        "delivered" -> Color(0xFF2E7D32)
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Thanh toán: ${detail.payment_status}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // 2. DANH SÁCH SẢN PHẨM
                    item {
                        Text("Sản phẩm đã chọn", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    items(detail.items) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = item.product_image,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.product_name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Text("x${item.quantity}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Text(formatPrice(item.subtotal), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }

                                if (status == "delivered") {
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { navController.navigate("product_detail_review/${item.product_id}/$orderId") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Đánh giá sản phẩm")
                                    }
                                }
                            }
                        }
                    }

                    // 3. TỔNG TIỀN
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tổng cộng", fontWeight = FontWeight.Bold)
                                Text(formatPrice(detail.total_amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // 4. NÚT ĐIỀU KHIỂN
                    item {
                        Spacer(Modifier.height(8.dp))
                        val isCancelling = cancelOrderResult is Resource.Loading

                        if (status == "pending") {
                            Button(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isCancelling,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                if (isCancelling) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("🗑️ Hủy đơn hàng", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (status == "shipping") {
                            Button(
                                onClick = { navController.navigate("messages/${detail.id}") },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("💬 Chat với Shipper", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = when (status) {
                                        "cancelled" -> "ℹ️ Đơn hàng này đã bị hủy."
                                        "processing" -> "📦 Nhà hàng đang chuẩn bị món ăn cho bạn."
                                        "delivered" -> "✅ Đơn hàng đã hoàn tất."
                                        else -> ""
                                    },
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}