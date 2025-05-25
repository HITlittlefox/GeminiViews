// TalkViewModel.kt
package com.example.geminiviews.prompttalk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminiviews.BuildConfig
import com.example.geminiviews.prompttalk.utils.Logger
import com.example.geminiviews.prompttalk.talkinterface.UiState
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield // 用于在协程被取消时立即让出CPU

// 导入 JSON 序列化库和数据类
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.ExperimentalSerializationApi
import com.example.geminiviews.prompttalk.data.PetListResponse
import com.example.geminiviews.prompttalk.data.JsonSchema

// !!! 导入 JsonParser !!!
import com.example.geminiviews.prompttalk.utils.JsonParser


@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
class TalkViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash", apiKey = BuildConfig.apiKey
    )

    private var currentGenerationJob: Job? = null

    // !!! 新增：当前正在显示的文本，用于精确停止打字机效果 !!!
    private var _currentDisplayedText: String = ""

    // !!! 新增：存储解析后的宠物列表，因为 UiState.Success 可能会多次发送 !!!
    private var _currentParsedPetList: PetListResponse? = null

    fun sendPrompt(userPrompt: String) {
        // 在新的请求开始时，取消旧的协程，重置状态
        currentGenerationJob?.cancel() // 取消旧的 job，以防其仍在运行
        _currentDisplayedText = "" // 重置当前显示文本
        _currentParsedPetList = null // 重置宠物列表

        _uiState.value = UiState.Loading

        currentGenerationJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                Logger.d("Sending prompt to Gemini: '$userPrompt'")

                var jsonSchemaToRequest: JsonSchema? = null
                val lowerCaseUserPrompt = userPrompt.lowercase()
                if (lowerCaseUserPrompt.contains("宠物列表") || lowerCaseUserPrompt.contains("json宠物")) {
                    jsonSchemaToRequest = JsonSchema.PetList
                }

                val response = if (jsonSchemaToRequest != null) {
                    Logger.d("Requesting JSON schema: ${jsonSchemaToRequest.description}")
                    generativeModel.generateContent(content { text(jsonSchemaToRequest.requestPrompt) })
                } else {
                    generativeModel.generateContent(content { text(userPrompt) })
                }

                val rawOutputText = response.text ?: ""
                Logger.d("Received raw response from Gemini: '$rawOutputText'")

                // !!! 调用 JsonParser 进行 JSON 处理 !!!
                if (jsonSchemaToRequest != null) {
                    try {
                        val parsedObject =
                            JsonParser.parseJsonFromResponse(rawOutputText, jsonSchemaToRequest)
                        if (parsedObject is PetListResponse) {
                            _currentParsedPetList = parsedObject
                            Logger.d("Successfully parsed pet list JSON via JsonParser.")
                        } else if (parsedObject == null) {
                            // parseJsonFromResponse 内部会抛出异常，这里仅作日志记录
                            Logger.e("JsonParser returned null for JsonSchema: ${jsonSchemaToRequest.description}")
                            _uiState.value =
                                UiState.Error("未能解析${jsonSchemaToRequest.description}：解析结果为空。")
                            return@launch
                        } else {
                            Logger.w("JsonParser returned unexpected type for JsonSchema: ${jsonSchemaToRequest.description}")
                        }
                    } catch (e: Exception) {
                        Logger.e(
                            "Failed to process JSON with JsonParser: ${e.localizedMessage}",
                            throwable = e
                        )
                        _uiState.value = UiState.Error(e.localizedMessage ?: "JSON处理失败")
                        return@launch
                    }
                }

                val finalOutputText = if (_currentParsedPetList != null) {
                    "成功获取${jsonSchemaToRequest?.description ?: "列表"}，请查看详情。"
                } else {
                    rawOutputText.trim()
                }

                if (finalOutputText.isNotEmpty()) {
                    // 发送一个初始的 Success 状态，currentText 为空，表示即将开始打字机效果。
                    // Activity 会据此添加一个新的 ChatMessage (内容为空或Placeholder)。
                    _uiState.value = UiState.Success(finalOutputText, "", _currentParsedPetList)

                    // 打字机效果
                    for (i in finalOutputText.indices) {
                        // yield() 确保协程可以在此处被取消 (如果用户点击了“停止”按钮)
                        yield()

                        // 更新 _currentDisplayedText
                        _currentDisplayedText = finalOutputText.substring(0, i + 1)
                        _uiState.value = UiState.Success(
                            finalOutputText, _currentDisplayedText, _currentParsedPetList
                        )
                        delay(20)
                    }
                    // 确保最终状态是完整的文本和数据
                    _uiState.value =
                        UiState.Success(finalOutputText, finalOutputText, _currentParsedPetList)

                } else {
                    Logger.e("Gemini response text was null or empty after processing.")
                    _uiState.value = UiState.Error("No response text received.")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Logger.d("Gemini generation job cancelled. Current displayed text: '$_currentDisplayedText'")
                    // 协程被取消时，我们希望UI显示当前已有的内容，而不是Initial或Error
                    // 所以这里不改变 _uiState，让Activity基于 _currentDisplayedText 来显示
                    // Activity 负责在接收到新的请求时清除旧的
                } else {
                    Logger.e("Error calling Gemini API: ${e.localizedMessage}", throwable = e)
                    _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown error occurred")
                }
            } finally {
                currentGenerationJob = null
                _currentDisplayedText = "" // 确保在协程结束后清除
                _currentParsedPetList = null // 确保在协程结束后清除
            }
        }
    }

    // !!! 核心修改：停止打字机效果，并显示当前已有的文本 !!!
    fun stopTypingEffectAndDisplayCurrent() {
        // 如果当前有正在进行的生成任务
        currentGenerationJob?.let { job ->
            if (job.isActive) {
                // 如果 _currentDisplayedText 不为空，则更新 UiState 为当前已显示的内容作为最终内容
                if (_currentDisplayedText.isNotEmpty()) {
                    // 发送一个 UiState.Success，将 currentText 和 outputText 都设为当前已显示的内容
                    // 这样 Activity 就会将这条消息视为最终的完整消息，并更新 UI
                    _uiState.value = UiState.Success(
                        outputText = _currentDisplayedText, // 这里的 outputText 变为实际显示的内容
                        currentText = _currentDisplayedText,
                        petList = _currentParsedPetList // 传递已解析的 petList
                    )
                    Logger.d("Typing effect stopped. Displaying current content: '$_currentDisplayedText'")
                } else {
                    // 如果 _currentDisplayedText 为空 (例如刚开始生成就被停止)，
                    // 可以选择显示一个提示，或者将状态重置为 Initial
                    _uiState.value = UiState.Initial
                    Logger.d("Typing effect stopped, but no content generated yet. Resetting to Initial.")
                }
                // 取消当前的 job，这样打字机效果循环会立即终止
                job.cancel()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentGenerationJob?.cancel()
        Logger.d("ViewModel onCleared: current generation job cancelled.")
    }
}