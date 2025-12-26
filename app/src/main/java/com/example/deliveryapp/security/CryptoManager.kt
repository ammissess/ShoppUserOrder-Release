package com.example.deliveryapp.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CryptoManager"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "delivery_app_token_key"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH = 128

@Singleton
class CryptoManager @Inject constructor() {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        createKeyIfNotExists()
    }

    /**
     * Tạo AES key trong Android Keystore nếu chưa có
     */
    private fun createKeyIfNotExists() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            Log.d(TAG, "Creating new AES key in Keystore")

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false) // Không yêu cầu biometric
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()

            Log.d(TAG, "AES key created successfully")
        } else {
            Log.d(TAG, "AES key already exists in Keystore")
        }
    }

    /**
     * Lấy SecretKey từ Keystore
     */
    private fun getSecretKey(): SecretKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Mã hóa plaintext
     * @return Base64(iv) + "." + Base64(ciphertext)
     */
    fun encrypt(plaintext: String): String? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

            val iv = cipher.iv // IV ngẫu nhiên được sinh tự động
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // Encode iv và ciphertext thành Base64
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

            // Format: iv.ciphertext
            val encrypted = "$ivBase64.$ciphertextBase64"

            Log.d(TAG, "Encryption successful")

            // ✅ LOG DEBUG – CHỈ DÙNG DEV
            Log.d(
                TAG,
                "🔐 AES encrypted token (iv.cipher): ${encrypted.take(60)}... (len=${encrypted.length})"
            )
            encrypted

        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ${e.message}", e)
            null
        }
    }

    /**
     * Giải mã ciphertext
     * @param encrypted Format: Base64(iv) + "." + Base64(ciphertext)
     * @return plaintext hoặc null nếu lỗi
     */
    fun decrypt(encrypted: String): String? {
        return try {
            val parts = encrypted.split(".")
            if (parts.size != 2) {
                Log.e(TAG, "Invalid encrypted format")
                return null
            }

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val plaintext = cipher.doFinal(ciphertext)
            val decrypted = String(plaintext, Charsets.UTF_8)

            Log.d(TAG, "Decryption successful")
            decrypted

        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${e.message}", e)
            null
        }
    }
}