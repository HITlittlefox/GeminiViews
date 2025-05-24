// ChatAdapter.kt
package com.example.geminiviews

import android.graphics.Color
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon // 导入 Markwon

class ChatAdapter(
    private val messages: MutableList<ChatMessage>, private val markwon: Markwon // 接收 Markwon 实例
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageLayout: LinearLayout =
            view.findViewById(R.id.messageLayout) // item_chat_message.xml 的根布局
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

        // !!! 新增：为 TextView 启用滚动 !!!
        // 只有当文本过长时才启用滚动，并且需要设置 maxLines 或固定高度
        // 如果你在 XML 中已经设置了 maxHeight 和 scrollbars="vertical"，通常不需要再加这一行
        // 但如果文本量真的很大，Markwon可能会影响TextView的默认滚动行为，加上这一行会更保险。
//        holder.messageText.movementMethod = ScrollingMovementMethod()

        // 根据发送者类型调整布局和背景
        val layoutParams = holder.messageText.layoutParams as LinearLayout.LayoutParams
        when (message.sender) {
            SenderType.USER -> {
                // 用户消息：右对齐，蓝色背景
                layoutParams.gravity = Gravity.END
                holder.messageText.background = ContextCompat.getDrawable(
                    holder.itemView.context, R.drawable.background_chat_bubble_user
                )
                holder.messageText.setTextColor(Color.WHITE)
            }

            SenderType.GEMINI -> {
                // Gemini 消息：左对齐，灰色背景
                layoutParams.gravity = Gravity.START
                holder.messageText.background = ContextCompat.getDrawable(
                    holder.itemView.context, R.drawable.background_chat_bubble_gemini
                )
                holder.messageText.setTextColor(Color.BLACK) // Gemini消息文本颜色
            }
        }
        holder.messageText.layoutParams = layoutParams

        // 处理“正在打字”状态
        if (message.isTyping) {
            holder.messageText.alpha = 0.7f // 稍微透明一点
            holder.messageText.text = "Gemini is typing..." // 显示打字提示
        } else {
            holder.messageText.alpha = 1.0f // 完全不透明
        }
    }

    override fun getItemCount(): Int = messages.size
}