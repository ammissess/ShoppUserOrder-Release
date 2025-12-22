package com.example.deliveryapp.ui.message

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    @ApplicationContext private val appContext: Context
) : ViewModel() {

//    private val _conversations = MutableStateFlow<Map<Long, MutableList<ChatMessage>>>(emptyMap())
//    val conversations: StateFlow<Map<Long, MutableList<ChatMessage>>> = _conversations

    private val _conversations =
        MutableStateFlow<Map<Long, List<ChatMessage>>>(emptyMap())

    val conversations: StateFlow<Map<Long, List<ChatMessage>>> = _conversations


    private var currentOrderId: Long? = null
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private val gson = Gson()

    init {
        loadCachedConversations()
    }

    /** Kết nối WebSocket bằng token người dùng */
    fun connectWebSocket(orderId: Long, accessToken: String) {
        currentOrderId = orderId
        val url = Constants.BASE_URL.replace("http", "ws") + "ws?token=$accessToken"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("ChatVM", "✅ WS connected (User)")
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

    /** Gửi tin nhắn từ user sang shipper */
    /** Gửi tin nhắn từ user sang shipper */
    fun sendMessage(orderId: Long, shipperId: Long, content: String) {
        if (content.isBlank()) return
        if (shipperId == 0L) {
            Log.e("ChatVM", "❌ Không thể gửi vì shipperId = 0")
            return
        }

        // 🔍 Thêm dòng log này để kiểm tra shipperId thực tế
        Log.d("ChatSend", "🚀 Sending message toUser=$shipperId for order=$orderId")

        val json = JSONObject().apply {
            put("type", "chat_message")
            put("order_id", orderId)
            put("to_user_id", shipperId) // ✅ ID shipper thật (users.id)
            put("content", content)
        }

        Log.d("ChatVM", "📤 Sending: order=$orderId, to=$shipperId, msg=$content")
        webSocket?.send(json.toString())

        val msg = ChatMessage(
            fromUserId = -1L, // user hiện tại
            toUserId = shipperId,
            content = content,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch { appendMessage(orderId, msg) }
    }


    /** Lưu tin nhắn vào bộ nhớ và cache */
//    private fun appendMessage(orderId: Long, msg: ChatMessage) {
//        val map = _conversations.value.toMutableMap()
//        val list = map[orderId] ?: mutableListOf()
//        list.add(msg)
//        map[orderId] = list
//        _conversations.value = map
//        saveConversation(orderId, list)
//    }

    private fun appendMessage(orderId: Long, msg: ChatMessage) {
        val currentMap = _conversations.value
        val oldList = currentMap[orderId] ?: emptyList()

        val newList = oldList + msg   // ✅ tạo list mới
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

    // ======= Cache bằng SharedPreferences =======
    private fun prefs() = appContext.getSharedPreferences("chat_cache_user", Context.MODE_PRIVATE)

    private fun saveConversation(orderId: Long, list: List<ChatMessage>) {
        prefs().edit().putString(orderId.toString(), gson.toJson(list)).apply()
    }

    private fun loadCachedConversations() {
        val all = prefs().all
        val map = mutableMapOf<Long, MutableList<ChatMessage>>()
        all.forEach { (orderIdStr, jsonStr) ->
            val id = orderIdStr.toLongOrNull() ?: return@forEach
            val type = object : TypeToken<MutableList<ChatMessage>>() {}.type
            runCatching {
                val list: MutableList<ChatMessage> = gson.fromJson(jsonStr as String, type)
                map[id] = list
            }
        }
        _conversations.value = map
    }

    private fun deleteConversation(orderId: Long) {
        prefs().edit().remove(orderId.toString()).apply()
    }
}
