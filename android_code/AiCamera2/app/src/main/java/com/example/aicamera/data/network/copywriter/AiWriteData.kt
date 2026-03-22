package com.example.aicamera.data.network.copywriter

import com.google.gson.annotations.SerializedName

data class AiWriteData(
    @SerializedName("content") val content: String? = null,
)
