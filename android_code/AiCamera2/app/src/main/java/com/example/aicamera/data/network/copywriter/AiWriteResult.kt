package com.example.aicamera.data.network.copywriter

/**
 * 对外返回的结果。
 */
data class AiWriteResult(
    val success: Boolean,
    val content: String? = null,
    val errorMessage: String? = null,
) {
    companion object {
        fun success(content: String): AiWriteResult = AiWriteResult(true, content = content)
        fun failure(message: String): AiWriteResult = AiWriteResult(false, errorMessage = message)
    }
}
