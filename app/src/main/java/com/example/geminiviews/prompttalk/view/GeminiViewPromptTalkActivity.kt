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
import io.noties.markwon.ext.tables.TablePlugin

class GeminiViewPromptTalkActivity : AppCompatActivity() {

    private lateinit var talkViewModel: TalkViewModel
    private lateinit var messageEditText: EditText
    private lateinit var sendMessageButton: Button
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    // !!! 修正：添加 chatAdapter 和 chatMessages 的声明 !!!
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()

    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_gemini_image)

        markwon = Markwon.builder(this)
//            .usePlugin(
//            TablePlugin.create(this), // 添加表格插件
            // ... 其他你需要的插件
//        )
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

        // 设置发送按钮点击事件
        sendMessageButton.setOnClickListener {
            sendMessage()
        }

        // 允许通过键盘的发送动作触发消息发送
        messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage()
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
                        sendMessageButton.isEnabled = false
                        messageEditText.isEnabled = false

                        // 添加一个“正在打字”的消息到列表
                        // 确保只添加一次“正在打字”提示
                        if (chatMessages.none { it.sender == SenderType.GEMINI && it.isTyping }) {
                            chatMessages.add(
                                ChatMessage(
                                    "Gemini is typing...", SenderType.GEMINI, isTyping = true
                                )
                            )
                            chatAdapter.notifyItemInserted(chatMessages.size - 1)
                            // !!! 在 Loading 状态下，如果添加了 typing 消息，滚动到底部
                            chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                        }
                    }

                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        sendMessageButton.isEnabled = true
                        messageEditText.isEnabled = true

                        // 移除“正在打字”的消息（如果存在）
                        val typingMessageIndex =
                            chatMessages.indexOfFirst { it.sender == SenderType.GEMINI && it.isTyping }
                        if (typingMessageIndex != -1) {
                            chatMessages.removeAt(typingMessageIndex)
                            chatAdapter.notifyItemRemoved(typingMessageIndex)
                        }

                        // 更新或添加Gemini的回复
                        // 逻辑：如果最后一条消息是Gemini的（且不是正在打字的消息），则更新它
                        // 否则，添加一条新的Gemini消息
                        if (uiState.currentText.isNotEmpty()) {
                            val lastMessage = chatMessages.lastOrNull()
                            // 确保 lastMessage 不为空且是 Gemini 消息，并且不是 typing 消息（typing 消息已移除）
                            val isLastMessageGemini = lastMessage != null && lastMessage.sender == SenderType.GEMINI

                            if (isLastMessageGemini) {
                                if (lastMessage?.content != uiState.currentText) {
                                    // 更新现有消息，时间戳保持不变或更新为当前时间
                                    chatMessages[chatMessages.size - 1] = ChatMessage(
                                        uiState.currentText,
                                        SenderType.GEMINI,
                                    ) // 确保时间戳保持
                                    chatAdapter.notifyItemChanged(chatMessages.size - 1)

                                    // !!! 智能滚动：只有当用户在底部附近时才自动滚动 !!!
                                    if (!chatRecyclerView.canScrollVertically(1)) { // 如果不能向下滚动（即已经在底部）
                                        chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                                    }
                                }
                                // 如果内容相同，说明是打字机效果的最后一次更新，不需要额外操作
                            } else {
                                // 添加一条新的Gemini消息 (或在Initial状态添加欢迎语)
                                chatMessages.add(
                                    ChatMessage(
                                        uiState.currentText, SenderType.GEMINI
                                    )
                                )
                                chatAdapter.notifyItemInserted(chatMessages.size - 1)

                                // !!! 智能滚动：只有当用户在底部附近时才自动滚动 !!!
                                if (!chatRecyclerView.canScrollVertically(1)) { // 如果不能向下滚动（即已经在底部）
                                    chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                                }
                            }
                        }
                    }

                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        sendMessageButton.isEnabled = true
                        messageEditText.isEnabled = true

                        // 移除“正在打字”的消息（如果存在）
                        val typingMessageIndex =
                            chatMessages.indexOfFirst { it.sender == SenderType.GEMINI && it.isTyping }
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
                        // !!! 错误消息也应该滚动到底部，确保用户看到错误提示
                        chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                    }

                    is UiState.Initial -> {
                        progressBar.visibility = View.GONE
                        sendMessageButton.isEnabled = true
                        messageEditText.isEnabled = true
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        val prompt = messageEditText.text.toString().trim()
        if (prompt.isNotEmpty()) {
            // 1. 添加用户消息到列表
            chatMessages.add(ChatMessage(prompt, SenderType.USER))
            chatAdapter.notifyItemInserted(chatMessages.size - 1)
            // !!! 用户发送消息时，总是滚动到底部 !!!
            chatRecyclerView.scrollToPosition(chatMessages.size - 1)

            messageEditText.text.clear()
            talkViewModel.sendPrompt(prompt)
        } else {
            Toast.makeText(this, "Please enter a message.", Toast.LENGTH_SHORT).show()
        }
    }
}