// BakingViewModel.kt (修正后的示例)
package com.example.geminiviews

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BakingViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash", apiKey = BuildConfig.apiKey
    )

    fun sendPromptOnlyBaking(
        bitmap: Bitmap, prompt: String
    ) {
        _uiState.value = UiState.Loading // 首先设置为加载状态

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // --- 使用 Logger 打印 ---
                Logger.d("Sending prompt to Gemini: '$prompt'")
                Logger.d("Image attached (Dimensions: ${bitmap.width}x${bitmap.height})")
                // --------------------------

                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    })
                response.text?.let { outputContent ->
                    // 仅在开始打字前设置一次 currentText 为空
                    // 这样打字效果完成后，_uiState.value 就会停留在最后的完整文本状态
                    _uiState.value = UiState.Success(outputContent, "")

                    for (i in outputContent.indices) {
                        delay(20) // 每个字符之间的延迟
                        // 每次迭代更新 currentText，直到完整
                        _uiState.value =
                            UiState.Success(outputContent, outputContent.substring(0, i + 1))
                    }
                    // 循环结束后，_uiState.value 已经包含了完整的 outputContent 作为 currentText
                    // ！！！ 这里不应该再有任何将 currentText 设置为空的语句 ！！！

                } ?: run {
                    _uiState.value = UiState.Error("No response text received.")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }

    fun sendPromptWithBitMapAndPrompt(
        bitmap: Bitmap, prompt: String
    ) {
        _uiState.value = UiState.Loading // 首先设置为加载状态

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // --- 使用 Logger 打印 ---
                Logger.d("Sending prompt to Gemini: '$prompt'")
                Logger.d("Image attached (Dimensions: ${bitmap.width}x${bitmap.height})")
                // --------------------------

                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    })
                response.text?.let { outputContent ->
                    // --- 使用 Logger 打印 ---
                    Logger.d("Received full response from Gemini: '$outputContent'")
                    // --------------------------------

                    // 发送 Success 状态，包含完整内容和当前打字进度（初始为空）
                    // MainActivity 会根据这个更新最后一条消息或添加新消息
                    _uiState.value = UiState.Success(outputContent, "")

                    for (i in outputContent.indices) {
                        delay(20) // 每个字符之间的延迟
                        // 每次迭代更新 currentText
                        // 这里只需要发送状态，MainActivity 会负责更新RecyclerView
                        _uiState.value =
                            UiState.Success(outputContent, outputContent.substring(0, i + 1))
                    }
                    // 确保循环结束后，最终状态是完整的文本
                    _uiState.value = UiState.Success(outputContent, outputContent)

                } ?: run {
                    Logger.e("Gemini response text was null.")

                    _uiState.value = UiState.Error("No response text received.")
                }
            } catch (e: Exception) {
                Logger.e(
                    "Error calling Gemini API: ${e.localizedMessage}", throwable = e
                ) // 可以传递 Throwable

                _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }

    // 关键更改：sendPrompt 现在只接受 prompt 字符串
    fun sendPrompt(
        prompt: String // 移除 bitmap 参数
    ) {
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Logger.d("Sending prompt to Gemini: '$prompt'")
                // 移除图片相关日志
                // Logger.d("Image attached (Dimensions: ${bitmap.width}x${bitmap.height})")

                // 关键更改：generateContent 只传入文本内容
                val response = generativeModel.generateContent(
                    content {
                        text(prompt) // 只传入文本
                    })

                response.text?.let { outputContent ->
                    Logger.d("Received full response from Gemini: '$outputContent'")

                    _uiState.value = UiState.Success(outputContent, "")

                    for (i in outputContent.indices) {
                        delay(20)
                        _uiState.value =
                            UiState.Success(outputContent, outputContent.substring(0, i + 1))
                    }
                    _uiState.value = UiState.Success(outputContent, outputContent)

                } ?: run {
                    Logger.e("Gemini response text was null.")
                    _uiState.value = UiState.Error("No response text received.")
                }
            } catch (e: Exception) {
                Logger.e("Error calling Gemini API: ${e.localizedMessage}", throwable = e)
                _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
}