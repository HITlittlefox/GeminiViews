// GeminiViewPromptTalkActivity.kt
package com.example.geminiviews.prompttalk.view

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geminiviews.prompttalk.data.ChatMessage
import com.example.geminiviews.prompttalk.utils.Logger
import com.example.geminiviews.R
import com.example.geminiviews.prompttalk.data.SenderType
import com.example.geminiviews.prompttalk.talkinterface.UiState
import com.example.geminiviews.prompttalk.adapter.ChatAdapter
import com.example.geminiviews.prompttalk.viewmodel.TalkViewModel
import kotlinx.coroutines.flow.collectLatest
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin // 如果你使用了表格插件，保留这个导入
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi

@OptIn(
    InternalSerializationApi::class, ExperimentalSerializationApi::class
) // @OptIn 注解，解决 opt-in 警告
class GeminiViewPromptTalkActivity : AppCompatActivity() {

    private lateinit var talkViewModel: TalkViewModel
    private lateinit var messageEditText: EditText
    private lateinit var sendMessageButton: Button // 这将是发送/停止按钮
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()

    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_gemini_image)

        markwon = Markwon.builder(this)
            // .usePlugin(TablePlugin.create(this)) // 如果需要表格支持，取消注释
            .build()

        talkViewModel = ViewModelProvider(this).get(TalkViewModel::class.java)

        // 绑定XML视图组件
        messageEditText = findViewById(R.id.messageEditText)
        sendMessageButton = findViewById(R.id.sendMessageButton)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        val titleTextView: TextView = findViewById(R.id.titleTextView)

        titleTextView.text = getString(R.string.baking_title)

        // 设置聊天RecyclerView的Adapter
        chatAdapter = ChatAdapter(chatMessages, markwon)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

        // !!! 关键更改：发送按钮的点击监听器根据当前状态动态绑定 !!!
        sendMessageButton.setOnClickListener {
            if (sendMessageButton.text == getString(R.string.send_button_send_text)) { // 如果是“发送”状态
                sendMessage()
            } else { // 如果是“停止”状态（即AI正在打字机效果中）
                stopTypingEffectAndDisplayCurrentContent() // 调用新的方法停止打字机效果并显示当前内容
            }
        }

        // 允许通过键盘的发送动作触发消息发送
        messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                if (sendMessageButton.text == getString(R.string.send_button_send_text)) {
                    sendMessage()
                } else {
                    // 如果是停止状态，键盘的发送不应该触发停止，因为停止是按钮行为
                    Toast.makeText(
                        this,
                        "AI is responding, click STOP to show current text.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            } else {
                false
            }
        }

        // 观察ViewModel的UI状态变化
        lifecycleScope.launchWhenStarted {
            talkViewModel.uiState.collectLatest { uiState ->
                Logger.d("Current UI State: ${uiState}")
                when (uiState) {
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        // 当开始加载时，将发送按钮变为“停止”按钮
                        sendMessageButton.text = getString(R.string.send_button_stop_text)
                        sendMessageButton.isEnabled = true // 停止按钮应该是可点击的
                        messageEditText.isEnabled = false // 输入框禁用，防止发送新消息

                        // 添加一个“正在打字”的消息到列表
                        // 确保只添加一次“正在打字”提示，并且在每次新对话开始时清除旧的
                        val typingMessageIndex = chatMessages.indexOfLast { it.isTyping }
                        if (typingMessageIndex == -1) { // 只有没有正在打字的消息时才添加
                            // 在这里添加一个临时的、isTyping为true的Gemini消息
                            chatMessages.add(
                                ChatMessage(
                                    content = "Gemini is typing...", // 初始内容，可以为空字符串或提示语
                                    sender = SenderType.GEMINI, isTyping = true
                                )
                            )
                            chatAdapter.notifyItemInserted(chatMessages.size - 1)
                            chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                        }
                    }

                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        // 恢复发送按钮状态为“发送”
                        sendMessageButton.text = getString(R.string.send_button_send_text)
                        sendMessageButton.isEnabled = true
                        messageEditText.isEnabled = true

                        // 查找最后一条 Gemini 消息的索引
                        // 注意：这里我们查找的是 ChatMessage 对象，而不是仅仅检查 isTyping
                        // 因为 typing message 在收到第一个字时就会转变为非 typing 状态并更新内容
                        val lastGeminiMessageIndex =
                            chatMessages.indexOfLast { it.sender == SenderType.GEMINI }

                        if (uiState.currentText.isNotEmpty()) {
                            // 如果是第一次接收到 AI 内容，或者之前的 AI 消息内容与当前不同 (例如，新一次完整的回复)
                            if (lastGeminiMessageIndex == -1 || chatMessages[lastGeminiMessageIndex].content != uiState.currentText) {
                                // 如果列表为空或者最后一条消息不是 Gemini 消息，或者内容不同（完整回复时）
                                if (lastGeminiMessageIndex == -1 || chatMessages[lastGeminiMessageIndex].sender == SenderType.USER) {
                                    // 添加一个新的消息条目
                                    chatMessages.add(
                                        ChatMessage(
                                            content = uiState.currentText,
                                            sender = SenderType.GEMINI,
                                            isTyping = false, // 此时已经开始显示内容，不是纯粹的 typing 状态
                                            petList = uiState.petList
                                        )
                                    )
                                    chatAdapter.notifyItemInserted(chatMessages.size - 1)
                                    chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                                } else {
                                    // 否则，更新现有的最后一条 Gemini 消息
                                    val existingMessage = chatMessages[lastGeminiMessageIndex]
                                    existingMessage.content = uiState.currentText
                                    existingMessage.isTyping = false // 确保不再是 typing 状态
                                    existingMessage.petList = uiState.petList // 更新 petList
                                    chatAdapter.notifyItemChanged(lastGeminiMessageIndex)
                                    chatRecyclerView.scrollToPosition(chatMessages.size - 1) // 保持滚动到底部
                                }
                            }
                        } else {
                            // 如果 currentText 为空，可能是打字机效果刚开始，或者 ViewModel 在重置
                            // 此时我们确保移除任何“正在打字”的占位符
                            val typingMessageIndex = chatMessages.indexOfLast { it.isTyping }
                            if (typingMessageIndex != -1) {
                                chatMessages.removeAt(typingMessageIndex)
                                chatAdapter.notifyItemRemoved(typingMessageIndex)
                            }
                        }

                        // 智能滚动：只有当用户在底部附近时才自动滚动
                        if (!chatRecyclerView.canScrollVertically(1)) {
                            chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                        }
                    }

                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        // 恢复发送按钮状态
                        sendMessageButton.text = getString(R.string.send_button_send_text)
                        sendMessageButton.isEnabled = true
                        messageEditText.isEnabled = true

                        // 移除“正在打字”的消息（如果存在）
                        val typingMessageIndex = chatMessages.indexOfLast { it.isTyping }
                        if (typingMessageIndex != -1) {
                            chatMessages.removeAt(typingMessageIndex)
                            chatAdapter.notifyItemRemoved(typingMessageIndex)
                        }

                        // 显示错误消息
                        chatMessages.add(
                            ChatMessage(
                                "Error: ${uiState.errorMessage}", SenderType.GEMINI
                            )
                        )
                        chatAdapter.notifyItemInserted(chatMessages.size - 1)
                        chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                    }

                    is UiState.Initial -> {
                        // 当 ViewModel 状态重置为 Initial 时（例如：ViewModel 启动时或用户点击“停止”后没有内容生成时）
                        progressBar.visibility = View.GONE
                        sendMessageButton.text =
                            getString(R.string.send_button_send_text) // 确保是“发送”状态
                        sendMessageButton.isEnabled = true
                        messageEditText.isEnabled = true

                        // 移除任何可能残留的“正在打字”消息或不完整的 AI 消息
                        cleanUpPartialGeminiMessage()
                        Logger.d("UI state reset to Initial, cleaned up partial messages.")
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        val prompt = messageEditText.text.toString().trim()
        if (prompt.isNotEmpty()) {
            // 在发送新消息之前，清除掉之前可能不完整的 Gemini 消息（如果存在）
            cleanUpPartialGeminiMessage()

            // 1. 添加用户消息到列表
            chatMessages.add(ChatMessage(prompt, SenderType.USER))
            chatAdapter.notifyItemInserted(chatMessages.size - 1)
            chatRecyclerView.scrollToPosition(chatMessages.size - 1)

            messageEditText.text.clear()
            talkViewModel.sendPrompt(prompt)
        } else {
            Toast.makeText(this, "Please enter a message.", Toast.LENGTH_SHORT).show()
        }
    }

    // 停止打字机效果并显示当前已生成内容的方法
    private fun stopTypingEffectAndDisplayCurrentContent() {
        talkViewModel.stopTypingEffectAndDisplayCurrent() // 通知 ViewModel
        Toast.makeText(this, "Typing effect stopped.", Toast.LENGTH_SHORT).show()
        Logger.d("User stopped typing effect.")
    }

    // 辅助方法：清除不完整的 Gemini 消息
    private fun cleanUpPartialGeminiMessage() {
        val lastGeminiMessageIndex = chatMessages.indexOfLast { it.sender == SenderType.GEMINI }
        if (lastGeminiMessageIndex != -1) {
            val lastGeminiMessage = chatMessages[lastGeminiMessageIndex]
            // 如果最后一条 Gemini 消息是“正在打字”状态，或者它的内容与 outputText 不符（意味着打字机效果还没完成）
            // 我们需要移除它，因为 ViewModel 会发送一个新的完整消息或者 Initial 状态。
            // 这里的判断需要更精确，考虑到 ViewModel 在 `stopTypingEffectAndDisplayCurrent` 之后会发送
            // `UiState.Success(currentText = "...", outputText = "...")` 且 `currentText == outputText`
            // 所以这里主要处理 `isTyping` 状态
            if (lastGeminiMessage.isTyping) {
                chatMessages.removeAt(lastGeminiMessageIndex)
                chatAdapter.notifyItemRemoved(lastGeminiMessageIndex)
                Logger.d("Cleaned up 'typing' message on new prompt or reset.")
            }
            // 不需要处理非 typing 但不完整的消息，因为 ViewModel 的 UiState.Success 机制会更新它。
        }
    }
}