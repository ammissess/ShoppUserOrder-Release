package com.example.deliveryapp.ui.product

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.deliveryapp.data.remote.dto.ProductDto
import com.example.deliveryapp.data.remote.dto.ProductImageDto
import com.example.deliveryapp.data.remote.dto.ReviewResponseDto
import com.example.deliveryapp.utils.Resource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: Long,
    viewModel: ProductViewModel = hiltViewModel()
) {
    LaunchedEffect(productId) { viewModel.loadProduct(productId) }

    val productState by viewModel.product.collectAsState()
    val reviewState by viewModel.reviews.collectAsState()

    when (productState) {
        is Resource.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        is Resource.Error -> Text((productState as Resource.Error).message ?: "Error")
        is Resource.Success -> {
            val product = (productState as Resource.Success<ProductDto>).data ?: return

            Scaffold(
                topBar = {
                    // ✅ TopAppBar có nút back
                    TopAppBar(
                        title = { Text(product.name) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Quay lại"
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Ảnh gallery
                    item { ProductImageGallery(product.images) }

                    // Tên + giá + sao
                    item {
                        Column {
                            Text(product.name, style = MaterialTheme.typography.headlineSmall)
                            Text("${product.price} đ", style = MaterialTheme.typography.titleMedium)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(product.avgRate.toInt()) {
                                    Icon(Icons.Default.Star, null, tint = Color.Yellow)
                                }
                                repeat(5 - product.avgRate.toInt()) {
                                    Icon(Icons.Default.Star, null, tint = Color.Gray)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("(${product.reviewCount} lượt đánh giá)")
                            }
                        }
                    }

                    // Description
                    if (!product.description.isNullOrBlank()) {
                        item { Text(product.description ?: "") }
                    }

                    // Nút đặt hàng
                    item {
                        Button(
                            onClick = { /* TODO: Navigate tới đặt hàng */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Đặt hàng")
                        }
                    }

                    // Reviews
                    item {
                        Divider()
                        Text("Đánh giá sản phẩm", style = MaterialTheme.typography.titleLarge)
                    }

                    when (reviewState) {
                        is Resource.Loading -> {
                            item { CircularProgressIndicator() }
                        }
                        is Resource.Error -> {
                            item { Text("Lỗi tải review") }
                        }
                        is Resource.Success -> {
                            val reviews =
                                (reviewState as Resource.Success<List<ReviewResponseDto>>).data ?: emptyList()
                            if (reviews.isEmpty()) {
                                item { Text("Chưa có đánh giá nào") }
                            } else {
                                items(reviews) { rev -> ReviewItem(rev) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductImageGallery(images: List<ProductImageDto>) {
    if (images.isEmpty()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "https://via.placeholder.com/300?text=No+Image",
                contentDescription = "No image",
                modifier = Modifier.size(200.dp)
            )
        }
    } else {
        val pagerState = rememberPagerState(pageCount = { images.size })
        Column {
            HorizontalPager(state = pagerState) { page ->
                val image = images[page]
                AsyncImage(
                   // model = image.url.ifEmpty { "https://via.placeholder.com/300" },
                    model = image.url?.takeIf { it.isNotBlank() }
                        ?: "https://via.placeholder.com/300?text=No+Image",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    error = painterResource(id = android.R.drawable.ic_menu_gallery)
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(images.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                    if (index < images.lastIndex) Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
fun ReviewItem(review: ReviewResponseDto) {
    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(review.rate) {
                Icon(Icons.Default.Star, null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
            }
            repeat(5 - review.rate) {
                Icon(Icons.Default.Star, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(review.user_name, style = MaterialTheme.typography.labelMedium)
        }
        Text(review.content, style = MaterialTheme.typography.bodyMedium)
        Divider()
    }
}