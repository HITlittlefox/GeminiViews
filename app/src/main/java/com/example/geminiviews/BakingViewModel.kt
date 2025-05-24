// BakingViewModel.kt (修正后的示例)
package com.example.geminiviews

import android.graphics.Bitmap
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

    fun sendPrompt(
        bitmap: Bitmap, prompt: String
    ) {
        _uiState.value = UiState.Loading // 首先设置为加载状态

        viewModelScope.launch(Dispatchers.IO) {
            try {
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
}