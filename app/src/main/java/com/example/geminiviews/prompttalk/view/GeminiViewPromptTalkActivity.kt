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
    private lateinit var sendMessageButton: Button
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    // !!! 修正：添加 chatAdapter 和 chatMessages 的声明 !!!
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()

    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_gemini_image) // 确认你的布局文件名称

        markwon = Markwon.builder(this)
            // .usePlugin(TablePlugin.create(this)) // 如果需要表格支持，取消注释
            .build()

        talkViewModel = ViewModelProvider(this).get(TalkViewModel::class.java)

        // 绑定XML视图组件
        messageEditText = findViewById(R.id.messageEditText)
        sendMessageButton = findViewById(R.id.sendMessageButton)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        val titleTextView: TextView = findViewById(R.id.titleTextView) // 确保你的布局中有这个ID

        titleTextView.text = getString(R.string.baking_title) // 确保你的 string 资源有 baking_title

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
                        // 使用 indexOfLast 找到最后一条打字消息，更稳妥
                        val typingMessageIndex = chatMessages.indexOfLast { it.isTyping }
                        if (typingMessageIndex != -1) {
                            chatMessages.removeAt(typingMessageIndex)
                            chatAdapter.notifyItemRemoved(typingMessageIndex)
                        }

                        // 查找最后一条非 typing 的 Gemini 消息的索引
                        val lastGeminiMessageIndex =
                            chatMessages.indexOfLast { it.sender == SenderType.GEMINI && !it.isTyping }

                        // 核心逻辑：判断是添加新消息还是更新现有消息
                        // 只有当 currentText 不为空时才处理消息更新/添加，因为空字符串通常不是有效回复
                        if (uiState.currentText.isNotEmpty()) {
                            // 如果是打字机效果的第一次更新，或者没有上一条 Gemini 消息，就添加新消息
                            if (lastGeminiMessageIndex == -1 || chatMessages[lastGeminiMessageIndex].content == uiState.outputText) {
                                // 添加新消息
                                chatMessages.add(
                                    ChatMessage(
                                        content = uiState.currentText,
                                        sender = SenderType.GEMINI,
                                        petList = uiState.petList // !!! 关键：将 petList 传递给新消息 !!!
                                    )
                                )
                                chatAdapter.notifyItemInserted(chatMessages.size - 1)
                            } else {
                                // 否则，更新现有消息 (打字机效果进行中)
                                val existingMessage = chatMessages[lastGeminiMessageIndex]
                                // 仅当内容发生变化时才更新，避免不必要的 UI 刷新
                                if (existingMessage.content != uiState.currentText) {
                                    existingMessage.content = uiState.currentText
                                    // 只有当 currentText 达到 outputText 时，才更新 petList
                                    // 这确保了 petList 仅在最终的完整回复时被设置，避免在打字过程中频繁替换对象
                                    if (uiState.currentText == uiState.outputText) {
                                        chatMessages[lastGeminiMessageIndex] = existingMessage.copy(
                                            petList = uiState.petList // !!! 关键：将 petList 传递给最终更新的消息 !!!
                                        )
                                    }
                                    chatAdapter.notifyItemChanged(lastGeminiMessageIndex)
                                }
                            }
                        }

                        // 智能滚动：只有当用户在底部附近时才自动滚动
                        // 如果不能向下滚动（即已经在底部）
                        if (!chatRecyclerView.canScrollVertically(1)) {
                            chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                        }
                    }

                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        sendMessageButton.isEnabled = true
                        messageEditText.isEnabled = true

                        // 移除“正在打字”的消息（如果存在）
                        val typingMessageIndex =
                            chatMessages.indexOfLast { it.isTyping } // 使用 indexOfLast，更稳妥地找到最新的打字消息
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