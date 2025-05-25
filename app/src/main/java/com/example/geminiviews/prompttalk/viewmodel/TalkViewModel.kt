package com.example.geminiviews.prompttalk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminiviews.BuildConfig
import com.example.geminiviews.prompttalk.utils.Logger
import com.example.geminiviews.prompttalk.talkinterface.UiState
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TalkViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash", apiKey = BuildConfig.apiKey
    )

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