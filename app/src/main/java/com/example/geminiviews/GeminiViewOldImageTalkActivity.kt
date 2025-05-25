package com.example.geminiviews

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geminiviews.prompttalk.talkinterface.UiState
import kotlinx.coroutines.flow.collectLatest

class GeminiViewOldImageTalkActivity : AppCompatActivity() {

    private lateinit var bakingViewModel: BakingViewModel
    private lateinit var promptEditText: EditText
    private lateinit var goButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var imageRecyclerView: RecyclerView

    private var selectedImageIndex: Int = 0

    // 与Compose代码中相同的图片资源和描述
    private val images = arrayOf(
        R.drawable.baked_goods_1,
        R.drawable.baked_goods_2,
        R.drawable.baked_goods_3,
    )
    private val imageDescriptions = arrayOf(
        R.string.image1_description,
        R.string.image2_description,
        R.string.image3_description,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_gemini_old_image_talk)

        // 初始化ViewModel
        bakingViewModel = ViewModelProvider(this).get(BakingViewModel::class.java)

        // 绑定XML视图组件
        promptEditText = findViewById(R.id.promptEditText)
        goButton = findViewById(R.id.goButton)
        resultTextView = findViewById(R.id.resultTextView)
        progressBar = findViewById(R.id.progressBar)
        imageRecyclerView = findViewById(R.id.imageRecyclerView)
        val titleTextView: TextView = findViewById(R.id.titleTextView)

        // 设置标题文本
        titleTextView.text = getString(R.string.baking_title)

        // 设置图片选择器的Adapter
        imageRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val imageAdapter = ImageAdapter(images.toList(), imageDescriptions.toList()) { position ->
            selectedImageIndex = position
            // 当图片被选中时，通知Adapter刷新以更新边框
            imageRecyclerView.adapter?.notifyDataSetChanged()
            Toast.makeText(this@GeminiViewOldImageTalkActivity, "${selectedImageIndex}", Toast.LENGTH_SHORT)
                .show()
            promptEditText.setText("${selectedImageIndex}")
        }
        imageRecyclerView.adapter = imageAdapter

        // 设置Go按钮点击事件
        goButton.setOnClickListener {
            val prompt = promptEditText.text.toString()
            if (prompt.isNotEmpty()) {
                val bitmap = BitmapFactory.decodeResource(resources, images[selectedImageIndex])
                bakingViewModel.sendPromptOnlyBaking(bitmap, prompt)
            }
        }

        // 观察ViewModel的UI状态变化
        lifecycleScope.launchWhenStarted {
            bakingViewModel.uiState.collectLatest { uiState ->
//                Log.e("gemini", uiState.toString())
                Log.d("gemini", "Current UI State: ${uiState}") // 添加这行
                when (uiState) {
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        resultTextView.visibility = View.GONE
                        goButton.isEnabled = false // 禁用按钮防止重复点击
                        resultTextView.text = "" // 清空之前的文本
                    }

                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        resultTextView.visibility = View.VISIBLE
                        // !!! 关键更改：显示 currentText 而不是 outputText
                        resultTextView.text = uiState.currentText
                        resultTextView.setTextColor(
                            ContextCompat.getColor(
                                this@GeminiViewOldImageTalkActivity, android.R.color.black
                            )
                        )
                        goButton.isEnabled = true
                    }

                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        resultTextView.visibility = View.VISIBLE
                        resultTextView.text = uiState.errorMessage
                        resultTextView.setTextColor(
                            ContextCompat.getColor(
                                this@GeminiViewOldImageTalkActivity, android.R.color.holo_red_dark
                            )
                        )
                        goButton.isEnabled = true
                    }

                    is UiState.Initial -> {
                        // 初始状态，可能不需要做特殊处理
                        progressBar.visibility = View.GONE
                        resultTextView.visibility = View.VISIBLE
                        resultTextView.text = getString(R.string.results_placeholder)
                        resultTextView.setTextColor(
                            ContextCompat.getColor(
                                this@GeminiViewOldImageTalkActivity, android.R.color.black
                            )
                        )
                        goButton.isEnabled = true
                    }
                }
            }
        }

        // 初始设置 promptEditText 的文本
        promptEditText.setText(getString(R.string.prompt_placeholder))
        resultTextView.text = getString(R.string.results_placeholder)
    }

    // RecyclerView Adapter
    inner class ImageAdapter(
        private val imageResIds: List<Int>,
        private val imageDescriptions: List<Int>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imageView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            holder.imageView.setImageResource(imageResIds[position])
            holder.imageView.contentDescription =
                holder.itemView.context.getString(imageDescriptions[position])

            // 更新边框
            if (position == selectedImageIndex) {
                // 设置一个边框，可以是一个GradientDrawable或者简单的background drawable
                val borderDrawable = GradientDrawable()
                borderDrawable.setStroke(
                    4.dpToPx(), ContextCompat.getColor(
                        holder.itemView.context,
                        com.google.android.material.R.color.design_default_color_primary
                    )
                )
                borderDrawable.setColor(Color.TRANSPARENT) // 透明背景
                holder.imageView.background = borderDrawable
            } else {
                holder.imageView.background = null // 移除边框
            }

            holder.imageView.setOnClickListener {
                onItemClick(position)
            }
        }

        override fun getItemCount(): Int = imageResIds.size
    }

    // 辅助函数：dp转像素
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density + 0.5f).toInt()
    }
}