package com.example.deliveryapp.ui.message

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.ui.order.OrderViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val ChatBlue = Color(0xFF007AFF)
val ChatGray = Color(0xFFF2F2F7)
val TextBlack = Color(0xFF1C1C1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    navController: NavController,
    orderId: Long,
    shipperId: Long,
    //shipperName: String?,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val dataStore = remember { DataStoreManager(context) }
    val token by dataStore.accessToken.map { it ?: "" }.collectAsState(initial = "")

    val messages by viewModel.conversations.collectAsState()
    val currentList = messages[orderId] ?: emptyList()
    var inputText by remember { mutableStateOf("") }

    // ✅ Lấy currentUserId từ ViewModel
    val currentUserId = viewModel.currentUserId

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Lấy shipperName từ OrderViewModel

    val orderViewModel: OrderViewModel = hiltViewModel()
    LaunchedEffect(orderId) {
        orderViewModel.loadOrderDetail(orderId)
    }
    val shipperName by orderViewModel.shipperName.collectAsState()

    LaunchedEffect(orderId, token) {
        if (token.isNotEmpty()) {
            Log.d("MessagesScreen", "🔑 Token received: ${token.take(20)}...")

            // ✅ 1. Set currentUserId TRƯỚC
            viewModel.setCurrentUserFromToken(token)

            Log.d("MessagesScreen", "👤 Current User ID after set: ${viewModel.currentUserId}")

            // ✅ 2. Sau đó load messages
            viewModel.loadMessagesFromServer(orderId)

            // ✅ 3. Cuối cùng connect WebSocket
            viewModel.connectWebSocket(orderId, token)
        } else {
            Log.e("MessagesScreen", "❌ Token is empty!")
        }
    }

    LaunchedEffect(currentList.size) {
        if (currentList.isNotEmpty()) {
            listState.animateScrollToItem(currentList.lastIndex)
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            ChatTopBar(
                onBack = { navController.popBackStack() },
                onCall = { },
               // shipperName = "Shipper Giao Hàng"
                shipperName = shipperName ?: "Đang chờ shipper"
            )
        },
        bottomBar = {
            MessageInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(orderId, inputText)
                        inputText = ""
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            items(currentList) { msg ->
                // ✅ Truyền currentUserId vào ChatBubble
                ChatBubble(
                    message = msg,
                    currentUserId = currentUserId
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    onBack: () -> Unit,
    onCall: () -> Unit,
    shipperName: String
) {
    Surface(
        shadowElevation = 4.dp,
        color = Color.White
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = ChatBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = shipperName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = TextBlack
                        )
                        Text(
                            text = "Đang hoạt động",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChatBlue
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ChatBlue
                    )
                }
            },
            actions = {
                IconButton(onClick = onCall) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = ChatBlue
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    currentUserId: Long? // ✅ Nhận currentUserId
) {
    // ✅ Kiểm tra tin nhắn của mình dựa trên currentUserId
    val isUser = currentUserId != null && message.fromUserId == currentUserId

    val backgroundColor = if (isUser) ChatBlue else ChatGray
    val contentColor = if (isUser) Color.White else TextBlack
    val alignment = if (isUser) Alignment.End else Alignment.Start

    val bubbleShape = if (isUser) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(1.dp, shape = bubbleShape, clip = false)
                .background(backgroundColor, bubbleShape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                ),
                color = contentColor
            )
        }
    }
}

@Composable
fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ChatGray),
                placeholder = {
                    Text("Nhập tin nhắn...", color = Color.Gray, fontSize = 14.sp)
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ChatGray,
                    unfocusedContainerColor = ChatGray,
                    disabledContainerColor = ChatGray,
                    cursorColor = ChatBlue,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (text.isNotBlank()) ChatBlue else ChatGray,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank()) Color.White else Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}