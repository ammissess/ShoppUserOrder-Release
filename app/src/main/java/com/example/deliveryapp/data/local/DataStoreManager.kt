package com.example.deliveryapp.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.deliveryapp.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DataStoreManager"

// Đảm bảo đây là top-level property để DataStore được khởi tạo đúng cách
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFS_NAME)

//class DataStoreManager(private val context: Context)

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
)

{
    companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)
        val REFRESH_TOKEN_KEY = stringPreferencesKey(Constants.KEY_REFRESH_TOKEN)

        // Thêm keys cho latitude/longitude
        val LATITUDE_KEY = doublePreferencesKey("latitude")
        val LONGITUDE_KEY = doublePreferencesKey("longitude")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN_KEY]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[REFRESH_TOKEN_KEY]
    }

    // Thêm flows cho latitude/longitude
    val latitude: Flow<Double?> = context.dataStore.data.map { prefs ->
        prefs[LATITUDE_KEY]
    }

    val longitude: Flow<Double?> = context.dataStore.data.map { prefs ->
        prefs[LONGITUDE_KEY]
    }

    suspend fun saveTokens(access: String, refresh: String) {
        Log.d(TAG, "Saving tokens - Access: ${access.take(10)}..., Refresh: ${refresh.take(10)}...")
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = access
            prefs[REFRESH_TOKEN_KEY] = refresh
        }

        // Verify tokens were saved
        val savedAccess = context.dataStore.data.first()[ACCESS_TOKEN_KEY]
        val savedRefresh = context.dataStore.data.first()[REFRESH_TOKEN_KEY]
        Log.d(TAG, "Saved tokens - Access: ${savedAccess?.take(10)}..., Refresh: ${savedRefresh?.take(10)}...")
    }

    suspend fun clearTokens() {
        Log.d(TAG, "Clearing all tokens")
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
        }
    }

    // Thêm method saveLocation
    suspend fun saveLocation(lat: Double, lng: Double) {
        Log.d(TAG, "Saving location: Lat=$lat, Lng=$lng")
        context.dataStore.edit { prefs ->
            prefs[LATITUDE_KEY] = lat
            prefs[LONGITUDE_KEY] = lng
        }
    }

    // Optional: Thêm clearLocation nếu cần reset
    suspend fun clearLocation() {
        Log.d(TAG, "Clearing location")
        context.dataStore.edit { prefs ->
            prefs.remove(LATITUDE_KEY)
            prefs.remove(LONGITUDE_KEY)
        }
    }
}