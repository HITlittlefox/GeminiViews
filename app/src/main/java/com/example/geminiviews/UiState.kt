package com.example.geminiviews

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
     *
     */
    data class Success(val outputText: String, val currentText: String = "") : UiState


    /**
     * There was an error generating text
     */
    data class Error(val errorMessage: String) : UiState
}