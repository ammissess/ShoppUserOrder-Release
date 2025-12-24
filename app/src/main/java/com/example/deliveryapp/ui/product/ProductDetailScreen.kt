package com.example.deliveryapp.ui.product

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.deliveryapp.data.remote.dto.ProductDto
import com.example.deliveryapp.data.remote.dto.ProductImageDto
import com.example.deliveryapp.data.remote.dto.ReviewResponseDto
import com.example.deliveryapp.ui.home.CartItem
import com.example.deliveryapp.ui.home.HomeViewModel
import com.example.deliveryapp.utils.Resource
import androidx.compose.foundation.lazy.items


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: Long,
    viewModel: ProductViewModel = hiltViewModel()
) {
    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
        viewModel.loadReviews(productId)
    }

    val productState by viewModel.product.collectAsState()
    val reviewState by viewModel.reviews.collectAsState()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val cart by homeViewModel.cart.collectAsState(initial = emptyList())

    when (productState) {
        is Resource.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is Resource.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text((productState as Resource.Error).message ?: "Lỗi kết nối")
            }
        }
        is Resource.Success -> {
            val product = (productState as Resource.Success<ProductDto>).data ?: return

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(product.name, style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.White
                        )
                    )
                },
                // ĐƯA NÚT RA ĐÂY ĐỂ CỐ ĐỊNH PHÍA DƯỚI
                bottomBar = {
                    ProductBottomBar(
                        product = product,
                        cart = cart,
                        onAddToCart = { homeViewModel.addToCart(product) },
                        onIncrease = { homeViewModel.increaseQty(product) },
                        onDecrease = { homeViewModel.decreaseQty(product) },
                        onCheckout = {
                            val checkoutCart = if (cart.any { it.product.id == product.id }) cart
                            else listOf(CartItem(product, 1))
                            navController.currentBackStackEntry?.savedStateHandle?.set("checkout_cart", checkoutCart)
                            navController.navigate("checkout")
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8F8F8)) // Màu nền nhẹ cho toàn trang
                        .padding(padding)
                ) {
                    // 1. Image Gallery với bo góc và shadow
                    item {
                        ProductImageGallery(product.images)
                    }

                    // 2. Product Info
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(16.dp)
                        ) {
                            Text(
                                product.name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${product.price} đ",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(5) { index ->
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (index < product.avg_rate.toInt()) Color(0xFFFFC107) else Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("${product.review_count} đánh giá", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                        }
                    }

                    // 3. Description
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .background(Color.White)
                                .padding(16.dp)
                        ) {
                            Text("Mô tả sản phẩm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                product.description ?: "Chưa có mô tả cho sản phẩm này.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // 4. Reviews
                    item {
                        Text(
                            "Đánh giá từ khách hàng",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    when (reviewState) {
                        is Resource.Loading -> {
                            item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
                        }

                        is Resource.Error -> {
                            item { Text("Không thể tải đánh giá", modifier = Modifier.padding(16.dp)) }
                        }

                        is Resource.Success -> {
                            val reviews = (reviewState as Resource.Success<List<ReviewResponseDto>>).data ?: emptyList()

                            if (reviews.isEmpty()) {
                                item { Text("Chưa có đánh giá nào", modifier = Modifier.padding(16.dp)) }
                            } else {
                                items(reviews) { review ->
                                    ReviewItem(review)
                                }
                            }
                        }
                    }


                    // ... (Phần Review giữ nguyên logic xử lý Resource nhưng bọc trong Background White)
                }
            }
        }
    }
}

@Composable
fun ReviewItem(review: ReviewResponseDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { index ->
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = if (index < review.rate) Color(0xFFFFC107) else Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(review.user_name, style = MaterialTheme.typography.labelMedium)
        }

        Spacer(Modifier.height(4.dp))

        Text(
            review.content,
            style = MaterialTheme.typography.bodyMedium
        )

        Divider(modifier = Modifier.padding(top = 12.dp))
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductImageGallery(images: List<ProductImageDto>) {
    val pagerState = rememberPagerState { if (images.isEmpty()) 1 else images.size }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(bottom = 16.dp)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(24.dp), // Bo góc sâu cho đẹp
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            HorizontalPager(state = pagerState) { page ->
                AsyncImage(
                    model = if (images.isNotEmpty()) images[page].url else "https://via.placeholder.com/600",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Indicator dots hiện đại hơn
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(6.dp)
                        .width(if (isSelected) 18.dp else 6.dp) // Dạng thanh dài khi chọn
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray)
                )
            }
        }
    }
}

@Composable
fun ProductBottomBar(
    product: ProductDto,
    cart: List<CartItem>,
    onAddToCart: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onCheckout: () -> Unit
) {
    val quantity = cart.find { it.product.id == product.id }?.quantity ?: 0

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding() // Tránh bị đè bởi thanh điều hướng Android
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nút Thêm / Tăng giảm (Chiếm 1/2 chiều ngang)
            Box(modifier = Modifier.weight(1f)) {
                if (quantity > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F0F0)),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDecrease) { Text("-", fontWeight = FontWeight.Bold) }
                        Text("$quantity", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = onIncrease) { Text("+", fontWeight = FontWeight.Bold) }
                    }
                } else {
                    Button(
                        onClick = onAddToCart,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text("+ Thêm", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Nút Giao hàng (Chiếm 1/2 chiều ngang)
            Button(
                onClick = onCheckout,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Giao hàng", fontWeight = FontWeight.Bold)
            }
        }
    }
}