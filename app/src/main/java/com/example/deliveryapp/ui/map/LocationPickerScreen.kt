package com.example.deliveryapp.ui.map

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.deliveryapp.R
import com.example.deliveryapp.utils.Resource
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import kotlinx.coroutines.launch

private const val TAG = "LocationPickerScreen"

@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
@Composable
fun LocationPickerScreen(
    navController: NavController,
    viewModel: LocationPickerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val addressState by viewModel.addressState.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedLocation by viewModel.selectedLocation.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var isLoadingCurrentLocation by remember { mutableStateOf(false) }
    var mapReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Trì hoãn 500ms để tránh tranh chấp tài nguyên khi app vừa mở
        kotlinx.coroutines.delay(2000)
        mapReady = true
    }
//    LaunchedEffect(Unit) {
//        mapReady = true
//    }

    val coroutineScope = rememberCoroutineScope()
    val pinIcon = rememberIconImage(resourceId = R.drawable.ic_locationn)

    val defaultCenter = Point.fromLngLat(105.804817, 21.028511)
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(defaultCenter)
            zoom(12.0)
        }
    }

    // FusedLocationProviderClient
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Launcher để xử lý GPS settings
    val resolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // GPS đã bật, gọi lại để lấy vị trí
            getUserLocation(fusedLocationClient, context) { location ->
                location?.let {
                    val lat = it.latitude
                    val lng = it.longitude
                    Log.d(TAG, "Current location after GPS enabled: Lat=$lat, Lng=$lng")

                    viewModel.selectLocation(lat, lng)
                    coroutineScope.launch {
                        mapViewportState.flyTo(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(lng, lat))
                                .zoom(17.0)
                                .build()
                        )
                    }
                }
                isLoadingCurrentLocation = false
            }
        } else {
            isLoadingCurrentLocation = false
            Toast.makeText(context, "Vui lòng bật GPS để sử dụng chức năng này", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted, check GPS và request location update
            requestLocationUpdate(fusedLocationClient, context, resolutionLauncher) {
                getUserLocation(fusedLocationClient, context) { location ->
                    location?.let {
                        val lat = it.latitude
                        val lng = it.longitude
                        Log.d(TAG, "Current location: Lat=$lat, Lng=$lng")

                        viewModel.selectLocation(lat, lng)
                        coroutineScope.launch {
                            mapViewportState.flyTo(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(lng, lat))
                                    .zoom(17.0)
                                    .build()
                            )
                        }
                    } ?: run {
                        Toast.makeText(context, "Không thể lấy vị trí hiện tại", Toast.LENGTH_SHORT).show()
                    }
                    isLoadingCurrentLocation = false
                }
            }
        } else {
            isLoadingCurrentLocation = false
            Toast.makeText(context, "Cần cấp quyền truy cập vị trí", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Debug info
        selectedLocation?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "DEBUG: Lat=${it.lat}, Lng=${it.lng}",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Nút vị trí hiện tại + Tìm kiếm
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Nút vị trí hiện tại
            Button(
                onClick = {
                    isLoadingCurrentLocation = true
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoadingCurrentLocation
            ) {
                if (isLoadingCurrentLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Đang lấy...")
                } else {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Vị trí hiện tại")
                }
            }

            // Nút tìm kiếm
            OutlinedButton(
                onClick = { showSearch = !showSearch },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (showSearch) "Ẩn" else "Tìm kiếm")
            }
        }

        // Phần tìm kiếm
        if (showSearch) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchLocation(it)
                    },
                    label = { Text("Nhập địa chỉ cần tìm") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Kết quả tìm kiếm
            when (val res = searchResults) {
                is Resource.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        val list = res.data ?: emptyList()
                        items(list) { result ->
                            ListItem(
                                headlineContent = { Text(result.display_name) },
                                supportingContent = { Text("Lat: ${result.lat}, Lng: ${result.lon}") },
                                modifier = Modifier.clickable {
                                    val lat = result.lat.toDoubleOrNull() ?: return@clickable
                                    val lng = result.lon.toDoubleOrNull() ?: return@clickable
                                    val address = result.display_name

                                    viewModel.selectLocation(lat, lng)

                                    coroutineScope.launch {
                                        mapViewportState.flyTo(
                                            CameraOptions.Builder()
                                                .center(Point.fromLngLat(lng, lat))
                                                .zoom(15.0)
                                                .build()
                                        )
                                    }

                                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                                        set("selectedLat", lat)
                                        set("selectedLng", lng)
                                        set("selectedAddress", address)
                                        Log.d(TAG, "📤 Sent: lat=$lat, lng=$lng, address=$address")
                                    }

                                    showSearch = false
                                    searchQuery = ""
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }

                is Resource.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is Resource.Error -> {
                    Text(
                        text = "Không tìm thấy kết quả: ${res.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // Bản đồ
        if (mapReady) {
        MapboxMap(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            mapViewportState = mapViewportState,
            style = {
                MapboxStandardStyle()
            }
        ) {
            MapEffect(Unit) { mapView ->
                mapView.mapboxMap.addOnMapClickListener { point ->
                    val lat = point.latitude()
                    val lng = point.longitude()

                    Log.d(TAG, "Map clicked at: Lat=$lat, Lng=$lng")
                    viewModel.selectLocation(lat, lng)

                    coroutineScope.launch {
                        mapViewportState.flyTo(
                            CameraOptions.Builder()
                                .center(point)
                                .zoom(15.0)
                                .build()
                        )
                    }
                    true
                }
            }

            selectedLocation?.let { location ->
                val point = Point.fromLngLat(location.lng, location.lat)
                PointAnnotation(point = point) {
                    iconImage = pinIcon
                }
            }
        }
    }
        // Phần xác nhận địa chỉ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            when (val state = addressState) {
                is Resource.Loading -> {
                    if (selectedLocation != null) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Đang tải địa chỉ...")
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text(
                                    text = "Tọa độ: (${String.format("%.6f", selectedLocation!!.lat)}, ${String.format("%.6f", selectedLocation!!.lng)})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                is Resource.Success -> {
                    val address = state.data ?: "Vị trí không xác định"
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Địa chỉ được chọn:", style = MaterialTheme.typography.titleMedium)
                            Text(address)

                            selectedLocation?.let { location ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Latitude: ${String.format("%.6f", location.lat)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Longitude: ${String.format("%.6f", location.lng)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    selectedLocation?.let { location ->
                                        Log.d(TAG, "Button clicked - Sending data")
                                        Log.d(TAG, "Lat: ${location.lat}, Lng: ${location.lng}, Address: $address")

                                        navController.previousBackStackEntry
                                            ?.savedStateHandle
                                            ?.apply {
                                                set("selectedLat", location.lat)
                                                set("selectedLng", location.lng)
                                                set("selectedAddress", address)
                                                Log.d(TAG, "📤 Data saved to savedStateHandle")
                                            }

                                        navController.popBackStack()
                                    } ?: Log.e(TAG, "selectedLocation is NULL!")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedLocation != null
                            ) {
                                Text("Xác nhận vị trí")
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    val errorAddress = state.data ?: "Vị trí không xác định (lỗi API)"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Lỗi lấy địa chỉ: ${state.message}",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            selectedLocation?.let { location ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Latitude: ${String.format("%.6f", location.lat)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Longitude: ${String.format("%.6f", location.lng)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        Log.d(TAG, "Error case - Button clicked")
                                        Log.d(TAG, "Lat: ${location.lat}, Lng: ${location.lng}, Address: $errorAddress")

                                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                                            set("selectedLat", location.lat)
                                            set("selectedLng", location.lng)
                                            set("selectedAddress", errorAddress)
                                            Log.d(TAG, "📤 Data saved (error case)")
                                        } ?: Log.e(TAG, "previousBackStackEntry is NULL!")

                                        navController.popBackStack()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Xác nhận tọa độ")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper functions
private fun getUserLocation(
    fusedLocationClient: FusedLocationProviderClient,
    context: android.content.Context,
    onLocationResult: (android.location.Location?) -> Unit
) {
    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                onLocationResult(location)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting location: ${e.message}")
                onLocationResult(null)
            }
    } catch (e: SecurityException) {
        Log.e(TAG, "Location permission not granted: ${e.message}")
        onLocationResult(null)
    }
}

private fun requestLocationUpdate(
    fusedLocationClient: FusedLocationProviderClient,
    context: android.content.Context,
    resolutionLauncher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>,
    onSuccess: () -> Unit
) {
    val locationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = 10000
        fastestInterval = 5000
    }

    val builder = LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest)

    val settingsClient = LocationServices.getSettingsClient(context)

    settingsClient.checkLocationSettings(builder.build())
        .addOnSuccessListener {
            // GPS đã bật
            Log.d(TAG, "GPS is already enabled")
            onSuccess()
        }
        .addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    resolutionLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, "Error: ${sendEx.message}")
                }
            } else {
                Toast.makeText(
                    context,
                    "Thiết bị không hỗ trợ định vị GPS",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
}