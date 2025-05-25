// ChatAdapter.kt
package com.example.geminiviews.prompttalk.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.geminiviews.prompttalk.data.ChatMessage
import com.example.geminiviews.R // 确保 R 导入正确
import com.example.geminiviews.prompttalk.data.SenderType
import com.example.geminiviews.prompttalk.data.Pet
import io.noties.markwon.Markwon
import kotlinx.serialization.InternalSerializationApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@InternalSerializationApi
class ChatAdapter(
    private val messages: MutableList<ChatMessage>, private val markwon: Markwon
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chatItemContainer: LinearLayout = view.findViewById(R.id.chat_item_container)
        val senderInfoText: TextView = view.findViewById(R.id.senderInfoText)
        val messageText: TextView = view.findViewById(R.id.messageText)
        val messageBubble: LinearLayout = view.findViewById(R.id.message_bubble)
        val petListTextView: TextView = view.findViewById(R.id.petListTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        // 处理“正在打字”状态优先
        if (message.isTyping) {
            holder.messageText.alpha = 0.7f // 使文本变淡
            holder.messageText.text = "Gemini is typing..." // 直接设置文本
            holder.petListTextView.visibility = View.GONE // 正在打字时隐藏宠物列表
        } else {
            // 不是“正在打字”状态时才渲染 Markdown
            holder.messageText.alpha = 1.0f // 恢复正常透明度
            if (message.content.isNotEmpty()) {
                markwon.setMarkdown(holder.messageText, message.content)
            } else {
                holder.messageText.text = "" // 如果内容为空，则清空
            }

            // 处理宠物列表显示 (仅当不是“正在打字”状态时才显示)
            if (message.petList != null && message.petList!!.pets.isNotEmpty()) {
                holder.petListTextView.visibility = View.VISIBLE
                val petsText = message.petList!!.pets.joinToString("\n") { pet ->
                    " - 类型: ${pet.type}, 名字: ${pet.name}, 爱好: ${pet.hobby}"
                }
                holder.petListTextView.text = "宠物列表:\n$petsText"
            } else {
                holder.petListTextView.visibility = View.GONE
            }
        }

        // --- 核心修改：调整 messageBubble 和 senderInfoText 的 gravity ---
        // 获取 message_bubble 的 LayoutParams
        val messageBubbleLayoutParams =
            holder.messageBubble.layoutParams as LinearLayout.LayoutParams
        // 获取 senderInfoText 的 LayoutParams
        val senderInfoTextLayoutParams =
            holder.senderInfoText.layoutParams as LinearLayout.LayoutParams

        when (message.sender) {
            SenderType.USER -> {
                messageBubbleLayoutParams.gravity = Gravity.END
                senderInfoTextLayoutParams.gravity = Gravity.END // !!! 让 senderInfoText 靠右 !!!
                holder.messageBubble.background = ContextCompat.getDrawable(
                    holder.itemView.context, R.drawable.chat_bubble_user
                )
                holder.messageText.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context, android.R.color.white
                    )
                )
                holder.petListTextView.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context, android.R.color.white
                    )
                )
            }

            SenderType.GEMINI -> {
                messageBubbleLayoutParams.gravity = Gravity.START
                senderInfoTextLayoutParams.gravity = Gravity.START // !!! 让 senderInfoText 靠左 !!!
                holder.messageBubble.background = ContextCompat.getDrawable( // <-- 修正此处
                    holder.itemView.context, R.drawable.chat_bubble_gemini
                )
                holder.messageText.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context, android.R.color.black
                    )
                )
                holder.petListTextView.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context, android.R.color.black
                    )
                )
            }
        }
        // 应用新的 LayoutParams 到 message_bubble
        holder.messageBubble.layoutParams = messageBubbleLayoutParams
        // !!! 应用新的 LayoutParams 到 senderInfoText !!!
        holder.senderInfoText.layoutParams = senderInfoTextLayoutParams


        // 处理 senderInfoText 的显示逻辑 (保持不变)
        val senderName = getSenderName(message.sender)
        if (position == 0) {
            holder.senderInfoText.text = "$senderName ${formatTimestamp(message.timestamp)}"
            holder.senderInfoText.visibility = View.VISIBLE
        } else {
            val previousMessage = messages[position - 1]
            val shouldDisplayInfo =
                (message.timestamp - previousMessage.timestamp > 5 * 60 * 1000) || (!isSameDay(
                    message.timestamp, previousMessage.timestamp
                )) || (message.sender != previousMessage.sender)

            if (shouldDisplayInfo) {
                holder.senderInfoText.text = "$senderName ${formatTimestamp(message.timestamp)}"
                holder.senderInfoText.visibility = View.VISIBLE
            } else {
                holder.senderInfoText.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    // ... (辅助函数保持不变)
    private fun formatTimestamp(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        return dateFormat.format(Date(timestamp1)) == dateFormat.format(Date(timestamp2))
    }

    private fun getSenderName(senderType: SenderType): String {
        return when (senderType) {
            SenderType.USER -> "You"
            SenderType.GEMINI -> "Gemini"
        }
    }
}