package com.example.aicamera.ui

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.aicamera.camera.CameraController
import com.example.aicamera.camera.CameraStreamManager
import com.example.aicamera.network.PoseRecommendationClient
import com.example.aicamera.network.model.PoseResponse
import com.example.aicamera.storage.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.aicamera.STT.SparkAsrManager; //导入STT模块
import com.example.aicamera.TTS.TTSManager

/**
 * 相机页面 ViewModel
 * 职责：管理相机页面的状态和业务逻辑
 */
class CameraViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "CameraViewModel"
    }

    // 依赖注入
    val cameraController = CameraController(application)
    val fileManager = FileManager(application)

    // UI 状态管理
    private val _uiState = MutableStateFlow<CameraUIState>(CameraUIState.Idle)
    val uiState: StateFlow<CameraUIState> = _uiState.asStateFlow()

    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * AI 相关数据存储（预留接口）
     *
     * 扩展点：
     * - aiAdvice 用于存储后端 AI 返回的建议（构图、姿势等）
     * - 在接收到 AI 响应时更新此字段
     * - UI 监听此字段变化，实时显示建议
     */
    private val _aiAdvice = MutableStateFlow<String>("")
    val aiAdvice: StateFlow<String> = _aiAdvice.asStateFlow()

    // AI姿势指导相关状态
    private val poseClient = PoseRecommendationClient.getInstance()
    private val cameraStreamManager = CameraStreamManager(application)
    private var isStreamCameraStarted = false

    private val _poseGuideText = MutableStateFlow("")
    val poseGuideText: StateFlow<String> = _poseGuideText.asStateFlow()

    private val _poseSuggestionText = MutableStateFlow("")
    val poseSuggestionText: StateFlow<String> = _poseSuggestionText.asStateFlow()

    private val _poseImageUrl = MutableStateFlow("")
    val poseImageUrl: StateFlow<String> = _poseImageUrl.asStateFlow()

    private val _poseLoading = MutableStateFlow(false)
    val poseLoading: StateFlow<Boolean> = _poseLoading.asStateFlow()

    private val _poseErrorMessage = MutableStateFlow<String?>(null)
    val poseErrorMessage: StateFlow<String?> = _poseErrorMessage.asStateFlow()

    /**
     * 语音播报开关状态（预留接口）
     *
     * 扩展点：
     * - 用于控制是否启用语音播报
     * - 当此值为 true 且有新的 AI 建议时，触发 playVoiceAdvice()
     */
    private val _voiceGuideEnabled = MutableStateFlow(false)
    val voiceGuideEnabled: StateFlow<Boolean> = _voiceGuideEnabled.asStateFlow()

    private val _lastUserIntent = MutableStateFlow("")
    val lastUserIntent: StateFlow<String> = _lastUserIntent.asStateFlow()

    private var lastLifecycleOwner: androidx.lifecycle.LifecycleOwner? = null
    private var isTtsInitialized = false

    // 最后拍摄的照片
    private val _lastPhoto = MutableStateFlow<Bitmap?>(null)
    val lastPhoto: StateFlow<Bitmap?> = _lastPhoto.asStateFlow()

    //TODO: 导入STT类
    // 语音识别相关
    private val _isListening = MutableStateFlow<Boolean>(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    private val _voiceRecognitionResult = MutableStateFlow<String>("")
    val voiceRecognitionResult: StateFlow<String> = _voiceRecognitionResult.asStateFlow()

    /**
     * 当前摄像头镜头朝向（0 = 后置，1 = 前置）
     *
     * 扩展点：可添加摄像头切换动画、提示等
     */
    private val _currentLensFacing = MutableStateFlow<Int>(androidx.camera.core.CameraSelector.LENS_FACING_BACK)
    val currentLensFacing: StateFlow<Int> = _currentLensFacing.asStateFlow()

    /**
     * 当前变焦比例
     *
     * 扩展点：UI 可监听此字段更新变焦滑块或显示当前变焦值
     */
    private val _currentZoom = MutableStateFlow<Float>(1f)
    val currentZoom: StateFlow<Float> = _currentZoom.asStateFlow()

    /**
     * 变焦范围信息（minZoom, maxZoom, step）
     */
    private val _zoomRangeInfo = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    val zoomRangeInfo: StateFlow<Triple<Float, Float, Float>?> = _zoomRangeInfo.asStateFlow()

    // 对焦状态
    private val _focusState = MutableStateFlow<CameraController.FocusState>(CameraController.FocusState.Idle)
    val focusState: StateFlow<CameraController.FocusState> = _focusState.asStateFlow()

    // 对焦点坐标（用于 UI 显示对焦框）
    private val _focusPointX = MutableStateFlow<Float>(0.5f)
    val focusPointX: StateFlow<Float> = _focusPointX.asStateFlow()

    private val _focusPointY = MutableStateFlow<Float>(0.5f)
    val focusPointY: StateFlow<Float> = _focusPointY.asStateFlow()

    /**
     * 初始化相机
     *
     * @param lifecycleOwner Activity/Fragment 生命周期持有者
     * @param previewView 相机预览 View
     */
    fun initializeCamera(
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        previewView: androidx.camera.view.PreviewView
    ) {
        _uiState.value = CameraUIState.Initializing
        lastLifecycleOwner = lifecycleOwner

        // 检查设备是否有摄像头
        if (!cameraController.hasCameraDevice()) {
            _errorMessage.value = "设备不支持相机功能"
            _uiState.value = CameraUIState.Error
            return
        }

        // 设置相机回调
        cameraController.onCameraReady = {
            _uiState.value = CameraUIState.Ready
        }

        cameraController.onCameraError = { error ->
            _errorMessage.value = error
            _uiState.value = CameraUIState.Error
        }

        cameraController.onFocusStateChanged = { focusState ->
            _focusState.value = focusState
        }

        cameraController.onZoomRangeUpdated = { minZoom, maxZoom ->
            Log.d(TAG, "变焦范围已更新: $minZoom - $maxZoom")
            updateZoomRangeInfo()
        }

        // 设置实时帧回调（预留接口）
        cameraController.onFrameAvailable = { frame ->
            handleFrameAvailable(frame)
        }

        // 初始化相机
        cameraController.initializeCamera(lifecycleOwner, previewView)
    }

    /**
     * 拍照
     */
    fun takePicture() {
        if (_uiState.value != CameraUIState.Ready) {
            _errorMessage.value = "相机未就绪"
            return
        }

        _uiState.value = CameraUIState.Taking

        cameraController.takePicture(
            androidx.core.content.ContextCompat.getMainExecutor(getApplication())
        ) { bitmap ->
            if (bitmap != null) {
                _lastPhoto.value = bitmap
                savePicture(bitmap)
            } else {
                _errorMessage.value = "拍照失败"
                _uiState.value = CameraUIState.Ready
            }
        }
    }

    /**
     * 切换前后置摄像头
     *
     * 扩展点：可添加切换动画、提示信息等
     *
     * @param lifecycleOwner Activity/Fragment 生命周期持有者
     * @param previewView 相机预览 View
     */
    fun switchCamera(
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        previewView: androidx.camera.view.PreviewView
    ) {
        try {
            val newLensFacing = cameraController.switchCamera(lifecycleOwner, previewView)
            if (newLensFacing != null) {
                _currentLensFacing.value = newLensFacing
                _currentZoom.value = 1f
                _errorMessage.value = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "切换摄像头异常", e)
            _errorMessage.value = "切换摄像头失败：${e.message}"
        }
    }

    /**
     * 设置变焦（支持动画）
     */
    fun setZoom(zoomFactor: Float, animate: Boolean = true) {
        try {
            val actualZoom = cameraController.setZoom(zoomFactor, animate)
            _currentZoom.value = actualZoom
        } catch (e: Exception) {
            Log.e(TAG, "变焦操作异常", e)
            _errorMessage.value = "变焦失败：${e.message}"
        }
    }

    /**
     * 放大（动画变焦）
     */
    fun zoomIn() {
        try {
            val actualZoom = cameraController.zoomIn()
            _currentZoom.value = actualZoom
        } catch (e: Exception) {
            Log.e(TAG, "放大失败", e)
        }
    }

    /**
     * 缩小（动画变焦）
     */
    fun zoomOut() {
        try {
            val actualZoom = cameraController.zoomOut()
            _currentZoom.value = actualZoom
        } catch (e: Exception) {
            Log.e(TAG, "缩小失败", e)
        }
    }

    /**
     * 重置变焦
     */
    fun resetZoom() {
        try {
            val actualZoom = cameraController.resetZoom()
            _currentZoom.value = actualZoom
        } catch (e: Exception) {
            Log.e(TAG, "重置变焦失败", e)
        }
    }

    /**
     * 自动对焦（单点击）
     */
    fun autoFocus(x: Float, y: Float) {
        try {
            _focusPointX.value = x
            _focusPointY.value = y
            cameraController.autoFocus(x, y)
        } catch (e: Exception) {
            Log.e(TAG, "对焦失败", e)
            _errorMessage.value = "对焦失败：${e.message}"
        }
    }

    /**
     * 锁定对焦（长按）
     */
    fun lockFocus(x: Float, y: Float) {
        try {
            _focusPointX.value = x
            _focusPointY.value = y
            cameraController.lockFocus(x, y)
        } catch (e: Exception) {
            Log.e(TAG, "对焦锁定失败", e)
            _errorMessage.value = "对焦锁定失败：${e.message}"
        }
    }

    /**
     * 重置对焦
     */
    fun resetFocus() {
        try {
            cameraController.resetFocus()
        } catch (e: Exception) {
            Log.e(TAG, "重置对焦失败", e)
        }
    }

    /**
     * 更新变焦范围信息
     */
    fun updateZoomRangeInfo() {
        try {
            val rangeInfo = cameraController.getZoomRangeInfo()
            _zoomRangeInfo.value = rangeInfo
        } catch (e: Exception) {
            Log.e(TAG, "获取变焦范围失败", e)
        }
    }

    /**
     * 保存照片到相册
     *
     * @param bitmap 照片位图
     */
    private fun savePicture(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = CameraUIState.Saving

            try {
                Log.d(TAG, "开始保存照片，Bitmap 尺寸: ${bitmap.width}x${bitmap.height}")

                // 检查存储空间
                if (!fileManager.hasEnoughStorage()) {
                    Log.e(TAG, "存储空间不足")
                    _errorMessage.value = "存储空间不足"
                    _uiState.value = CameraUIState.Ready
                    return@launch
                }

                Log.d(TAG, "存储空间充足，开始保存到相册...")

                // 保存到相册
                val result = fileManager.saveBitmapToGallery(bitmap)
                if (result != null) {
                    Log.d(TAG, "照片成功保存，URI: $result")
                    _uiState.value = CameraUIState.PhotoSaved
                } else {
                    Log.e(TAG, "照片保存失败")
                    _errorMessage.value = "照片保存失败"
                    _uiState.value = CameraUIState.Ready
                }
            } catch (e: Exception) {
                Log.e(TAG, "保存异常", e)
                _errorMessage.value = "保存异常：${e.message}"
                _uiState.value = CameraUIState.Ready
            }
        }
    }

    /**
     * 处理实时帧数据回调（预留接口）
     *
     * 扩展点：
     * - 在此处接收相机实时帧
     * - 将帧发送给后端 AI 服务
     * - 接收 AI 建议并更新 _aiAdvice 字段
     * - 如果启用了语音播报，触发 playVoiceAdvice()
     *
     * @param frame 相机实时帧 Bitmap
     */
    private fun handleFrameAvailable(frame: Bitmap) {
        // TODO: 实现实时帧处理逻辑
        // 当前暂不实现，预留给后续 AI 功能
        // 1. 将 frame 发送给后端 AI 服务
        // 2. 接收 AI 返回的建议
        // 3. 更新 _aiAdvice 字段
        // 4. 如果 _voiceGuideEnabled 为 true，调用 playVoiceAdvice()
    }

    /**
     * 触发 AI 姿势指导分析
     *
     * 扩展点：
     * - 可在此处补充 userIntent / metaJson
     * - 可根据镜头方向/拍摄模式生成更丰富的元数据
     */
    fun requestPoseGuidance(
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        userIntent: String? = null,
        metaJson: String? = null
    ) {
        if (_poseLoading.value) return

        val resolvedIntent = when {
            !userIntent.isNullOrBlank() -> userIntent
            _voiceGuideEnabled.value -> _lastUserIntent.value.takeIf { it.isNotBlank() }
            else -> null
        }

        _poseLoading.value = true
        _poseErrorMessage.value = null
        _poseGuideText.value = ""
        _poseSuggestionText.value = ""
        _poseImageUrl.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                ensureStreamCameraStarted(lifecycleOwner)

                val focusOk = cameraStreamManager.triggerAutoFocus()
                Log.d(TAG, "AI姿势指导对焦结果: $focusOk")

                val bitmap = cameraStreamManager.captureSingleFrame()
                val imageFile = cameraStreamManager.saveBitmapToJpg(bitmap)

                poseClient.analyzePose(imageFile, resolvedIntent, metaJson, object : PoseRecommendationClient.PoseCallback {
                    override fun onSuccess(response: PoseResponse) {
                        viewModelScope.launch {
                            _poseLoading.value = false
                            _poseGuideText.value = response.guideText ?: ""
                            _poseImageUrl.value = response.poseImageUrl ?: ""
                            _poseSuggestionText.value = formatPoseSuggestions(response)

                            if (_voiceGuideEnabled.value && !_poseGuideText.value.isNullOrBlank()) {
                                playVoiceAdvice(_poseGuideText.value)
                            }
                        }
                    }

                    override fun onError(errorMessage: String) {
                        viewModelScope.launch {
                            _poseLoading.value = false
                            _poseErrorMessage.value = errorMessage
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "AI姿势指导请求失败", e)
                viewModelScope.launch {
                    _poseLoading.value = false
                    _poseErrorMessage.value = "AI姿势指导失败：${e.message}"
                }
            }
        }
    }

    private fun ensureStreamCameraStarted(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        if (isStreamCameraStarted) return
        cameraStreamManager.startCamera(lifecycleOwner)
        isStreamCameraStarted = true
    }

    private fun formatPoseSuggestions(response: PoseResponse): String {
        val suggestions = response.poseSuggestions ?: emptyList()
        if (suggestions.isEmpty()) return ""

        val lines = mutableListOf<String>()
        for (suggestion in suggestions) {
            val name = suggestion.name?.takeIf { it.isNotBlank() } ?: continue
            lines.add("• $name")

            suggestion.tips?.forEach { tip ->
                if (!tip.isNullOrBlank()) {
                    lines.add("  - $tip")
                }
            }

            val details = suggestion.details
            if (details != null) {
                addPoseDetail(lines, "头部", details.head)
                addPoseDetail(lines, "手臂", details.arms)
                addPoseDetail(lines, "手部", details.hands)
                addPoseDetail(lines, "躯干", details.torso)
                addPoseDetail(lines, "髋部", details.hips)
                addPoseDetail(lines, "腿部", details.legs)
                addPoseDetail(lines, "脚部", details.feet)
                addPoseDetail(lines, "朝向", details.orientation)
            }
        }

        return lines.joinToString("\n")
    }

    private fun addPoseDetail(lines: MutableList<String>, label: String, value: String?) {
        if (!value.isNullOrBlank()) {
            lines.add("  - $label：$value")
        }
    }

    /**
     * 切换语音播报开关
     */
    fun toggleVoiceGuide() {
        _voiceGuideEnabled.value = !_voiceGuideEnabled.value
    }

    /**
     * 重新开始拍照
     */
    fun resumePreview() {
        _uiState.value = CameraUIState.Ready
        _errorMessage.value = null
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    fun releaseCamera(context: android.content.Context) {
        val cameraController = CameraController(context)
        cameraController.releaseCamera()
    }

    // TODO: 语音识别
    /**
     * 初始化语音识别
     */
    fun initializeSpeechRecognizer() {
        val context = getApplication<Application>().applicationContext
        SparkAsrManager.getInstance().init(context)
    }

    /**
     * 开始语音识别
     */
    fun startListening() {
        _voiceGuideEnabled.value = true
        if (!_voiceGuideEnabled.value) {
            _errorMessage.value = "请先开启语音指导功能"
            //return
        }

        // 检查录音权限
        val hasAudioPermission = PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(
                getApplication(),
                android.Manifest.permission.RECORD_AUDIO
            )

        if (!hasAudioPermission) {
            _errorMessage.value = "请先授予录音权限"
            return
        }


        initializeSpeechRecognizer()

        try {
            _isListening.value = true
            SparkAsrManager.getInstance().startListening(object : SparkAsrManager.AsrListener {
                override fun onResult(text: String, isLast: Boolean) {
                    _voiceRecognitionResult.value = text
                    if (isLast) {
                        _isListening.value = false
                        _lastUserIntent.value = text
                        val owner = lastLifecycleOwner
                        if (_voiceGuideEnabled.value && owner != null && text.isNotBlank()) {
                            requestPoseGuidance(owner, userIntent = text)
                        }
                    }

                    val resultText = text ?: "空内容"
                    Log.d(TAG, "ASR识别结果: $resultText, 是否最后一条: $isLast")
                }

                override fun onError(code: Int, msg: String) {
                    _errorMessage.value = "ASR语音识别失败$msg"
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "开始语音识别失败", e)
            _isListening.value = false
            _errorMessage.value = "开始语音识别失败: ${e.message}"
        }
    }

    /**
     * 停止语音识别
     */
    fun stopListening() {
        try {
            _isListening.value = false
            _voiceGuideEnabled.value = false
            _lastUserIntent.value = ""
            SparkAsrManager.getInstance().stopListening();
            TTSManager.getInstance().stopTTS()
        } catch (e: Exception) {
            Log.e(TAG, "停止语音识别失败", e)
        }
    }

    /**
     * 释放语音识别资源
     */
    private fun releaseSpeechRecognizer() {
        SparkAsrManager.getInstance().destroy();
    }

    fun playVoiceAdvice(advice: String) {
        if (advice.isBlank()) return
        ensureTtsInitialized()

        TTSManager.getInstance().startTTS(advice, object : TTSManager.TTSListener {
            override fun onResult(result: com.iflytek.sparkchain.core.tts.TTS.TTSResult?, usrTag: Any?) {
                Log.d(TAG, "TTS播放完成")
            }

            override fun onError(error: com.iflytek.sparkchain.core.tts.TTS.TTSError?, usrTag: Any?) {
                _errorMessage.value = "语音播报失败：${error?.errMsg ?: "未知错误"}"
            }
        })
    }

    private fun ensureTtsInitialized() {
        if (isTtsInitialized) return
        TTSManager.getInstance().init()
        isTtsInitialized = true
    }

    override fun onCleared() {
        super.onCleared()
        cameraController.releaseCamera()
        cameraStreamManager.stopCamera()
        TTSManager.getInstance().destroy()
        releaseSpeechRecognizer()
    }

    fun clearVoiceRecognitionText() {
        _voiceRecognitionResult.value = "当前未识别到语音，请打开语音识别按钮"
        _lastUserIntent.value = "ai指导建议：当前无ai指导建议"
    }

    fun clearPoseGuidanceText() {
        _poseGuideText.value = ""
        _poseSuggestionText.value = ""
        _poseImageUrl.value = ""
        _poseErrorMessage.value = null
    }
}

/**
 * 相机 UI 状态定义
 */
sealed class CameraUIState {
    object Idle : CameraUIState()
    object Initializing : CameraUIState()
    object Ready : CameraUIState()
    object Taking : CameraUIState()
    object Saving : CameraUIState()
    object PhotoSaved : CameraUIState()
    object Error : CameraUIState()
}
