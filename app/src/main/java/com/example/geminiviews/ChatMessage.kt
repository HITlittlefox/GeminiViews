// ChatMessage.kt
package com.example.geminiviews

enum class SenderType {
    USER, GEMINI
}

data class ChatMessage(
    val content: String,
    val sender: SenderType,
    val isTyping: Boolean = false,
    val timestamp: Long = System.currentTimeMillis() // 新增时间戳字段，默认为当前时间
)