// ui/product/ProductDetailReviewScreen.kt
package com.example.deliveryapp.ui.product

import android.R.attr.category
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.deliveryapp.data.remote.dto.ReviewResponseDto
import com.example.deliveryapp.ui.home.HomeViewModel
import com.example.deliveryapp.ui.navigation.Screen
import com.example.deliveryapp.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailReviewScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    navController: NavController,
    productId: Long,
    orderId: Long,
    viewModel: ProductReviewViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(productId) {
        viewModel.loadProductAndReviews(productId)
        //viewModel.loadReviews(productId)
        viewModel.checkIfAlreadyReviewed(productId, orderId)
    }

    val productState by viewModel.product.collectAsState()
    val reviewsState by viewModel.reviews.collectAsState()
    val submitState by viewModel.submitReviewResult.collectAsState()
    val hasReviewed by viewModel.hasUserReviewed.collectAsState()

    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showThankDialog by remember { mutableStateOf(false) }


    // Xử lý kết quả submit
    LaunchedEffect(submitState) {
        when (submitState) {
            is Resource.Success -> {
                showReviewDialog = false
                viewModel.resetSubmitState()
                showThankDialog = true // ✅ bật dialog cảm ơn
            }
            is Resource.Error -> {
                Toast.makeText(
                    context,
                    (submitState as Resource.Error).message ?: "Lỗi gửi đánh giá",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetSubmitState()
            }
            else -> {}
        }
    }

    if (showThankDialog) {
        AlertDialog(
            onDismissRequest = { showThankDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showThankDialog = false
                        // ✅ Quay về Home và refresh
                        homeViewModel.fetchProducts()
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    }
                ) {
                    Text("OK")
                }
            },
            title = { Text("Cảm ơn bạn!") },
            text = { Text("Đánh giá của bạn đã được gửi thành công.") }
        )
    }


    // Dialog đánh giá
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Đánh giá sản phẩm") },
            text = {
                Column {
                    Text("Chọn số sao:")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.Center) {
                        for (i in 1..5) {
                            IconButton(onClick = { rating = i }) {
                                Icon(
                                    if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "$i sao",
                                    tint = if (i <= rating) Color(0xFFFFD700) else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Nhập nội dung đánh giá:")
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Chia sẻ trải nghiệm của bạn...") },
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (comment.isBlank()) {
                            Toast.makeText(context, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.submitReview(navController, productId, orderId, rating, comment)
                        }
                    },
                    enabled = submitState !is Resource.Loading
                ) {
                    if (submitState is Resource.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Gửi đánh giá")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết sản phẩm") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = productState) {
            is Resource.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Lỗi: ${state.message}")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProductAndReviews(productId) }) {
                            Text("Thử lại")
                        }
                    }
                }
            }
            is Resource.Success -> {
                val product = state.data ?: return@Scaffold

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Ảnh sản phẩm
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = product.images.firstOrNull()?.url ?: "",
                                contentDescription = product.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }

                    // Thông tin sản phẩm
                    item {
                        Column {
                            Text(product.name, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${product.price} đ",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(product.avg_rate.toInt()) {
                                    Icon(Icons.Filled.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                                }
                                repeat(5 - product.avg_rate.toInt()) {
                                    Icon(Icons.Outlined.StarOutline, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("(${product.review_count} đánh giá)")
                            }
                        }
                    }

                    // Nút đánh giá
                    item {
                        if (!hasReviewed) {
                            Button(
                                onClick = { showReviewDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Filled.Star, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Viết đánh giá")
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✅ Bạn đã đánh giá sản phẩm này")
                                }
                            }
                        }
                    }

                    // Danh sách đánh giá
                    item {
                        Divider()
                        Text("Đánh giá sản phẩm", style = MaterialTheme.typography.titleLarge)
                    }

                    when (val reviewState = reviewsState) {
                        is Resource.Loading -> {
                            item { CircularProgressIndicator() }
                        }
                        is Resource.Error -> {
                            item { Text("Lỗi tải review: ${reviewState.message}") }
                        }
                        is Resource.Success -> {
                            val reviews = reviewState.data ?: emptyList()
                            if (reviews.isEmpty()) {
                                item { Text("Chưa có đánh giá nào") }
                            } else {
                                items(reviews) { review ->
                                    ReviewItemCard(review)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItemCard(review: ReviewResponseDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(review.rate) {
                    Icon(
                        Icons.Filled.Star,
                        null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(18.dp)
                    )
                }
                repeat(5 - review.rate) {
                    Icon(
                        Icons.Outlined.StarOutline,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    review.user_name,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(review.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                review.created_at,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}