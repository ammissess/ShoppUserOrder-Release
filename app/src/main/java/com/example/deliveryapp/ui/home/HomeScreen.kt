package com.example.deliveryapp.ui.home

import android.os.Parcelable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.deliveryapp.data.remote.dto.ProductDto
import com.example.deliveryapp.ui.navigation.Screen
import com.example.deliveryapp.utils.Resource
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.text.NumberFormat
import java.util.*
import androidx.compose.material.icons.filled.Star
import android.util.Log
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow

@Parcelize
data class CartItem(
    val product: ProductDto,
    val quantity: Int = 1
) : Parcelable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = hiltViewModel()
) {

    val gridState = rememberLazyGridState()

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val productsState by homeViewModel.products.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val categories by homeViewModel.categories.collectAsState()
    val selectedCategory by homeViewModel.selectedCategory.collectAsState()
    val cart by homeViewModel.cart.collectAsState()

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showCartSheet by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    Text("Danh mục", modifier = Modifier.padding(16.dp))

                    categories.forEach { category ->
                        Text(
                            text = category,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    homeViewModel.fetchProductsByCategory(category)
                                }
                                .padding(16.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                homeViewModel.logout()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                        Spacer(Modifier.width(8.dp))
                        Text("Đăng xuất")
                    }
                }
            },
            drawerState = drawerState
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(selectedCategory.ifEmpty { "Tất cả" }) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                },
                bottomBar = {
                    BottomNavigationBar(
                        navController = navController,
                        selectedTab = selectedTab,
                        onTabSelected = { newTab -> selectedTab = newTab }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            homeViewModel.searchProducts(it.text)
                        },
                        label = { Text("Tìm sản phẩm...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )

                    when (productsState) {
                        is Resource.Loading -> {
                            if (productsState.data.isNullOrEmpty()) {
                                Box(Modifier.fillMaxSize(), Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        is Resource.Error -> {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text(productsState.message ?: "Đã xảy ra lỗi")
                            }
                        }
                        is Resource.Success -> {
                            val products = productsState.data ?: emptyList()
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize().weight(1f)
                            ) {
                                items(
                                    items = products,
                                    key = { product -> product.id } // ✅ DUY NHẤT
                                ) { product ->
                                    ProductItemDelivery(
                                        product = product,
                                        quantity = homeViewModel.getCartQuantity(product.id),
                                        onAdd = { homeViewModel.addToCart(product) },
                                        onIncrease = { homeViewModel.increaseQty(product) },
                                        onDecrease = { homeViewModel.decreaseQty(product) },
                                        onClick = {
                                            navController.navigate(Screen.ProductDetail.createRoute(product.id))
                                        }
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        // --- Cart bar dưới ---
        if (cart.isNotEmpty()) {
            val totalItems = cart.sumOf { it.quantity }
            val totalPrice = cart.sumOf { it.product.price * it.quantity }

            Box(Modifier.align(Alignment.BottomCenter)) {
                CartBar(
                    itemCount = totalItems,
                    totalPrice = totalPrice,
                    onCartClick = { showCartSheet = true },
                    //thay doi nut thanh Giao hang
                   // onCheckout = { navController.navigate("checkout") }
                    onCheckout = {
                        // Truyền cart qua navigation
                        navController.currentBackStackEntry?.savedStateHandle?.set("checkout_cart", cart)
                        navController.navigate("checkout")
                    }
                )
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }


    val products: List<ProductDto> = when (productsState) {
        is Resource.Success -> productsState.data ?: emptyList()
        else -> emptyList()
    }


    LaunchedEffect(gridState, products.size){
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                val total = products.size
                if (lastIndex != null && lastIndex >= total - 4) {
                    homeViewModel.loadMoreProducts()
                }
            }
    }

    // Lắng nghe sự kiện xóa giỏ hàng từ màn hình Checkout
    LaunchedEffect(navController) {
        navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("clear_cart")?.let {
            if (it) {
                Log.d("HomeScreen", "Received clear_cart event, clearing cart")
                homeViewModel.clearCart()
                navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("clear_cart")
            }
        }
    }

    // --- BottomSheet ---
    if (showCartSheet) {
        ModalBottomSheet(onDismissRequest = { showCartSheet = false }) {
            CartSheetContent(
                cart = cart,
                onIncrease = { homeViewModel.increaseQty(it) },
                onDecrease = { homeViewModel.decreaseQty(it) },
                onClear = { homeViewModel.clearCart() }
            )
        }
    }
}

@Composable
fun ProductItemDelivery(
    product: ProductDto,
    quantity: Int,
    onAdd: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .height(380.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp)
        ) {

            // Ảnh
            val mainImage = product.images.firstOrNull { it.isMain }?.url
                ?: product.images.firstOrNull()?.url

            AsyncImage(
                model = mainImage ?: "",
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(8.dp))

            // Tên + Giá
            Text(
                product.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatPrice(product.price),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // ⭐ Rating
            Row(verticalAlignment = Alignment.CenterVertically) {
                val avgRate = (product.avg_rate ?: 0).toInt().coerceIn(0, 5)
                repeat(avgRate) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(14.dp))
                }
                repeat(5 - avgRate) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("(${product.review_count ?: 0})", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(4.dp))

            // Mô tả ngắn (cắt tối đa 2 dòng thôi để không làm vỡ layout)
            Text(
                text = product.description ?: "",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 🔥 Spacer đẩy nút xuống đáy
            Spacer(modifier = Modifier.weight(1f))

            // Nút giỏ hàng
            if (quantity > 0) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDecrease) { Text("−") }
                    Text("$quantity", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = onIncrease) { Text("+") }
                }
            } else {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Thêm")
                }
            }
        }
    }



}


/** CartBar có icon giỏ hàng */
@Composable
fun CartBar(
    itemCount: Int,
    totalPrice: Double,
    onCartClick: () -> Unit,
    onCheckout: () -> Unit
) {
    Surface(
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clickable { onCartClick() },
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    if (itemCount > 0) {
                        Surface(
                            color = Color.Red,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$itemCount",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(formatPrice(totalPrice),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = onCheckout) { Text("Giao hàng") }
        }
    }
}

/** Nội dung BottomSheet */
@Composable
fun CartSheetContent(
    cart: List<CartItem>,
    onIncrease: (ProductDto) -> Unit,
    onDecrease: (ProductDto) -> Unit,
    onClear: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Giỏ hàng", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onClear) { Text("Xóa tất cả") }
        }

        Spacer(Modifier.height(8.dp))

        cart.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = item.product.images.firstOrNull()?.url,
                    contentDescription = item.product.name,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.product.name, style = MaterialTheme.typography.bodyLarge)
                    Text(formatPrice(item.product.price),
                        color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onDecrease(item.product) }) { Text("−") }
                    Text("${item.quantity}", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { onIncrease(item.product) }) { Text("+") }
                }
            }
            Divider()
        }
    }
}

fun formatPrice(price: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    return formatter.format(price)
}