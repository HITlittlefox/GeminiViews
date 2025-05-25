// JsonParser.kt
package com.example.geminiviews.prompttalk.utils

import com.example.geminiviews.prompttalk.data.JsonSchema
import com.example.geminiviews.prompttalk.data.PetListResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
object JsonParser {

    private val json = Json { ignoreUnknownKeys = true } // 忽略未知字段，增加健壮性

    /**
     * 从原始文本中提取并解析 JSON 数据。
     *
     * @param rawText 包含潜在 JSON 的原始文本。
     * @param jsonSchema 要解析的 JSON 结构类型。
     * @return 如果成功解析，返回解析后的对象（目前是 PetListResponse），否则返回 null。
     * @throws Exception 如果 JSON 提取或解析失败。
     */
    fun parseJsonFromResponse(rawText: String, jsonSchema: JsonSchema): Any? {
        val jsonRegex = "\\{[\\s\\S]*\\}".toRegex()
        val jsonString = jsonRegex.find(rawText)?.value?.trim()

        if (jsonString != null && jsonString.startsWith("{") && jsonString.endsWith("}")) {
            Logger.d("Extracted JSON string: $jsonString")
            return when (jsonSchema) {
                is JsonSchema.PetList -> {
                    json.decodeFromString<PetListResponse>(jsonString)
                }
                // 未来如果添加其他 JsonSchema，可以在这里继续添加 when 分支
                else -> {
                    Logger.w("No parser defined for JsonSchema: ${jsonSchema.description}")
                    null
                }
            }
        } else {
            Logger.e("No valid JSON block found in response for ${jsonSchema.description} request.")
            throw Exception("未能解析${jsonSchema.description}：未找到有效的JSON块。")
        }
    }
}