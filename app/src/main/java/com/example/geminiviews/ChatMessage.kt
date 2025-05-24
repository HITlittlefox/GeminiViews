// ChatMessage.kt
package com.example.geminiviews

enum class SenderType {
    USER, GEMINI
}

data class ChatMessage(
    val content: String,
    val sender: SenderType,
    val isTyping: Boolean = false // 用于指示Gemini是否正在打字
)