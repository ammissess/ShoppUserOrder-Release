package com.example.deliveryapp.ui.order

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.deliveryapp.ui.home.CartItem
import com.example.deliveryapp.ui.home.formatPrice
import com.example.deliveryapp.utils.Resource
import kotlinx.coroutines.delay

private const val TAG = "CheckoutDebug"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    // 1. Lấy dữ liệu từ màn hình trước và ViewModel
    val cart = navController.previousBackStackEntry?.savedStateHandle?.get<List<CartItem>>("checkout_cart") ?: emptyList()
    val profileState by viewModel.profileState.collectAsState()
    val confirmState by viewModel.confirmOrderState.collectAsState()
    val deliveryInfo by viewModel.deliveryInfo.collectAsState()

    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val savedStateHandle = navBackStackEntry?.savedStateHandle
    val lifecycleOwner = LocalLifecycleOwner.current

    var paymentMethod by remember { mutableStateOf("unpaid") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Tính toán tiền
    val subTotal = remember(cart) { cart.sumOf { it.product.price * it.quantity } }
    val shippingFee = 00000.0
    val totalAmount = subTotal + shippingFee

    // 2. LOGIC: Nhận địa chỉ từ bản đồ gửi về
    LaunchedEffect(savedStateHandle) {
        navBackStackEntry?.savedStateHandle?.let { handle ->
            handle.getLiveData<Double>("selectedLat").observe(lifecycleOwner) { lat ->
                val lng = handle.get<Double>("selectedLng")
                val address = handle.get<String>("selectedAddress")

                if (lat != null && lng != null && address != null) {
                    viewModel.updateDeliveryAddress(lat, lng, address)
                    // Xóa dữ liệu tạm để tránh lặp logic
                    handle.remove<Double>("selectedLat")
                    handle.remove<Double>("selectedLng")
                    handle.remove<String>("selectedAddress")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
        viewModel.resetConfirmState()
    }

    // Xử lý khi đặt hàng thành công
    LaunchedEffect(confirmState) {
        if (confirmState is Resource.Success && (confirmState as Resource.Success).data?.isNotEmpty() == true) {
            showSuccessDialog = true
            delay(2000)
            navController.previousBackStackEntry?.savedStateHandle?.set("clear_cart", true)
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Xác nhận đơn hàng", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // THANH THANH TOÁN CỐ ĐỊNH PHÍA DƯỚI
            Surface(tonalElevation = 8.dp, shadowElevation = 16.dp, color = Color.White) {
                Column(modifier = Modifier.navigationBarsPadding().padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tổng cộng", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            formatPrice(totalAmount),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 1. Kiểm tra trạng thái đơn hàng (Đang xử lý hoặc Đã thành công)
                    val isOrderProcessed = confirmState is Resource.Success && !confirmState.data.isNullOrEmpty()
                    val isLoading = confirmState is Resource.Loading

                    Button(
                        onClick = { viewModel.confirmOrder(cart, paymentMethod) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        // ✅ CẬP NHẬT ĐIỀU KIỆN ENABLED
                        enabled = !isLoading && // Không cho nhấn khi đang load [cite: 128]
                                !isOrderProcessed && // Không cho nhấn khi đã đặt hàng thành công
                                deliveryInfo.latitude != null && // Bắt buộc có tọa độ [cite: 128]
                                deliveryInfo.latitude != 0.0 &&
                                !deliveryInfo.address.isNullOrBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            disabledContainerColor = Color.Gray, // Màu xám khi bị vô hiệu hóa
                            disabledContentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when {
                                    isOrderProcessed -> "ĐANG ĐẶT HÀNG"
                                    deliveryInfo.latitude == null || deliveryInfo.latitude == 0.0 -> "Cần chọn vị trí trên bản đồ"
                                    deliveryInfo.address.isNullOrBlank() -> "Địa chỉ không hợp lệ"
                                    else -> "ĐẶT HÀNG"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 3. THÔNG TIN NHẬN HÀNG
            SectionHeader(title = "Địa chỉ nhận hàng")
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = deliveryInfo.name ?: (profileState as? Resource.Success)?.data?.name ?: "Người nhận",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = deliveryInfo.phone ?: (profileState as? Resource.Success)?.data?.phone ?: "Chưa có SĐT",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }

                    Divider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                    // HIỂN THỊ ĐỊA CHỈ VÀ TỌA ĐỘ
                    if (deliveryInfo.address.isNullOrEmpty()) {
                        OutlinedButton(
                            onClick = { navController.navigate("location_picker") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(imageVector = Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Chọn vị trí trên bản đồ")
                        }
                    } else {
                        Column {
                            Text(text = deliveryInfo.address!!, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)

                            // Dòng tọa độ hiển thị trên Card
                            Text(
                                text = "Tọa độ: ${String.format("%.5f", deliveryInfo.latitude)}, ${String.format("%.5f", deliveryInfo.longitude)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            TextButton(
                                onClick = { navController.navigate("location_picker") },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Thay đổi địa chỉ", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // 4. DANH SÁCH SẢN PHẨM
            SectionHeader(title = "Sản phẩm")
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    cart.forEachIndexed { index, item ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(shape = RoundedCornerShape(8.dp)) {
                                AsyncImage(
                                    model = item.product.images.firstOrNull()?.url,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.product.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Số lượng: ${item.quantity}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text(formatPrice(item.product.price * item.quantity), fontWeight = FontWeight.Medium)
                        }
                        if (index < cart.size - 1) Divider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
                    }
                }
            }

            // 5. THANH TOÁN
            SectionHeader(title = "Phương thức thanh toán")
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(12.dp))
                    Text("Thanh toán khi nhận hàng (COD)", modifier = Modifier.weight(1f))
                    RadioButton(selected = paymentMethod == "unpaid", onClick = { paymentMethod = "unpaid" })
                }
            }

            // 6. CHI TIẾT GIÁ
            SectionHeader(title = "Chi tiết thanh toán")
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                PriceRow("Tạm tính", formatPrice(subTotal))
                PriceRow("Phí vận chuyển", formatPrice(shippingFee))
                PriceRow("Tổng cộng", formatPrice(totalAmount), isBold = true)
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    // Dialog đặt hàng thành công
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp)) },
            title = { Text(text = "Đặt hàng thành công!", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Đơn hàng của bạn đã được xác nhận", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(text = "Cảm ơn bạn đã tin tưởng sử dụng dịch vụ!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.previousBackStackEntry?.savedStateHandle?.set("clear_cart", true)
                        navController.navigate("home") { popUpTo("home") { inclusive = true } }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Về trang chủ")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = Color.Gray,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun PriceRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = if (isBold) Color.Black else Color.Gray)
        Text(
            value,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Normal,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        )
    }
}