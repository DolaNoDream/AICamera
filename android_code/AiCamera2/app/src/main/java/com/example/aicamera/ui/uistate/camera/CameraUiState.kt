package com.example.aicamera.ui.uistate.camera

import androidx.compose.ui.unit.IntOffset
import com.example.aicamera.data.camera.CameraController


/**
 * CameraUiState
 *
 * 目标：让 CameraScreen 只订阅一个 StateFlow<CameraUiState>，避免在 UI 层散落大量 collectAsState。
 *
 * 说明：这里放的是“可序列化/可展示”的 UI 数据，不放 Context / CameraX 对象。
 */
data class CameraUiState(
    // --- 基础拍照流程 ---
    val cameraState: CameraState = CameraState.Idle,
    val errorMessage: String? = null,

    // --- 语音 ---
    val voiceGuideEnabled: Boolean = false,
    val isListening: Boolean = false,
    val voiceRecognitionResult: String = "",
    val lastUserIntent: String = "",

    // --- AI 姿势指导返回 ---
    val poseGuideText: String = "",
    val poseSuggestionText: String = "",
    val poseImageUrl: String = "",
    val poseLoading: Boolean = false,
    val poseErrorMessage: String? = null,

    // --- 镜头与变焦 ---
    val currentLensFacing: Int = androidx.camera.core.CameraSelector.LENS_FACING_BACK,
    val zoom: ZoomUi = ZoomUi(),

    // --- 对焦 UI 表现 ---
    val focusState: CameraController.FocusState = CameraController.FocusState.Idle,
    val focusPointX: Float = 0.5f,
    val focusPointY: Float = 0.5f,

    // --- 页面上的 UI 控件状态（Compose remember 的部分也可以逐步迁移到这里） ---
    val selectedMode: CameraMode = CameraMode.Standard,
    val isMenuExpanded: Boolean = false,
    val isLeftPanelExpanded: Boolean = false,

    // --- 悬浮窗状态 ---
    val floatingWindowStatus: FloatingWindowStatus = FloatingWindowStatus.Default,
    val floatingWindowPosition: FloatingWindowPosition = FloatingWindowPosition.Right, // 默认在右边
    val voiceToTextContent: String = "等待语音输入...", // 显示的文字
    // 新增：记录悬浮窗相对于初始位置的偏移量 (x, y)
    val floatingOffset: IntOffset = IntOffset(0, 0)
) {
    val isAiVoiceBoxVisible: Boolean
        get() = selectedMode == CameraMode.AiSuggestion || selectedMode == CameraMode.AiPose
}

/** 相机主流程状态（替代原 CameraUIState sealed class） */
enum class CameraState {
    Idle,
    Initializing,
    Ready,
    Taking,
    Saving,
    PhotoSaved,
    Error
}

data class ZoomUi(
    val currentZoom: Float = 1f,
    val minZoom: Float = 0.5f,
    val maxZoom: Float = 5f
)

/**
 * 页面模式（标准拍照 / AI语音建议 / AI姿势指导）。
 *
 * 说明：用 enum 避免 UI 里用字符串判断，后续国际化改文案不会影响逻辑。
 */
enum class CameraMode {
    Standard,
    AiSuggestion,
    AiPose
}

/**
 * 悬浮窗的UISTATE
 */
// 1. 定义悬浮窗的状态枚举
enum class FloatingWindowStatus {
    Default,    // 圆形图标状态
    Activated   // 横向模块展开状态
}

// 2. 定义悬浮窗的位置枚举
enum class FloatingWindowPosition {
    Left,
    Right
}