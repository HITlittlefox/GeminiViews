// ChatMessage.kt
package com.example.geminiviews.prompttalk.data

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable // 用于 PetListResponse

enum class SenderType {
    USER, GEMINI
}

@InternalSerializationApi
data class ChatMessage(
    var content: String,
    val sender: SenderType,
    val isTyping: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: Uri? = null, // 之前用于发送图片，现在暂时用不上
    val images: List<Bitmap>? = null, // 之前用于接收图片，现在暂时用不上
    val petList: PetListResponse? = null // !!! 新增：用于存储解析后的宠物列表 !!!
)