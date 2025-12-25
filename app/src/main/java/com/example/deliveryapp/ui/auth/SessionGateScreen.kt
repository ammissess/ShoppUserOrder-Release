package com.example.deliveryapp.ui.auth

import android.text.Layout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.deliveryapp.ui.navigation.Screen
import com.example.deliveryapp.ui.session.SessionViewModel
import androidx.compose.ui.Alignment


@Composable
fun SessionGateScreen(
    navController: NavController,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        when (isLoggedIn) {
            true -> navController.navigate(Screen.Home.route) {
                popUpTo(Screen.SessionGate.route) { inclusive = true }
                launchSingleTop = true
                restoreState = true
            }

            false -> navController.navigate(Screen.Login.route) {
                popUpTo(Screen.SessionGate.route) { inclusive = true }
                launchSingleTop = true
                restoreState = true
            }

            null -> Unit
        }
    }


    Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator()
    }
}
