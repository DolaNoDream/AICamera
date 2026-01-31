package com.example.aicamera.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 工具类：根据 URL 下载图片到缓存目录，供 UI 加载展示。
 */
object ImageDownloadHelper {
    private const val TAG = "ImageDownloadHelper"

    suspend fun downloadToCache(context: Context, imageUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                requestMethod = "GET"
                doInput = true
            }

            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "下载失败，HTTP ${connection.responseCode}")
                return@withContext null
            }

            val file = File(context.cacheDir, "pose_${System.currentTimeMillis()}.jpg")
            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()

            file
        } catch (e: Exception) {
            Log.e(TAG, "下载图片异常", e)
            null
        }
    }

    suspend fun decodeBitmap(file: File): Bitmap? = withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "解码图片异常", e)
            null
        }
    }
}
