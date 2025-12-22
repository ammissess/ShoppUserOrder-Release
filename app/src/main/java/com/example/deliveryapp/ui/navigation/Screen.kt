package com.example.deliveryapp.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object SessionGate : Screen("session_gate")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Home : Screen("home")
    object ProductDetail : Screen("product/{productId}") {
        fun createRoute(id: Long) = "product/$id"
    }
    object OrderStatus : Screen("order/{orderId}") {
        fun createRoute(id: Long) = "order/$id"
    }

    object EditProfile : Screen("edit_profile")
    object LocationPicker : Screen("location_picker")



    object Profile : Screen("profile")
    object CustomProfile : Screen("custom_profile")
    object Register : Screen("register")

    //tao don giao hang
    object Checkout : Screen("checkout")
}