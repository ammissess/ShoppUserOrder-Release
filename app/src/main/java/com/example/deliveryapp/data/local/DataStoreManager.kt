package com.example.deliveryapp.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.deliveryapp.security.CryptoManager
import com.example.deliveryapp.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DataStoreManager"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFS_NAME)

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) {
    companion object {
        // Key cũ (plaintext)
        private val OLD_ACCESS_KEY = stringPreferencesKey("access_token")
        private val OLD_REFRESH_KEY = stringPreferencesKey("refresh_token")

        // Key mới (encrypted)
        val ACCESS_TOKEN_ENC_KEY = stringPreferencesKey("access_token_enc")
        val REFRESH_TOKEN_ENC_KEY = stringPreferencesKey("refresh_token_enc")

        val LATITUDE_KEY = doublePreferencesKey("latitude")
        val LONGITUDE_KEY = doublePreferencesKey("longitude")
    }

    // ✅ Migration tự động khi init
    init {
        // Không dùng coroutine trong init, sẽ gọi từ bên ngoài
    }

    /**
     * ✅ Gọi hàm này TỪ BÊN NGOÀI (MainActivity/Application) 1 lần duy nhất
     */
    suspend fun migrateIfNeeded() {
        try {
            val prefs = context.dataStore.data.first()

            val oldAccess = prefs[OLD_ACCESS_KEY]
            val oldRefresh = prefs[OLD_REFRESH_KEY]

            // Nếu có token cũ và chưa có token mới
            if (oldAccess != null && prefs[ACCESS_TOKEN_ENC_KEY] == null) {
                Log.d(TAG, "🔄 Migrating old tokens to encrypted format...")

                val encryptedAccess = cryptoManager.encrypt(oldAccess)
                val encryptedRefresh = oldRefresh?.let { cryptoManager.encrypt(it) }

                if (encryptedAccess != null) {
                    context.dataStore.edit { mutablePrefs ->
                        // Lưu token mới (encrypted)
                        mutablePrefs[ACCESS_TOKEN_ENC_KEY] = encryptedAccess
                        if (encryptedRefresh != null) {
                            mutablePrefs[REFRESH_TOKEN_ENC_KEY] = encryptedRefresh
                        }

                        // Xóa token cũ
                        mutablePrefs.remove(OLD_ACCESS_KEY)
                        mutablePrefs.remove(OLD_REFRESH_KEY)
                    }

                    Log.d(TAG, "✅ Migration completed successfully")
                } else {
                    Log.e(TAG, "❌ Encryption failed during migration")
                }
            } else {
                Log.d(TAG, "ℹ️ No migration needed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Migration failed: ${e.message}", e)
        }
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        val encrypted = prefs[ACCESS_TOKEN_ENC_KEY]
        if (encrypted != null) {
            cryptoManager.decrypt(encrypted)
        } else {
            null
        }
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs ->
        val encrypted = prefs[REFRESH_TOKEN_ENC_KEY]
        if (encrypted != null) {
            cryptoManager.decrypt(encrypted)
        } else {
            null
        }
    }

    val latitude: Flow<Double?> = context.dataStore.data.map { prefs ->
        prefs[LATITUDE_KEY]
    }

    val longitude: Flow<Double?> = context.dataStore.data.map { prefs ->
        prefs[LONGITUDE_KEY]
    }

    suspend fun saveTokens(access: String, refresh: String) {
        Log.d(TAG, "Encrypting and saving tokens...")

        val encryptedAccess = cryptoManager.encrypt(access)
        val encryptedRefresh = cryptoManager.encrypt(refresh)

        if (encryptedAccess == null || encryptedRefresh == null) {
            Log.e(TAG, "Encryption failed, tokens not saved")
            return
        }

        // ✅ LOG TRƯỚC KHI GHI
        Log.d(
            TAG,
            "💾 Saving encrypted tokens:\n" +
                    "access_enc=${encryptedAccess.take(60)}...\n" +
                    "refresh_enc=${encryptedRefresh.take(60)}..."
        )

        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_ENC_KEY] = encryptedAccess
            prefs[REFRESH_TOKEN_ENC_KEY] = encryptedRefresh
        }

        Log.d(TAG, "Tokens encrypted and saved successfully")
    }

    suspend fun clearTokens() {
        Log.d(TAG, "Clearing all tokens")
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_ENC_KEY)
            prefs.remove(REFRESH_TOKEN_ENC_KEY)
        }
    }

    suspend fun saveLocation(lat: Double, lng: Double) {
        Log.d(TAG, "Saving location: Lat=$lat, Lng=$lng")
        context.dataStore.edit { prefs ->
            prefs[LATITUDE_KEY] = lat
            prefs[LONGITUDE_KEY] = lng
        }
    }

    suspend fun clearLocation() {
        Log.d(TAG, "Clearing location")
        context.dataStore.edit { prefs ->
            prefs.remove(LATITUDE_KEY)
            prefs.remove(LONGITUDE_KEY)
        }
    }
}