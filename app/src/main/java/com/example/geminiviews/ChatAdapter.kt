// ChatAdapter.kt
package com.example.geminiviews

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val messages: MutableList<ChatMessage>, private val markwon: Markwon
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chatItemContainer: LinearLayout = view.findViewById(R.id.chat_item_container)
        val senderInfoText: TextView = view.findViewById(R.id.senderInfoText) // 引用新的ID
        val messageText: TextView = view.findViewById(R.id.messageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        // 设置消息文本并应用 Markdown
        markwon.setMarkdown(holder.messageText, message.content)

        val containerLayoutParams =
            holder.chatItemContainer.layoutParams as ViewGroup.MarginLayoutParams
        val messageLayoutParams = holder.messageText.layoutParams as LinearLayout.LayoutParams
        val senderInfoLayoutParams = holder.senderInfoText.layoutParams as LinearLayout.LayoutParams

        // 根据发送者类型调整整个消息项的 gravity
        when (message.sender) {
            SenderType.USER -> {
                holder.chatItemContainer.gravity = Gravity.END // 整个容器右对齐
                messageLayoutParams.gravity = Gravity.END // 消息气泡在容器内右对齐
                senderInfoLayoutParams.gravity = Gravity.END // 信息文本在容器内右对齐

                holder.messageText.background = ContextCompat.getDrawable(
                    holder.itemView.context, R.drawable.background_chat_bubble_user
                )
                holder.messageText.setTextColor(Color.WHITE)
            }

            SenderType.GEMINI -> {
                holder.chatItemContainer.gravity = Gravity.START // 整个容器左对齐
                messageLayoutParams.gravity = Gravity.START // 消息气泡在容器内左对齐
                senderInfoLayoutParams.gravity = Gravity.START // 信息文本在容器内左对齐

                holder.messageText.background = ContextCompat.getDrawable(
                    holder.itemView.context, R.drawable.background_chat_bubble_gemini
                )
                holder.messageText.setTextColor(Color.BLACK)
            }
        }
        holder.messageText.layoutParams = messageLayoutParams // 确保更新 layoutParams
        holder.senderInfoText.layoutParams = senderInfoLayoutParams // 确保更新 layoutParams

        // !!! 设置发送者信息和时间戳 !!!
        holder.senderInfoText.visibility = View.GONE // 默认隐藏

        // 获取发送者名称字符串
        val senderName = when (message.sender) {
            SenderType.USER -> holder.itemView.context.getString(R.string.sender_you)
            SenderType.GEMINI -> holder.itemView.context.getString(R.string.sender_gemini)
        }

        // 智能显示信息文本的逻辑
        if (position == 0) { // 第一条消息总是显示
            holder.senderInfoText.text = "$senderName ${formatTimestamp(message.timestamp)}"
            holder.senderInfoText.visibility = View.VISIBLE
        } else {
            val previousMessage = messages[position - 1]
            // 如果与上一条消息的间隔超过 5 分钟，或者日期不同，或者发送者类型改变，则显示
            val shouldDisplayInfo =
                (message.timestamp - previousMessage.timestamp > 5 * 60 * 1000) || (!isSameDay(
                    message.timestamp, previousMessage.timestamp
                )) || (message.sender != previousMessage.sender) // 关键：如果发送者改变，也显示信息

            if (shouldDisplayInfo) {
                holder.senderInfoText.text = "$senderName ${formatTimestamp(message.timestamp)}"
                holder.senderInfoText.visibility = View.VISIBLE
            }
        }

        // 处理“正在打字”状态 (保持不变)
        if (message.isTyping) {
            holder.messageText.alpha = 0.7f // 使文本变淡
            holder.messageText.text = "Gemini is typing..."
        } else {
            holder.messageText.alpha = 1.0f // 恢复正常透明度
        }
    }

    override fun getItemCount(): Int = messages.size

    // 辅助函数：格式化时间戳
    private fun formatTimestamp(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    // 辅助函数：判断是否是同一天
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        return dateFormat.format(Date(timestamp1)) == dateFormat.format(Date(timestamp2))
    }
}