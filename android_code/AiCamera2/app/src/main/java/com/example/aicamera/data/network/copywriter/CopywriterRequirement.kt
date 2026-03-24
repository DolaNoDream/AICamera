package com.example.aicamera.data.network.copywriter

/**
 * AI 成文接口 requirement 参数。
 *
 * 该对象会被序列化成 JSON 字符串，通过 multipart 的 form-data 字段 `requirement` 发送。
 */
data class CopywriterRequirement(
    val type: String? = null,
    val emotion: String? = null,
    val theme: String? = null,
    val style: String? = null,
    val length: String? = null,
    val special: String? = null,
    val custom: String? = null,
)
