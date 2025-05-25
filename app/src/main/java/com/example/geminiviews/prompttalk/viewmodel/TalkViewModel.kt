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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 导入 JSON 序列化库
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import com.example.geminiviews.prompttalk.data.PetListResponse
import com.example.geminiviews.prompttalk.data.JsonSchema // !!! 导入 JsonSchema !!!
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi

@OptIn(
    InternalSerializationApi::class, ExperimentalSerializationApi::class
) // @OptIn 注解，解决 opt-in 警告
class TalkViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash", apiKey = BuildConfig.apiKey
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun sendPrompt(userPrompt: String) { // 将参数名改为 userPrompt 更清晰
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Logger.d("Sending prompt to Gemini: '$userPrompt'")

                var jsonSchemaToRequest: JsonSchema? = null
                // 判断用户意图：选择要请求的 JSON 结构
                val lowerCaseUserPrompt = userPrompt.lowercase()
                if (lowerCaseUserPrompt.contains("宠物列表") || lowerCaseUserPrompt.contains("json宠物")) {
                    jsonSchemaToRequest = JsonSchema.PetList
                }
                // 未来可以在这里添加更多判断，例如：
                // else if (lowerCaseUserPrompt.contains("用户资料") || lowerCaseUserPrompt.contains("json用户")) {
                //     jsonSchemaToRequest = JsonSchema.UserProfile
                // }


                val response = if (jsonSchemaToRequest != null) {
                    // 如果识别到 JSON 请求意图，使用对应的 schema 生成 prompt
                    Logger.d("Requesting JSON schema: ${jsonSchemaToRequest.description}")
                    generativeModel.generateContent(content { text(jsonSchemaToRequest.requestPrompt) })
                } else {
                    // 否则，进行普通文本生成
                    generativeModel.generateContent(content { text(userPrompt) })
                }

                val rawOutputText = response.text ?: ""
                Logger.d("Received raw response from Gemini: '$rawOutputText'")

                var outputTextForDisplay: String = rawOutputText
                var parsedPetList: PetListResponse? = null

                if (jsonSchemaToRequest != null) {
                    // 如果之前请求了 JSON，尝试解析
                    try {
                        // 强大的 JSON 提取：查找第一个以 `{` 开头并以 `}` 结尾的 JSON 块
                        // 考虑到模型可能在 JSON 外面包含其他文本或 Markdown 标记
                        val jsonRegex =
                            "\\{.*\\}".toRegex(RegexOption.DOT_MATCHES_ALL) // 匹配任何字符包括换行符
                        val jsonString = jsonRegex.find(rawOutputText)?.value

                        if (jsonString != null) {
                            Logger.d("Extracted JSON string: $jsonString")
                            // 根据请求的 JSON Schema 类型进行反序列化
                            when (jsonSchemaToRequest) {
                                is JsonSchema.PetList -> {
                                    parsedPetList =
                                        json.decodeFromString<PetListResponse>(jsonString)
                                    outputTextForDisplay =
                                        "成功获取${jsonSchemaToRequest.description}，请查看详情。"
                                }
                                // 未来在这里添加其他 JSON Schema 类型的解析
                                // is JsonSchema.UserProfile -> { /* 解析 UserProfile */ }
                            }
                        } else {
                            // 没有找到有效的 JSON 块
                            Logger.e("No valid JSON block found in response for ${jsonSchemaToRequest.description} request.")
                            outputTextForDisplay =
                                "未能解析${jsonSchemaToRequest.description}。原始响应：\n$rawOutputText"
                            _uiState.value =
                                UiState.Error("未能解析${jsonSchemaToRequest.description}：未找到有效的JSON块。")
                            return@launch
                        }

                    } catch (e: Exception) {
                        Logger.e(
                            "Failed to parse JSON for ${jsonSchemaToRequest.description}: ${e.localizedMessage}",
                            throwable = e
                        )
                        outputTextForDisplay =
                            "未能解析${jsonSchemaToRequest.description}。原始响应：\n$rawOutputText"
                        _uiState.value =
                            UiState.Error("未能解析${jsonSchemaToRequest.description}：${e.localizedMessage}")
                        return@launch
                    }
                }

                // 处理文本响应（打字机效果）
                if (outputTextForDisplay.isNotEmpty()) {
                    _uiState.value = UiState.Success(
                        outputText = outputTextForDisplay,
                        currentText = "",
                        petList = parsedPetList // 将解析后的宠物列表传递给 UiState
                    )

                    for (i in outputTextForDisplay.indices) {
                        delay(20)
                        _uiState.value = UiState.Success(
                            outputText = outputTextForDisplay,
                            currentText = outputTextForDisplay.substring(0, i + 1),
                            petList = parsedPetList
                        )
                    }
                    _uiState.value = UiState.Success(
                        outputText = outputTextForDisplay,
                        currentText = outputTextForDisplay,
                        petList = parsedPetList
                    )

                } else {
                    Logger.e("Gemini response text was null or empty.")
                    _uiState.value = UiState.Error("No response text received.")
                }
            } catch (e: Exception) {
                Logger.e("Error calling Gemini API: ${e.localizedMessage}", throwable = e)
                _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
}