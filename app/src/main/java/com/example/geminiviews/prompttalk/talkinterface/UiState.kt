// UiState.kt
package com.example.geminiviews.prompttalk.talkinterface

import com.example.geminiviews.prompttalk.data.PetListResponse
import kotlinx.serialization.InternalSerializationApi

/**
 * A sealed hierarchy describing the state of the text generation.
 */
sealed interface UiState {

    /**
     * Empty state when the screen is first shown
     */
    object Initial : UiState

    /**
     * Still loading
     */
    object Loading : UiState

    /**
     * Text has been generated
     *
     * 2025/5/24 改造 UiState.Success，增加 currentText 字段
     * 2025/5/25 增加 petList
     *
     */
    @InternalSerializationApi
    data class Success(
        val outputText: String, // AI完整的响应文本
        val currentText: String = "", // 打字机效果当前显示的文本
        val petList: PetListResponse? = null // !!! 新增：用于存储解析后的宠物列表 !!!
    ) : UiState

    /**
     * There was an error generating text
     */
    data class Error(val errorMessage: String) : UiState
}