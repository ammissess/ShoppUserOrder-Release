package com.example.deliveryapp.ui.message

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.remote.api.ChatApi
import com.example.deliveryapp.utils.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val chatApi: ChatApi
) : ViewModel() {

    private val _conversations = MutableStateFlow<Map<Long, List<ChatMessage>>>(emptyMap())
    val conversations: StateFlow<Map<Long, List<ChatMessage>>> = _conversations

    var currentUserId: Long? = null
        private set

    private var currentOrderId: Long? = null
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private val gson = Gson()

    init {
        loadCachedConversations()
    }

    private fun parseCreatedAt(value: String): Long {
        return try {
            val formatter = java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()
            )
            formatter.parse(value)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun loadMessagesFromServer(orderId: Long) {
        viewModelScope.launch {
            try {
                Log.d("ChatVM", "🔥 Loading messages from DB for order=$orderId")
                Log.d("ChatVM", "🔐 Current User ID when loading: $currentUserId") // ✅ Debug

                val res = chatApi.getMessages(orderId, limit = 50)

                if (res.isSuccessful) {
                    val serverMessages = res.body()?.data ?: emptyList()

                    Log.d("ChatVM", "📦 API Response size: ${serverMessages.size}")

                    // ✅ Debug: In ra message đầu tiên từ API
                    serverMessages.firstOrNull()?.let { first ->
                        Log.d("ChatVM", "📦 First API message: from_user=${first.from_user_id}, to_user=${first.to_user_id}, content=${first.content}")
                    }

                    val mapped = serverMessages.map { msg ->
                        Log.d("ChatVM", "🔄 Mapping: from_user_id=${msg.from_user_id}, to_user_id=${msg.to_user_id}")

                        ChatMessage(
                            fromUserId = msg.from_user_id.toLong(),  // ✅ Dùng from_user_id
                            toUserId = msg.to_user_id.toLong(),      // ✅ Dùng to_user_id
                            content = msg.content,
                            createdAt = parseCreatedAt(msg.created_at)
                        )
                    }
                        //.sortedBy { it.createdAt }
                        .sortedByDescending { it -> it.createdAt }

                    _conversations.value = _conversations.value + (orderId to mapped)
                    saveConversation(orderId, mapped)

                    Log.d("ChatVM", "✅ Loaded ${mapped.size} messages from DB")

                    // ✅ Debug: In ra vài tin nhắn đầu tiên
                    mapped.take(3).forEach {
                        Log.d("ChatVM", "📨 Message: from=${it.fromUserId}, to=${it.toUserId}, content=${it.content}")
                    }
                } else {
                    Log.e("ChatVM", "❌ Load DB failed: ${res.code()}")
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "❌ Load DB exception", e)
            }
        }
    }

    // ✅ Hàm mới: Set currentUserId từ token
    fun setCurrentUserFromToken(accessToken: String) {
        currentUserId = extractUserIdFromToken(accessToken)
        Log.d("ChatVM", "🔐 Set Current User ID: $currentUserId")
    }

    fun connectWebSocket(orderId: Long, accessToken: String) {
        currentOrderId = orderId

        // ✅ Đảm bảo currentUserId được set
        if (currentUserId == null) {
            setCurrentUserFromToken(accessToken)
        }

        val url = Constants.BASE_URL.replace("http", "ws") + "ws?token=$accessToken"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("ChatVM", "✅ WS connected (User ID: $currentUserId)")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val msg = ChatMessage(
                        fromUserId = json.optLong("from_user_id", 0L),
                        toUserId = json.optLong("to_user_id", 0L),
                        content = json.optString("content"),
                        createdAt = System.currentTimeMillis()
                    )
                    val id = currentOrderId ?: return
                    viewModelScope.launch { appendMessage(id, msg) }
                } catch (e: Exception) {
                    Log.e("ChatVM", "Parse error: ${e.message}")
                }
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                onMessage(ws, bytes.utf8())
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("ChatVM", "❌ WS Failure: ${t.message}")
            }
        })
    }

    fun extractUserIdFromToken(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) {
                Log.e("ChatVM", "❌ Token format invalid")
                return null
            }
            val payloadJson = String(
                android.util.Base64.decode(
                    parts[1],
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                )
            )
            Log.d("ChatVM", "🔍 Token payload: $payloadJson") // ✅ Debug

            val payload = org.json.JSONObject(payloadJson)

            // ✅ Thử nhiều tên field khác nhau
            val userId = when {
                payload.has("userID") -> payload.getLong("userID")  // ← API của bạn dùng "userID"
                payload.has("user_id") -> payload.getLong("user_id")
                payload.has("userId") -> payload.getLong("userId")
                payload.has("id") -> payload.getLong("id")
                else -> -1L
            }

            Log.d("ChatVM", "🔍 Extracted userID: $userId") // ✅ Debug

            if (userId == -1L) null else userId
        } catch (e: Exception) {
            Log.e("ChatVM", "❌ Extract token error: ${e.message}")
            null
        }
    }

    fun sendMessage(orderId: Long, content: String) {
        if (content.isBlank()) return

        val json = JSONObject().apply {
            put("type", "chat_message")
            put("order_id", orderId)
            put("content", content)
        }

        webSocket?.send(json.toString())

        val msg = ChatMessage(
            fromUserId = currentUserId ?: -1L,
            toUserId = -1L,
            content = content,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            appendMessage(orderId, msg)
        }
    }

    private fun appendMessage(orderId: Long, msg: ChatMessage) {
        val currentMap = _conversations.value
        val oldList = currentMap[orderId] ?: emptyList()

        val newList = oldList + msg
        val newMap = currentMap + (orderId to newList)

        _conversations.value = newMap
        saveConversation(orderId, newList)
    }

    fun clearConversation(orderId: Long) {
        val map = _conversations.value.toMutableMap()
        map.remove(orderId)
        _conversations.value = map
        deleteConversation(orderId)
    }

    override fun onCleared() {
        webSocket?.close(1000, "closed")
        super.onCleared()
    }

    private fun prefs() = appContext.getSharedPreferences("chat_cache_user", Context.MODE_PRIVATE)

    private fun saveConversation(orderId: Long, list: List<ChatMessage>) {
        prefs().edit().putString(orderId.toString(), gson.toJson(list)).apply()
    }

    private fun loadCachedConversations() {
        val all = prefs().all
        val map = mutableMapOf<Long, List<ChatMessage>>()
        all.forEach { (orderIdStr, jsonStr) ->
            val id = orderIdStr.toLongOrNull() ?: return@forEach
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            runCatching {
                val list: List<ChatMessage> = gson.fromJson(jsonStr as String, type)
                map[id] = list
            }
        }
        _conversations.value = map
    }

    private fun deleteConversation(orderId: Long) {
        prefs().edit().remove(orderId.toString()).apply()
    }
}