package com.example.deliveryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.deliveryapp.data.remote.interceptor.AuthInterceptor
import com.example.deliveryapp.ui.navigation.NavGraph
import com.example.deliveryapp.ui.navigation.Screen
import com.example.deliveryapp.ui.session.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ✅ Gắn callback cho interceptor
        AuthInterceptor.setLogoutCallback {
            sessionViewModel.notifySessionExpiredFromInterceptor()
        }

        setContent {
            MaterialTheme {

                val navController = rememberNavController()

                // ✅ Lắng nghe sự kiện session hết hạn
                LaunchedEffect(Unit) {
                    sessionViewModel.sessionExpired.collect {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    NavGraph(
                        navController = navController,
                        startDestination = Screen.Splash.route
                    )
                }
            }
        }
    }
}
