package com.example.aicamera.ui.viewmodel.camera

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.remote.creation.compose.state.log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicamera.app.di.ServiceLocator
import com.example.aicamera.data.camera.CameraController
import com.example.aicamera.data.camera.CameraStreamManager
import com.example.aicamera.data.db.entity.AlbumPhotoEntity
import com.example.aicamera.data.network.pose.PoseRecommendationClient
import com.example.aicamera.data.network.pose.model.PoseResponse
import com.example.aicamera.data.permission.PermissionManager
import com.example.aicamera.data.speech.stt.SparkAsrManager
import com.example.aicamera.data.speech.tts.TTSManager
import com.example.aicamera.data.storage.FileManager
import com.example.aicamera.ui.uistate.camera.CameraMode
import com.example.aicamera.ui.uistate.camera.CameraState
import com.example.aicamera.ui.uistate.camera.CameraUiEvent
import com.example.aicamera.ui.uistate.camera.CameraUiState
import com.example.aicamera.ui.uistate.camera.FloatingWindowPosition
import com.example.aicamera.ui.uistate.camera.FloatingWindowStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private var permissionManager : PermissionManager = PermissionManager(getApplication())

    /**
     * 单一 UiState：UI 层只需要 collect 这一份即可。
     */
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    /**
     * 一次性事件流：Snackbar/导航等。
     */
    private val _events = MutableSharedFlow<CameraUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CameraUiEvent> = _events

    // AI姿势指导相关状态
    private val poseClient = PoseRecommendationClient.getInstance()
    private val cameraStreamManager = CameraStreamManager(application)
    private var isStreamCameraStarted = false

    private var lastLifecycleOwner: androidx.lifecycle.LifecycleOwner? = null
    private var isTtsInitialized = false

    // 最后拍摄的照片
    private val _lastPhoto = MutableStateFlow<Bitmap?>(null)
    val lastPhoto: StateFlow<Bitmap?> = _lastPhoto.asStateFlow()

    private val albumRepository by lazy {
        ServiceLocator.provideAlbumRepository(getApplication<Application>().applicationContext)
    }

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
        _uiState.update { currentState ->
            currentState.copy(cameraState = CameraState.Initializing)
        }
        lastLifecycleOwner = lifecycleOwner

        // 检查设备是否有摄像头
        if (!cameraController.hasCameraDevice()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "设备不支持相机功能",
                    cameraState = CameraState.Error
                )
            }
            return
        }

        // 设置相机回调
        cameraController.onCameraReady = {
            _uiState.update { currentState ->
                currentState.copy(cameraState = CameraState.Ready)
            }
        }

        cameraController.onCameraError = { error ->
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = error,
                    cameraState = CameraState.Error
                )
            }
        }

        cameraController.onFocusStateChanged = { focusState ->
            _uiState.update { currentState ->
                currentState.copy(
                    focusState = focusState
                )
            }
        }

        cameraController.onZoomRangeUpdated = { minZoom, maxZoom ->
            Log.d(TAG, "变焦范围已更新: $minZoom - $maxZoom")
            updateZoomRangeInfo()
        }

        // 初始化相机
        cameraController.initializeCamera(lifecycleOwner, previewView)
    }

    /**
     * 拍照
     */
    fun takePicture() {
        if (_uiState.value.cameraState != CameraState.Ready) {
            _uiState.update { currentState ->
                currentState.copy(errorMessage = "相机未就绪")
            }
            return
        }

        _uiState.update { currentState ->
            currentState.copy(cameraState = CameraState.Taking)
        }

        cameraController.takePicture(
            androidx.core.content.ContextCompat.getMainExecutor(getApplication())
        ) { bitmap ->
            if (bitmap != null) {
                _lastPhoto.value = bitmap
                savePicture(bitmap)
            } else {
                _uiState.update { currentState ->
                    currentState.copy(
                        errorMessage = "拍照失败",
                        cameraState = CameraState.Ready
                    )
                }
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
                _uiState.update{ currentState ->
                    currentState.copy(
                        currentLensFacing = newLensFacing
                    )
                }
                _uiState.update {
                        currentState ->
                    currentState.copy(
                        zoom = currentState.zoom.copy(currentZoom = 1f)
                    )
                }
                _uiState.update { currentState ->
                    currentState.copy(errorMessage = null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "切换摄像头异常", e)
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "切换摄像头失败：${e.message}"
                )
            }
        }
    }

    /**
     * 设置变焦（支持动画）
     */
    fun setZoom(zoomFactor: Float, animate: Boolean = true) {
        try {
            val actualZoom = cameraController.setZoom(zoomFactor, animate)
            _uiState.update {
                    currentState ->
                currentState.copy(
                    zoom = currentState.zoom.copy(currentZoom = actualZoom)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "变焦操作异常", e)
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "变焦失败：${e.message}"
                )
            }
        }
    }

    /**
     * 放大（动画变焦）
     */
    fun zoomIn() {
        try {
            val actualZoom = cameraController.zoomIn()
            _uiState.update {
                    currentState ->
                currentState.copy(
                    zoom = currentState.zoom.copy(currentZoom = actualZoom)
                )
            }
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
            _uiState.update {
                    currentState ->
                currentState.copy(
                    zoom = currentState.zoom.copy(currentZoom = actualZoom)
                )
            }
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
            _uiState.update {
                currentState ->
                currentState.copy(
                    zoom = currentState.zoom.copy(currentZoom = actualZoom)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "重置变焦失败", e)
        }
    }

    /**
     * 自动对焦（单点击）
     */
    fun autoFocus(x: Float, y: Float) {
        try {
            _uiState.update { currentState ->
                currentState.copy(
                    focusPointX = x,
                    focusPointY = y,
                )
            }
            cameraController.autoFocus(x, y)
        } catch (e: Exception) {
            Log.e(TAG, "对焦失败", e)
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "对焦失败：${e.message}"
                )
            }
        }
    }

    /**
     * 锁定对焦（长按）
     */
    fun lockFocus(x: Float, y: Float) {
        try {
            _uiState.update { currentState ->
                currentState.copy(
                    focusPointX = x,
                    focusPointY = y,
                )
            }
            cameraController.lockFocus(x, y)
        } catch (e: Exception) {
            Log.e(TAG, "对焦锁定失败", e)
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "对焦锁定失败：${e.message}"
                )
            }
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
            _uiState.update {
                currentState ->
                currentState.copy(
                    zoom = currentState.zoom.copy(
                        minZoom = rangeInfo?.first ?: 0.5f,
                        maxZoom = rangeInfo?.second ?: 5f,
                        currentZoom = rangeInfo?.third ?: 1f
                    )
                )
            }
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
            _uiState.update { currentState ->
                currentState.copy(cameraState = CameraState.Saving)
            }

            try {
                Log.d(TAG, "开始保存照片，Bitmap 尺寸: ${bitmap.width}x${bitmap.height}")

                // 检查存储空间
                if (!fileManager.hasEnoughStorage()) {
                    Log.e(TAG, "存储空间不足")
                    _uiState.update { currentState ->
                        currentState.copy(
                            errorMessage = "存储空间不足",
                            cameraState = CameraState.Ready
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "存储空间充足，开始保存到相册...")

                // 保存到相册
                val result = fileManager.saveBitmapToGallery(bitmap)
                if (result != null) {
                    Log.d(TAG, "照片成功保存，URI: $result")

                    // 保存到 Room（用于相册列表页展示）
                    // 扩展点：后续可在这里更新 type/text（例如 P 图/手账结果）或补充更多 meta。
                    runCatching {
                        albumRepository.insertPhoto(
                            AlbumPhotoEntity(
                                filePath = result,
                                type = 0,
                                text = "无",
                                createTime = System.currentTimeMillis(),
                                width = bitmap.width,
                                height = bitmap.height,
                                fileSize = null
                            )
                        )
                    }.onFailure { e ->
                        Log.w(TAG, "照片已保存到系统相册，但写入本地数据库失败：${e.message}")
                    }

                    _uiState.update { currentState ->
                        currentState.copy(cameraState = CameraState.PhotoSaved)
                    }
                } else {
                    Log.e(TAG, "照片保存失败")
                    _uiState.update { currentState ->
                        currentState.copy(
                            errorMessage = "照片保存失败",
                            cameraState = CameraState.Ready
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "保存异常", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        errorMessage = "保存异常：${e.message}",
                        cameraState = CameraState.Ready
                    )
                }
            }
        }
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
        if (_uiState.value.poseLoading) return

        val resolvedIntent: String? = when {
            !userIntent.isNullOrBlank() -> userIntent
            _uiState.value.voiceGuideEnabled -> _uiState.value.lastUserIntent.takeIf { it.isNotBlank() }
            else -> null
        }

        _uiState.update { currentState ->
            currentState.copy(
                poseLoading = true,
                poseErrorMessage = null,
                poseGuideText = "",
                poseSuggestionText = "",
                poseImageUrl = ""
            )
        }

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
                            _uiState.update { currentState ->
                                currentState.copy(
                                    poseLoading = false,
                                    poseGuideText = response.guideText ?: "",
                                    poseImageUrl = response.poseImageUrl ?: "",
                                    poseSuggestionText = formatPoseSuggestions(response)
                                )
                            }

                            if (_uiState.value.voiceGuideEnabled && !_uiState.value.poseGuideText.isNullOrBlank()) {
                                playVoiceAdvice(_uiState.value.poseGuideText)
                            }
                        }
                    }

                    override fun onError(errorMessage: String) {
                        viewModelScope.launch {
                            _uiState.update { currentState ->
                                currentState.copy(
                                    poseLoading = false,
                                    poseErrorMessage = errorMessage
                                )
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "AI姿势指导请求失败", e)
                viewModelScope.launch {
                    _uiState.update { currentState ->
                        currentState.copy(
                            poseLoading = false,
                            poseErrorMessage = "AI姿势指导失败：${e.message}"
                        )
                    }
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
        _uiState.update { currentState ->
            currentState.copy(
                voiceGuideEnabled = !currentState.voiceGuideEnabled
            )
        }
    }

    /**
     * 重新开始拍照
     */
    fun resumePreview() {
        _uiState.update { currentState ->
            currentState.copy(
                cameraState = CameraState.Ready,
                errorMessage = null
            )
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { currentState ->
            currentState.copy(
                errorMessage = null
            )
        }
    }

    fun releaseCamera(context: android.content.Context) {
        val cameraController = CameraController(context)
        cameraController.releaseCamera()
    }

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
        _uiState.update { currentState ->
            currentState.copy(voiceGuideEnabled = true)
        }
        if (!_uiState.value.voiceGuideEnabled) {
            _uiState.update { currentState ->
                currentState.copy(errorMessage = "请先开启语音指导功能")
            }
            //return
        }


        if (!permissionManager.hasAudioPermission()) {
            _uiState.update { currentState ->
                currentState.copy(errorMessage = "请先授予录音权限")
            }
            permissionManager.requestAudioPermission()
            if (permissionManager.hasAudioPermission()) {
                _uiState.update { currentState ->
                    currentState.copy(errorMessage = null)
                }
            }
            return
        }

        initializeSpeechRecognizer()

        try {
            _uiState.update{ currentState ->
                currentState.copy(
                    voiceRecognitionResult = "正在聆听，请说话...",
                    isListening = true
                )
            }
            SparkAsrManager.getInstance().startListening(object : SparkAsrManager.AsrListener {
                override fun onResult(text: String, isLast: Boolean) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            voiceRecognitionResult = text
                        )
                    }
                    if (isLast) {
                        _uiState.update{ currentState ->
                            currentState.copy(
                                isListening = false
                            )
                        }
                        _uiState.update{ currentState ->
                            currentState.copy(
                                lastUserIntent = text
                            )
                        }
                        val owner = lastLifecycleOwner
                        if (_uiState.value.voiceGuideEnabled && owner != null && text.isNotBlank()) {
                            requestPoseGuidance(owner, userIntent = text)
                        }
                    }

                    val resultText = text ?: "空内容"
                    Log.d(TAG, "ASR识别结果: $resultText, 是否最后一条: $isLast")
                }

                override fun onError(code: Int, msg: String) {
                    _uiState.update { currentState ->
                        currentState.copy(errorMessage = "ASR语音识别失败$msg")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "开始语音识别失败", e)
            _uiState.update{ currentState ->
                currentState.copy(isListening = false)
            }
            _uiState.update { currentState ->
                currentState.copy(errorMessage = "开始语音识别失败: ${e.message}")
            }
        }
    }

    /**
     * 停止语音识别
     */
    fun stopListening() {
        try {
            _uiState.update{ currentState ->
                currentState.copy(isListening = false)
            }
            _uiState.update { currentState ->
                currentState.copy(voiceGuideEnabled = false)
            }
            _uiState.update{ currentState ->
                currentState.copy(
                    lastUserIntent = ""
                )
            }
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
                _uiState.update { currentState ->
                    currentState.copy(errorMessage = "语音播报失败：${error?.errMsg ?: "未知错误"}")
                }
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
        _uiState.update { currentState ->
            currentState.copy(
                voiceRecognitionResult = "当前未识别到语音，请打开语音识别按钮"
            )
        }
        _uiState.update{ currentState ->
            currentState.copy(
                lastUserIntent = "ai指导建议：当前无ai指导建议"
            )
        }
    }

    fun clearPoseGuidanceText() {
        _uiState.update { currentState ->
            currentState.copy(
                poseGuideText = "",
                poseSuggestionText = "",
                poseImageUrl = "",
                poseErrorMessage = null
            )
        }
    }

    /**
     * UI 控件状态：顶部菜单是否展开。
     * 放在 UiState 里可以避免 remember 丢失（例如配置变更/进程恢复）。
     */
    fun setMenuExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isMenuExpanded = expanded) }
    }

    /**
     * UI 控件状态：左侧面板是否展开。
     */
    fun setLeftPanelExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isLeftPanelExpanded = expanded) }
    }

    /**
     * UI 模式：标准拍照 / AI语音建议 / AI姿势指导。
     * 注意：这里用的是 uistate/camera/CameraMode(enum)，不受文案(strings)变化影响。
     */
    fun setSelectedMode(mode: CameraMode) {
        _uiState.update { it.copy(selectedMode = mode) }
        // 约束：进入 AI 姿势模式默认展开左侧面板；其他模式收起
        _uiState.update { current ->
            current.copy(isLeftPanelExpanded = mode == CameraMode.AiPose)
        }
    }

    // 切换悬浮窗的展开/折叠状态
    fun toggleFloatingWindowStatus() {
        val currentStatus = uiState.value.floatingWindowStatus
        _uiState.update {
            it.copy(
                floatingWindowStatus = if (currentStatus == FloatingWindowStatus.Default) {
                    FloatingWindowStatus.Activated
                } else {
                    FloatingWindowStatus.Default
                }
            )
        }
    }

    // 改变悬浮窗的位置（左/右）
    fun setFloatingWindowPosition(position: FloatingWindowPosition) {
        _uiState.update { it.copy(floatingWindowPosition = position) }
    }

    // 模拟语音转文字更新
    fun updateVoiceText(text: String) {
        _uiState.update { it.copy(voiceToTextContent = text) }
    }

    // 悬浮窗内按钮的点击处理
    fun onFloatingWindowButtonClick() {
        // 这里处理点击后的逻辑，比如确认 AI 建议
        println("悬浮窗按钮被点击了！当前文字: ${uiState.value.voiceToTextContent}")
        // 处理完后，通常会把悬浮窗收起
        _uiState.update { it.copy(floatingWindowStatus = FloatingWindowStatus.Default) }
    }

    // 悬浮窗移动处理
    fun updateFloatingOffset(dragAmount: Offset) {
        _uiState.update {
            val newX = (it.floatingOffset.x + dragAmount.x).toInt()
            val newY = (it.floatingOffset.y + dragAmount.y).toInt()
            it.copy(floatingOffset = IntOffset(newX, newY))
        }
    }

    //判断悬浮窗离哪边更近
    fun onDragEnd(screenWidthPx: Int) {
        _uiState.update { currentState ->
            val currentX = currentState.floatingOffset.x
            val componentWidth = 150 // 预估宽度像素

            // 判断靠近哪一边
            val isLeft = currentX < (screenWidthPx / 2)
            val targetX = if (isLeft) 0 else (screenWidthPx - 130) // 130是图标px宽度左右

            Log.d("Postion", isLeft.toString())

            currentState.copy(
                floatingOffset = IntOffset(targetX, currentState.floatingOffset.y),
                // 这一步至关重要：决定了 AiFloatingWindow 内部 Row 的组件顺序
                floatingWindowPosition = if (isLeft) FloatingWindowPosition.Left else FloatingWindowPosition.Right
            )

        }
    }
}

/**
 * 相机 UI 状态定义
 */
