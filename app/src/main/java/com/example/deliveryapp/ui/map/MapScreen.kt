package com.example.deliveryapp.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import kotlinx.coroutines.time.delay
import kotlin.time.Duration

@Composable
fun MapScreen(
    userLat: Double,
    userLng: Double,
    driverLat: Double,
    driverLng: Double
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )

    LaunchedEffect(userLat, userLng, driverLat, driverLng) {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { _ ->
            val annotationApi = mapView.annotations
            val pointAnnotationManager = annotationApi.createPointAnnotationManager()

            pointAnnotationManager.deleteAll()

            val userPoint = Point.fromLngLat(userLng, userLat)
            pointAnnotationManager.create(
                PointAnnotationOptions().withPoint(userPoint)
            )

            val driverPoint = Point.fromLngLat(driverLng, driverLat)
            pointAnnotationManager.create(
                PointAnnotationOptions().withPoint(driverPoint)
            )

            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(userPoint)
                    .zoom(12.0)
                    .build()
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onStop()
            mapView.onDestroy()
        }
    }
}
