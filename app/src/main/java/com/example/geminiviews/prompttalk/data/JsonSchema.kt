package com.example.geminiviews.prompttalk.data

// JsonSchema.kt

sealed class JsonSchema {
    abstract val requestPrompt: String // 获取请求该JSON结构的Prompt
    abstract val description: String // 对该JSON结构的描述，用于提示用户或内部识别

    // 宠物列表 JSON 结构
    data object PetList : JsonSchema() {
        override val description: String = "宠物列表"
        override val requestPrompt: String = """
            请提供一个包含至少3个宠物的JSON列表。每个宠物应包含以下字段：
            - "type": 宠物的种类 (字符串，例如 "狗", "猫", "鹦鹉")
            - "name": 宠物的名称 (字符串)
            - "hobby": 宠物的爱好 (字符串)
            
            请确保输出是纯JSON格式，不包含任何额外文本或Markdown。
            示例：
            ```json
            {
              "pets": [
                {
                  "type": "狗",
                  "name": "旺财",
                  "hobby": "玩球"
                },
                {
                  "type": "猫",
                  "name": "咪咪",
                  "hobby": "晒太阳"
                }
              ]
            }
            ```
            请返回符合此结构的JSON。
        """.trimIndent()
    }

    // 未来可以添加其他 JSON 结构，例如：
    /*
    data object UserProfile : JsonSchema() {
        override val description: String = "用户个人资料"
        override val requestPrompt: String = """
            请提供一个包含用户个人资料的JSON对象。字段包括：
            - "name": 用户的全名 (字符串)
            - "age": 用户的年龄 (整数)
            - "email": 用户的电子邮件地址 (字符串)
            - "interests": 用户的兴趣列表 (字符串数组)

            请确保输出是纯JSON格式，不包含任何额外文本或Markdown。
            示例：
            ```json
            {
              "name": "张三",
              "age": 30,
              "email": "zhangsan@example.com",
              "interests": ["编程", "阅读", "旅行"]
            }
            ```
            请返回符合此结构的JSON。
        """.trimIndent()
    }
    */
}