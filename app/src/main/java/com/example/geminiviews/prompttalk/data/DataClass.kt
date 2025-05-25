package com.example.geminiviews.prompttalk.data

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable // 标记为可序列化
data class Pet(
    val type: String, // 种类 (e.g., "狗", "猫")
    val name: String, // 姓名 (e.g., "旺财", "咪咪")
    val hobby: String // 爱好 (e.g., "玩球", "晒太阳")
)

@InternalSerializationApi
@Serializable // 标记为可序列化
data class PetListResponse(
    val pets: List<Pet> // 包含一个宠物列表
)