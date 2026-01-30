package com.example.aicamera.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.CameraControl
import androidx.camera.core.Camera
import java.util.concurrent.TimeUnit
import kotlin.apply
import kotlin.coroutines.resume

/**
 * 摄像头流管理器
 * 负责：开启预览、监听数据流、截取单帧
 */
class CameraStreamManager(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private var imageAnalysis: ImageAnalysis? = null

    // 这是一个"钩子"，当它不为空时，说明有人想要截图
    private var captureCallback: ((Bitmap) -> Unit)? = null

    private var cameraControl: CameraControl? = null

    private var cameraProvider: ProcessCameraProvider? = null

    // 启动摄像头
    fun startCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            this.cameraProvider = cameraProvider

            // 2. 分析配置：建立数据通道
            // STRATEGY_KEEP_ONLY_LATEST: 如果处理不过来，丢弃旧帧，只留最新的
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) // 直接输出 RGBA，方便转 Bitmap
                .build()

            imageAnalysis?.setAnalyzer(executor) { imageProxy ->
                processImageFrame(imageProxy)
            }

            try {
                cameraProvider.unbindAll()
                // 绑定生命周期：只在界面显示时运行摄像头
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    imageAnalysis
                )

                cameraControl = camera.cameraControl
            } catch (e: Exception) {
                Log.e("CameraStream", "绑定失败", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 触发一次中心对焦
     */
    suspend fun triggerAutoFocus() = suspendCoroutine<Boolean> { cont ->
        val control = cameraControl
        if (control == null) {
            cont.resume(false)
            return@suspendCoroutine
        }

        // 创建一个指向画面中心（0.5, 0.5）的测光点
        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        val centerPoint = factory.createPoint(0.5f, 0.5f)

        val action = FocusMeteringAction.Builder(centerPoint)
            .setAutoCancelDuration(3, TimeUnit.SECONDS) // 3秒后取消对焦状态
            .build()

        Log.d("CameraStream", "正在触发中心自动对焦...")
        val future = control.startFocusAndMetering(action)

        future.addListener({
            try {
                val result = future.get()
                // 是否对焦成功
                Log.d("CameraStream", "对焦完成: ${result.isFocusSuccessful}")
                cont.resume(result.isFocusSuccessful)
            } catch (e: Exception) {
                cont.resume(false)
            }
        }, ContextCompat.getMainExecutor(context))
    }


    // 核心逻辑：处理每一帧
    private fun processImageFrame(imageProxy: ImageProxy) {
        val currentCallback = captureCallback

        if (currentCallback != null) {
            // --- 此时说明用户点击了按钮，需要这一帧 ---

            // 1. 将 ImageProxy 转换为 Bitmap
            // CameraX 1.1+ 提供了直接转 Bitmap 的方法，自动处理了 YUV 转换
            val bitmap = imageProxy.toBitmap()

            // 2. 修正旋转角度 (这是新手最容易遇到的坑，图片是歪的)
            val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)

            // 3. 回调给 UI
            currentCallback(rotatedBitmap)

            // 4. 清空回调，表示"截图任务已完成"，后续的帧继续忽略
            captureCallback = null
        }

        // --- 重要：必须关闭每一帧，否则流会卡死 ---
        imageProxy.close()
    }

    // 对外接口：调用此方法获取当前的一帧
    // 使用 suspendCoroutine 将回调转为协程，方便 UI 层调用
    suspend fun captureSingleFrame(): Bitmap = suspendCoroutine { cont ->
        // 设置钩子，下一帧来的时候就会触发 resume
        captureCallback = { bitmap ->
            cont.resume(bitmap)
        }
    }

    // 辅助工具：将 Bitmap 保存为 .jpg 文件 (模拟上传前的准备)
    fun saveBitmapToJpg(bitmap: Bitmap): File {
        val file = File(context.cacheDir, "upload_temp_${System.currentTimeMillis()}.jpg")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream) // 80% 质量压缩
        stream.flush()
        stream.close()
        return file
    }

    // 辅助工具：旋转图片
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    /**
     * 手动释放相机资源
     */
    fun stopCamera() {
        try {
            // 1. 解除所有用例绑定
            cameraProvider?.unbindAll()

            // 2. 清空状态，确保不会发生内存泄漏
            cameraProvider = null
            cameraControl = null
            imageAnalysis = null

            Log.d("CameraStream", "相机已手动关闭并释放资源")
        } catch (e: Exception) {
            Log.e("CameraStream", "手动关闭相机时出错", e)
        }
    }
}