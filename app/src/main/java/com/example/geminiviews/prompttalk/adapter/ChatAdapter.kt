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
import com.example.geminiviews.R
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

    // ChatViewHolder 类保持不变
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

        // 1. 处理发送者和时间信息
        handleSenderInfo(holder, message, position)

        // 2. 根据消息类型处理内容显示
        when {
            message.isTyping -> handleTypingMessage(holder)
            message.petList != null && message.petList.pets.isNotEmpty() -> handlePetListMessage(
                holder, message.petList.pets
            )

            else -> handleTextMessage(holder, message.content)
        }

        // 3. 设置气泡背景和文本颜色
        setBubbleStyle(holder, message.sender)
    }

    override fun getItemCount(): Int = messages.size

    // --- 辅助函数：处理不同类型的消息显示逻辑 ---

    private fun handleSenderInfo(holder: ChatViewHolder, message: ChatMessage, position: Int) {
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

    private fun handleTypingMessage(holder: ChatViewHolder) {
        holder.messageText.alpha = 0.7f
        holder.messageText.text = "Gemini is typing..."
        holder.messageText.visibility = View.VISIBLE
        holder.petListTextView.visibility = View.GONE
        // 隐藏其他可能存在的视图，例如图片 ImageView
        // holder.messageImage?.visibility = View.GONE
    }

    private fun handlePetListMessage(holder: ChatViewHolder, pets: List<Pet>) {
        holder.messageText.visibility = View.GONE // 隐藏普通文本
        holder.petListTextView.visibility = View.VISIBLE // 显示宠物列表 TextView
        // holder.messageImage?.visibility = View.GONE // 隐藏其他视图

        val petListString = StringBuilder("宠物列表：\n")
        pets.forEach { pet ->
            petListString.append("- ${pet.type}：${pet.name} (爱好：${pet.hobby})\n")
        }
        holder.petListTextView.text = petListString.toString().trim()
    }

    private fun handleTextMessage(holder: ChatViewHolder, content: String) {
        holder.messageText.alpha = 1.0f
        holder.messageText.visibility = View.VISIBLE // 确保文本可见
        holder.petListTextView.visibility = View.GONE // 隐藏宠物列表
        // holder.messageImage?.visibility = View.GONE // 隐藏其他视图

        markwon.setMarkdown(holder.messageText, content)
    }

    private fun setBubbleStyle(holder: ChatViewHolder, senderType: SenderType) {
        val layoutParams = holder.messageBubble.layoutParams as LinearLayout.LayoutParams
        when (senderType) {
            SenderType.USER -> {
                layoutParams.gravity = Gravity.END
                holder.messageBubble.background =
                    ContextCompat.getDrawable(holder.itemView.context, R.drawable.chat_bubble_user)
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
                layoutParams.gravity = Gravity.START
                holder.messageBubble.background = ContextCompat.getDrawable(
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
        holder.messageBubble.layoutParams = layoutParams
    }

    // --- 辅助格式化函数 (保持不变) ---
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