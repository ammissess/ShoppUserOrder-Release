package com.example.deliveryapp.utils

import android.util.Base64
import org.json.JSONObject

object JwtUtils {
    fun getRoleFromToken(token: String): String? {
        try {
            // Token JWT gồm 3 phần ngăn cách bởi dấu chấm. Phần giữa (index 1) chứa dữ liệu (Payload)
            val parts = token.split(".")
            if (parts.size < 2) return null

            // Giải mã Base64
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE), Charsets.UTF_8)

            // Parse JSON để lấy field "role"
            val json = JSONObject(payload)

            // Lưu ý: Tùy Backend đặt tên key là "role", "roles", hay "aud".
            // Code này ưu tiên lấy "role"
            return json.optString("role")
        } catch (e: Exception) {
            return null
        }
    }
}