package com.example.aicamera.ui.screen.camera

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.example.aicamera.data.camera.CameraController
import com.example.aicamera.ui.screen.camera.components.AiSuggestionStatusBox
import com.example.aicamera.ui.screen.camera.components.CameraControlsLayer
import com.example.aicamera.ui.screen.camera.components.ErrorOverlay
import com.example.aicamera.ui.screen.camera.components.FocusIndicator
import com.example.aicamera.ui.screen.camera.components.LeftHiddenMenu
import com.example.aicamera.ui.screen.camera.components.LoadingOverlay
import com.example.aicamera.ui.screen.camera.components.SaveSuccessOverlay
import com.example.aicamera.ui.screen.camera.components.TimeDisplay
import com.example.aicamera.ui.screen.camera.components.ZoomButton
import com.example.aicamera.ui.uistate.camera.CameraMode
import com.example.aicamera.ui.uistate.camera.CameraState
import com.example.aicamera.ui.viewmodel.camera.CameraViewModel
import com.google.android.material.snackbar.Snackbar
import kotlin.math.sqrt

/**
 * 相机页面 UI
 * 支持手动对焦、长按锁定对焦、双指捏合变焦
 */
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier,
    onNavigateToAlbum: (() -> Unit)? = null,
    onNavigateToCopywriting: (() -> Unit)? = null
) {
    val state = viewModel.uiState.collectAsState().value

    val context = LocalContext.current
    val lifecycleOwnerRef = lifecycleOwner
    val previewViewRef = remember { mutableStateOf<androidx.camera.view.PreviewView?>(null) }

    // 模式列表改为 enum（由 UiState 驱动选中态，不再在 UI 层 remember）
    val cameraModes = remember {
        listOf(CameraMode.Standard, CameraMode.AiSuggestion, CameraMode.AiPose)
    }

    LaunchedEffect(state.errorMessage) {
        if (!state.errorMessage.isNullOrEmpty()) {
            val rootView = (context as? android.app.Activity)?.window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
            if (rootView != null) {
                Snackbar.make(rootView, state.errorMessage, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(state.cameraState) {
        if (state.cameraState == CameraState.PhotoSaved) {
            kotlinx.coroutines.delay(1000)
            viewModel.resumePreview()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updateZoomRangeInfo()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 预览层：真实 PreviewView 在最底层
        CameraPreviewLayer(
            viewModel = viewModel,
            lifecycleOwner = lifecycleOwner,
            onPreviewViewReady = { previewView ->
                previewViewRef.value = previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 轻微的遮罩，让整体更接近系统相机的“稳重”观感
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f))
        )

        // 顶部栏：左侧入口（文案列表），右侧相册入口
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (onNavigateToCopywriting != null) {
                IconButton(
                    onClick = onNavigateToCopywriting,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = "文案",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onNavigateToAlbum != null) {
                IconButton(
                    onClick = onNavigateToAlbum,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "打开相册"
                    )
                }
            }
        }

        // 左侧隐藏式菜单（AI 姿势建议/图片等）
        LeftHiddenMenu(
            isExpanded = state.isLeftPanelExpanded,
            onToggle = { viewModel.setLeftPanelExpanded(!state.isLeftPanelExpanded) },
            poseSuggestionText = state.poseSuggestionText,
            poseImageUrl = state.poseImageUrl,
            isLoading = state.poseLoading,
            errorMessage = state.poseErrorMessage,
            showConfirmButton = state.selectedMode == CameraMode.AiPose,
            onConfirm = { viewModel.requestPoseGuidance(lifecycleOwnerRef) },
            onClear = { viewModel.clearPoseGuidanceText() },
            modifier = Modifier.align(Alignment.CenterStart)
        )

        // 顶部 AI 文本框：显示用户语音 + guideText
        if (state.isAiVoiceBoxVisible) {
            AiSuggestionStatusBox(
                recognizedText = state.voiceRecognitionResult,
                guideText = state.poseGuideText,
                onClear = {
                    viewModel.clearVoiceRecognitionText()
                    viewModel.clearPoseGuidanceText()
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
                    .fillMaxWidth(0.92f)
            )
        }

        // 时间显示：小字、半透明
        TimeDisplay(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        )

        // 对焦框显示
        if (state.focusState != CameraController.FocusState.Idle) {
            FocusIndicator(
                focusPointX = state.focusPointX,
                focusPointY = state.focusPointY,
                focusState = state.focusState,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 底部控制区：仿系统相机
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 变焦按钮（你已有组件，保持）
            ZoomButton(
                currentZoom = state.zoom.currentZoom,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // 拍照/语音/切镜头 + 模式 Tabs（仍旧交给原层，但让其吃主题）
            CameraControlsLayer(
                uiState = state.cameraState,
                onTakePicture = { viewModel.takePicture() },
                onSwitchCamera = {
                    previewViewRef.value?.let { previewView ->
                        viewModel.switchCamera(lifecycleOwnerRef, previewView)
                    }
                },
                selectedMode = state.selectedMode,
                modes = cameraModes,
                onModeSelected = { viewModel.setSelectedMode(it) },
                onVoiceStateChange = { isActive -> if (isActive) viewModel.startListening() else viewModel.stopListening() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 错误提示覆盖层
        if (state.cameraState == CameraState.Error && !state.errorMessage.isNullOrEmpty()) {
            ErrorOverlay(
                message = state.errorMessage,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 加载状态提示
        if (state.cameraState == CameraState.Initializing || state.cameraState == CameraState.Taking || state.cameraState == CameraState.Saving) {
            LoadingOverlay(
                status = when (state.cameraState) {
                    CameraState.Initializing -> "初始化相机..."
                    CameraState.Taking -> "拍照中..."
                    CameraState.Saving -> "保存照片..."
                    else -> ""
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 照片保存成功提示
        if (state.cameraState == CameraState.PhotoSaved) {
            SaveSuccessOverlay(modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * 相机预览层（支持手势交互）
 */
@Composable
private fun CameraPreviewLayer(
    viewModel: CameraViewModel,
    lifecycleOwner: LifecycleOwner,
    onPreviewViewReady: (androidx.camera.view.PreviewView) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 保存上一次的触摸位置，用于滑动变焦
    val lastTouchY = remember { mutableStateOf(0f) }

    AndroidView(
        factory = { ctx ->
            androidx.camera.view.PreviewView(ctx).apply {
                viewModel.initializeCamera(lifecycleOwner, this)
                onPreviewViewReady(this)
            }
        },
        update = { view ->
            val previewView = view as androidx.camera.view.PreviewView
            // 设置手势检测器
            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    // 单点击对焦
                    val x = e.x / previewView.width
                    val y = e.y / previewView.height
                    viewModel.autoFocus(x, y)
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    // 长按锁定对焦
                    val x = e.x / previewView.width
                    val y = e.y / previewView.height
                    viewModel.lockFocus(x, y)
                }
            })

            // 设置 OnTouchListener 处理手势
            previewView.setOnTouchListener { v, event ->
                // 处理双指捏合变焦
                val handled = when (event.pointerCount) {
                    2 -> handlePinchZoom(event, viewModel)
                    1 -> {
                        // 处理单指滑动变焦（在屏幕左右边缘）
                        val screenWidth = previewView.width
                        val touchX = event.x
                        val edgeThreshold = screenWidth * 0.1f // 10% 的屏幕宽度作为边缘阈值

                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                // 记录初始触摸位置
                                lastTouchY.value = event.y
                            }

                            MotionEvent.ACTION_MOVE -> {
                                // 检查是否在屏幕左右边缘
                                if (touchX < edgeThreshold || touchX > screenWidth - edgeThreshold) {
                                    val deltaY = event.y - lastTouchY.value
                                    if (kotlin.math.abs(deltaY) > 5) {
                                        // 向上滑动放大，向下滑动缩小
                                        val zoomFactor = if (deltaY < 0) 0.05f else -0.05f
                                        val currentZoom = viewModel.cameraController.getCurrentZoom()
                                        viewModel.setZoom(currentZoom + zoomFactor, animate = false)
                                        lastTouchY.value = event.y
                                    }
                                } else {
                                    // 不在边缘，使用默认手势处理
                                    gestureDetector.onTouchEvent(event)
                                }
                            }

                            else -> {
                                gestureDetector.onTouchEvent(event)
                            }
                        }
                        true
                    }

                    else -> gestureDetector.onTouchEvent(event)
                }

                // 无障碍：单指抬起才 performClick（避免拖动/双指手势误触发）
                if (event.pointerCount == 1 && event.action == MotionEvent.ACTION_UP) {
                    v.performClick()
                }

                handled
            }
        },
        modifier = modifier
    )
}

/**
 * 处理双指捏合变焦
 */
private fun handlePinchZoom(event: MotionEvent, viewModel: CameraViewModel): Boolean {
    return when (event.action and MotionEvent.ACTION_MASK) {
        MotionEvent.ACTION_POINTER_DOWN -> {
            // 记录初始距离
            lastPinchDistance = calculateDistance(event)
            true
        }
        MotionEvent.ACTION_MOVE -> {
            val currentDistance = calculateDistance(event)
            val distanceDelta = currentDistance - lastPinchDistance

            if (kotlin.math.abs(distanceDelta) > 10) {
                // 根据距离变化调整变焦
                val zoomFactor = if (distanceDelta > 0) 0.05f else -0.05f
                val currentZoom = viewModel.cameraController.getCurrentZoom()
                viewModel.setZoom(currentZoom + zoomFactor, animate = false)
                lastPinchDistance = currentDistance
            }
            true
        }
        else -> false
    }
}

private var lastPinchDistance = 0f

/**
 * 计算双指间距离
 */
private fun calculateDistance(event: MotionEvent): Float {
    if (event.pointerCount < 2) return 0f

    val x1 = event.getX(0)
    val y1 = event.getY(0)
    val x2 = event.getX(1)
    val y2 = event.getY(1)

    val dx = x2 - x1
    val dy = y2 - y1

    return sqrt(dx * dx + dy * dy)
}

@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
}
