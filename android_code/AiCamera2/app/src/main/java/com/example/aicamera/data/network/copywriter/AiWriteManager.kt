package com.example.aicamera.data.network.copywriter

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * AI 成文（文案生成）核心管理类。
 *
 * 接口：POST http://1.95.125.238:9001/ai/write
 * multipart/form-data:
 * - sessionId: string
 * - image: file[] (同名多 part)
 * - requirement: json string (可选)
 */
class AiWriteManager private constructor(private val appContext: Context) {

    interface AiWriteCallback {
        fun onResult(result: AiWriteResult)
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val gson: Gson = Gson()

    /**
     * 以 File 列表上传（多图）。
     */
    fun writeWithFiles(
        sessionId: String,
        imageFiles: List<File>,
        requirement: CopywriterRequirement? = null,
        callback: AiWriteCallback,
    ) {
        val check = checkFileParams(sessionId, imageFiles)
        if (check != null) {
            callback.onResult(AiWriteResult.failure(check))
            return
        }

        val multipartBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("sessionId", sessionId)

        // 关键点：requirement 必须作为 JSON 字符串发送（而不是数组/多 part）
        if (requirement != null) {
            multipartBuilder.addFormDataPart("requirement", gson.toJson(requirement))
        }

        imageFiles.forEach { file ->
            val mediaType = guessMediaTypeFromFileName(file.name)
            multipartBuilder.addFormDataPart(
                "image",
                file.name,
                file.asRequestBody(mediaType),
            )
        }

        val request = Request.Builder()
            .url(BASE_URL + WRITE_API_PATH)
            .post(multipartBuilder.build())
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(AiWriteResult.failure("网络连接失败：${e.message}"))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback.onResult(AiWriteResult.failure("接口响应失败，状态码：${it.code}"))
                        return
                    }

                    val body = it.body?.string()
                    if (body.isNullOrBlank()) {
                        callback.onResult(AiWriteResult.failure("接口响应体为空"))
                        return
                    }

                    callback.onResult(parseResponse(body))
                }
            }
        })
    }

    /**
     * 以 Uri 列表上传（多图）。内部会拷贝到 cache 临时文件后再走 writeWithFiles。
     */
    fun writeWithUris(
        sessionId: String,
        imageUris: List<Uri>,
        requirement: CopywriterRequirement? = null,
        callback: AiWriteCallback,
    ) {
        val check = checkUriParams(sessionId, imageUris)
        if (check != null) {
            callback.onResult(AiWriteResult.failure(check))
            return
        }

        try {
            val tempFiles = imageUris.mapIndexed { index, uri ->
                copyUriToCacheFile(appContext, uri, prefix = "ai_write_${sessionId}_${index}_")
            }
            writeWithFiles(sessionId, tempFiles, requirement, object : AiWriteCallback {
                override fun onResult(result: AiWriteResult) {
                    tempFiles.forEach { runCatching { it.delete() } }
                    callback.onResult(result)
                }
            })
        } catch (e: Exception) {
            callback.onResult(AiWriteResult.failure("读取图片失败：${e.message}"))
        }
    }

    private fun parseResponse(json: String): AiWriteResult {
        return try {
            val root = JsonParser.parseString(json).asJsonObject

            val code = root.get("code")?.asInt ?: -1
            val msg = root.get("msg")?.asString

            if (code != 200) {
                return AiWriteResult.failure(msg ?: "接口返回异常")
            }

            val dataObj = root.getAsJsonObject("data")
            val contentEl = dataObj?.get("content")

            val content = extractContentAsString(contentEl)
            if (content.isNullOrBlank()) {
                AiWriteResult.failure("接口返回 content 为空")
            } else {
                AiWriteResult.success(content)
            }
        } catch (e: Exception) {
            AiWriteResult.failure("响应解析失败：${e.message}")
        }
    }

    private fun extractContentAsString(contentEl: JsonElement?): String? {
        if (contentEl == null || contentEl.isJsonNull) return null

        return when {
            contentEl.isJsonPrimitive -> {
                // string/number/bool 都转成 string
                contentEl.asString
            }

            contentEl.isJsonArray -> {
                // 后端有时会返回数组（比如分段文案），这里做兼容拼接
                contentEl.asJsonArray
                    .mapNotNull { el ->
                        when {
                            el == null || el.isJsonNull -> null
                            el.isJsonPrimitive -> el.asString
                            else -> el.toString()
                        }
                    }
                    .joinToString(separator = "\n")
            }

            else -> {
                // object 等其他类型，直接 toString（保证不崩）
                contentEl.toString()
            }
        }
    }

    private fun checkFileParams(sessionId: String, imageFiles: List<File>?): String? {
        if (sessionId.isBlank()) return "sessionId不能为空"
        if (imageFiles.isNullOrEmpty()) return "至少选择一张图片"
        if (imageFiles.any { !it.exists() }) return "存在图片文件不存在"
        return null
    }

    private fun checkUriParams(sessionId: String, imageUris: List<Uri>?): String? {
        if (sessionId.isBlank()) return "sessionId不能为空"
        if (imageUris.isNullOrEmpty()) return "至少选择一张图片"
        return null
    }

    private fun copyUriToCacheFile(context: Context, uri: Uri, prefix: String): File {
        val resolver = context.contentResolver

        // 尽量拿到原始文件名（包含扩展名），否则用 mime 推断扩展名，避免后端按扩展名校验失败。
        val displayName = queryDisplayName(resolver, uri)
        val mimeType = resolver.getType(uri)
        val ext = fileExtensionFromDisplayName(displayName)
            ?: extensionFromMime(mimeType)
            ?: "jpg" // 兜底，避免无扩展名

        val safeExt = ext.lowercase(Locale.US).trimStart('.')
        val outFile = File(context.cacheDir, "$prefix$imageStubName.$safeExt")

        resolver.openInputStream(uri).use { input ->
            if (input == null) throw IOException("无法打开Uri输入流")
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
        return outFile
    }

    private fun queryDisplayName(resolver: android.content.ContentResolver, uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    private fun fileExtensionFromDisplayName(displayName: String?): String? {
        val name = displayName ?: return null
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.length - 1) return null
        return name.substring(dot + 1)
    }

    private fun extensionFromMime(mimeType: String?): String? {
        return when (mimeType?.lowercase(Locale.US)) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            else -> null
        }
    }

    private fun guessMediaTypeFromFileName(fileName: String): MediaType? {
        val lower = fileName.lowercase(Locale.US)
        val mime = when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".heic") -> "image/heic"
            lower.endsWith(".heif") -> "image/heif"
            else -> "image/*"
        }
        return mime.toMediaTypeOrNull()
    }

    // 旧的强类型响应体保留也可以，但已不再用于解析，避免 content 类型变化导致崩溃
    @Suppress("unused")
    private data class AiWriteApiResponse(
        val code: Int = -1,
        val msg: String? = null,
        val data: AiWriteData? = null,
    )

    companion object {
        private const val BASE_URL = "http://1.95.125.238:9001"
        private const val WRITE_API_PATH = "/ai/write"
        private const val imageStubName = "image"

        @Volatile private var instance: AiWriteManager? = null

        fun getInstance(context: Context): AiWriteManager {
            return instance ?: synchronized(this) {
                instance ?: AiWriteManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
