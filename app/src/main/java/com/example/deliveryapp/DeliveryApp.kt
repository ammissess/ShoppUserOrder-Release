package com.example.deliveryapp

import android.app.Application
import com.example.deliveryapp.data.local.DataStoreManager

import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DeliveryApp : Application() {

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    override fun onCreate() {
        super.onCreate()

        // ✅ Migration chạy sớm nhất, 1 lần duy nhất
        CoroutineScope(Dispatchers.IO).launch {
            dataStoreManager.migrateIfNeeded()
        }
    }
}
