package com.example.deliveryapp.ui.navigation

import com.example.deliveryapp.ui.message.MessagesScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.deliveryapp.ui.auth.*
import com.example.deliveryapp.ui.home.HomeScreen
import com.example.deliveryapp.ui.product.ProductDetailScreen
import com.example.deliveryapp.ui.order.OrderStatusScreen
import com.example.deliveryapp.ui.profile.ProfileScreen
import com.example.deliveryapp.ui.auth.SplashScreen   // ✅ import Splash
import com.example.deliveryapp.ui.home.CartItem
import com.example.deliveryapp.ui.map.LocationPickerScreen
import com.example.deliveryapp.ui.order.CheckoutScreen
import com.example.deliveryapp.ui.order.CheckoutViewModel
import com.example.deliveryapp.ui.order.OrderDetailScreen
import com.example.deliveryapp.ui.order.OrderListScreen
import com.example.deliveryapp.ui.product.ProductDetailReviewScreen
import com.example.deliveryapp.ui.profile.CustomProfile

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route // ✅ Splash mặc định
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ✅ Splash route
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }

        //route cho SessionGate
        composable(Screen.SessionGate.route) {
            SessionGateScreen(navController)
        }


        // Authentication routes
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Signup.route) { SignupScreen(navController) }

        composable(
            route = "otp_verify/{email}",
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            OtpVerifyScreen(navController, email)
        }

        composable("forgot_password") { ForgotPasswordScreen(navController) }

        composable(
            route = "reset_password/{email}",
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ResetPasswordScreen(navController, email)
        }

        // Main app routes
        composable(Screen.Home.route) { HomeScreen(navController) }

//        composable("messages") {
//            MessagesScreen(navController, orderId = 0L, shipperId = 0L, shipperName = "")  // Default args để tránh crash
//        }
        composable("messages") {
            MessagesScreen(navController, orderId = 0L, shipperId = 0L)
        }

//        composable(
//            route = "messages/{orderId}/{shipperId}/{shipperName}",
//            arguments = listOf(
//                navArgument("orderId") { type = NavType.LongType },
//                navArgument("shipperId") { type = NavType.LongType },
//                navArgument("shipperName") { type = NavType.StringType }
//            )
//        ) { backStackEntry ->
//            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
//            val shipperId = backStackEntry.arguments?.getLong("shipperId") ?: 0L
//            val shipperName = backStackEntry.arguments?.getString("shipperName") ?: ""
//            MessagesScreen(navController, orderId, shipperId, shipperName)
//        }

//        composable(
//            route = "messages/{orderId}",
//            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
//        ) { backStackEntry ->
//            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
//            // vì không có shipperId / shipperName nên truyền mặc định 0 và ""
//            MessagesScreen(navController, orderId = orderId, shipperId = 0L, shipperName = "")
//        }

        composable(
            route = "messages/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            MessagesScreen(navController, orderId = orderId, shipperId = 0L)
        }



        // Chi tiết / trạng thái đơn hàng
        composable(
            route = "orderStatus/{orderId}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            OrderStatusScreen(orderId = orderId, navController = navController)
        }


// Chat giữa user và shipper
        composable(
            route = "messages/{orderId}/{shipperId}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.LongType },
                navArgument("shipperId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            val shipperId = backStackEntry.arguments?.getLong("shipperId") ?: 0L
            MessagesScreen(navController, orderId = orderId, shipperId = shipperId)
        }



        composable("profile") { ProfileScreen(navController) }

//Them nut checkout Giao hang
        composable("checkout") {
            val cart = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<CartItem>>("checkout_cart") ?: emptyList()

            val viewModel: CheckoutViewModel = hiltViewModel()  // ← Tạo ViewModel

            LaunchedEffect(Unit) {  // ← Set cart ngay khi composable load
                viewModel.setCart(cart)
            }

            CheckoutScreen(navController, viewModel)  // ← Truyền ViewModel thay vì cart
        }

        // Màn hình đánh giá sản phẩm sau khi giao hàng
        composable(
            route = "product_detail_review/{productId}/{orderId}",
            arguments = listOf(
                navArgument("productId") { type = NavType.LongType },
                navArgument("orderId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L

            // ✅ Gọi đúng composable
            ProductDetailReviewScreen(
                navController = navController,
                productId = productId,
                orderId = orderId
            )
        }

        //Route sang màn hình order
        // Thêm vào NavGraph:
        composable("order_list") {
            OrderListScreen(navController)
        }

        composable(
            route = "order_detail/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            OrderDetailScreen(navController, orderId)  // Tạo màn hình này nếu cần
        }

        // Edit Profile route
        composable(Screen.EditProfile.route) {
            CustomProfile(navController)  // Tên file như yêu cầu của bạn
        }

        // Location Picker route (xử lý result từ LocationPicker)
        composable(Screen.LocationPicker.route) {
            val backStackEntry = navController.previousBackStackEntry
            LocationPickerScreen(navController = navController)

            // Xử lý result từ LocationPicker (lat, lng, address)
            val selectedLat = navController.currentBackStackEntry?.savedStateHandle?.get<Double>("selectedLat")
            val selectedLng = navController.currentBackStackEntry?.savedStateHandle?.get<Double>("selectedLng")
            val selectedAddress = navController.currentBackStackEntry?.savedStateHandle?.get<String>("selectedAddress")

            if (selectedLat != null && selectedLng != null && selectedAddress != null) {
                backStackEntry?.savedStateHandle?.set("lat", selectedLat)
                backStackEntry?.savedStateHandle?.set("lng", selectedLng)
                backStackEntry?.savedStateHandle?.set("address", selectedAddress)
                navController.currentBackStackEntry?.savedStateHandle?.remove<Double>("selectedLat")
                navController.currentBackStackEntry?.savedStateHandle?.remove<Double>("selectedLng")
                navController.currentBackStackEntry?.savedStateHandle?.remove<String>("selectedAddress")
            }
        }

        // Product detail route
        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("productId") ?: 0L
            ProductDetailScreen(navController, productId = id)
        }
    }
}