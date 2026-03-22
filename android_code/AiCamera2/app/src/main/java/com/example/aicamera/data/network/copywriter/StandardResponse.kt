package com.example.aicamera.data.network.copywriter

import com.google.gson.annotations.SerializedName

/**
 * 通用响应格式： { "code": 200, "msg": "success", "data": { ... } }
 */
data class StandardResponse<T>(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("msg") val msg: String? = null,
    @SerializedName("data") val data: T? = null,
)
