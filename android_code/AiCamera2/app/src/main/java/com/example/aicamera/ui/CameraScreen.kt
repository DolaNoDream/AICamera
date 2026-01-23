package com.example.aicamera.ui

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Menu
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.example.aicamera.R
import com.example.aicamera.camera.CameraController
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
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState.collectAsState().value
    val errorMessage = viewModel.errorMessage.collectAsState().value
    val aiAdvice = viewModel.aiAdvice.collectAsState().value
    val voiceGuideEnabled = viewModel.voiceGuideEnabled.collectAsState().value
    val currentZoom = viewModel.currentZoom.collectAsState().value
    val zoomRangeInfo = viewModel.zoomRangeInfo.collectAsState().value
    val focusState = viewModel.focusState.collectAsState().value
    val focusPointX = viewModel.focusPointX.collectAsState().value
    val focusPointY = viewModel.focusPointY.collectAsState().value
    val voiceRecognitionResult = viewModel.voiceRecognitionResult.collectAsState().value
    val isListening = viewModel.isListening.collectAsState().value

    val context = LocalContext.current
    val lifecycleOwnerRef = lifecycleOwner
    val previewViewRef = remember { mutableStateOf<androidx.camera.view.PreviewView?>(null) }
    
    // 状态变量
    val isMenuExpanded = remember { mutableStateOf(false) }
    val isLeftPanelExpanded = remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrEmpty()) {
            val rootView = (context as? android.app.Activity)?.window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
            if (rootView != null) {
                Snackbar.make(rootView, errorMessage, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is CameraUIState.PhotoSaved) {
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
        // 相机预览层（支持点击对焦和手势变焦）
        CameraPreviewLayer(
            viewModel = viewModel,
            lifecycleOwner = lifecycleOwner,
            onPreviewViewReady = { previewView ->
                previewViewRef.value = previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 顶部控制栏
        TopControllerBar(
            isMenuExpanded = isMenuExpanded.value,
            onMenuClick = { isMenuExpanded.value = !isMenuExpanded.value },
            modifier = Modifier.align(Alignment.TopStart)
        )
        
        // 左侧隐藏式菜单
        LeftHiddenMenu(
            isExpanded = isLeftPanelExpanded.value,
            onToggle = { isLeftPanelExpanded.value = !isLeftPanelExpanded.value },
            modifier = Modifier.align(Alignment.CenterStart)
        )

        // AI 建议显示
        if (aiAdvice.isNotEmpty()) {
            AISuggestionBox(
                advice = aiAdvice,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .fillMaxWidth(0.9f)
            )
        }

        // 语音识别结果显示
        if (voiceRecognitionResult.isNotEmpty()) {
            VoiceRecognitionResultBox(
                result = voiceRecognitionResult,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.9f)
            )
        }

        // 时间显示（系统时间框1秒后自动隐藏）
        TimeDisplay(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        )

        // 对焦框显示
        if (focusState != CameraController.FocusState.Idle) {
            FocusIndicator(
                focusPointX = focusPointX,
                focusPointY = focusPointY,
                focusState = focusState,
                modifier = Modifier.fillMaxSize()
            )
        }

//        // 实时滤镜预览条
//        FilterBar(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(bottom = 140.dp)
//        )



        // 拍照按钮和控制区域
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp) // Zoom控件和CameraControlsLayer间距4dp
        ) {
            // 变焦按钮 + 变焦滑块
            ZoomButton(
                currentZoom = currentZoom,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // 变焦滑块
            if (zoomRangeInfo != null) {
                ZoomSlider(
                    currentZoom = currentZoom,
                    minZoom = zoomRangeInfo.first,
                    maxZoom = zoomRangeInfo.second,
                    onZoomChange = { viewModel.setZoom(it, animate = true) },
                    modifier = Modifier.fillMaxWidth()
                )
            }


            // 原控制层（已移除变焦控件）
            CameraControlsLayer(
                viewModel = viewModel,
                uiState = uiState,
                voiceGuideEnabled = voiceGuideEnabled,
                isListening = isListening,
                onTakePicture = { viewModel.takePicture() },
                onToggleVoiceGuide = { viewModel.toggleVoiceGuide() },
                onStartListening = { viewModel.startListening() },
                onStopListening = { viewModel.stopListening() },
                onSwitchCamera = {
                    previewViewRef.value?.let { previewView ->
                        viewModel.switchCamera(lifecycleOwnerRef, previewView)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        // 错误提示覆盖层
        if (uiState is CameraUIState.Error && !errorMessage.isNullOrEmpty()) {
            ErrorOverlay(
                message = errorMessage,
                onRetry = { /* 权限处理在 MainActivity 中进行 */ },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 加载状态提示
        if (uiState is CameraUIState.Initializing || uiState is CameraUIState.Taking || uiState is CameraUIState.Saving) {
            LoadingOverlay(
                status = when (uiState) {
                    CameraUIState.Initializing -> "初始化相机..."
                    CameraUIState.Taking -> "拍照中..."
                    CameraUIState.Saving -> "保存照片..."
                    else -> ""
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 照片保存成功提示
        if (uiState is CameraUIState.PhotoSaved) {
            SaveSuccessOverlay(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 相机模式切换标签栏
 */
@Composable
private fun CameraModeTabs(modifier: Modifier = Modifier) {
    val modes = listOf(
        CameraMode("AI建议文本显示"),
        CameraMode("AI姿势指导"),
    )
    val selectedMode = remember { mutableStateOf(modes[0]) }

    Box(
        modifier = modifier
            .background(
                color = Color.Black,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(2.dp)
    ) {
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            items(modes) {
                Box(
                    modifier = Modifier
                        .background(
                            color = if (it == selectedMode.value) Color.Transparent else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable {
                            selectedMode.value = it
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = it.name,
                            color = if (it == selectedMode.value) Color(0xFFFF9800) else Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
    }
}

/**
 * 相机模式数据类
 */
data class CameraMode(val name: String)

/**
 * 滤镜数据类
 */
data class Filter(val name: String, val previewColor: Color)

/**
 * 实时滤镜预览条
 */
@Composable
private fun FilterBar(modifier: Modifier = Modifier) {
    val filters = listOf(
        Filter("原图", Color.Transparent),
        Filter("清新", Color(0xFFE1F5FE)),
        Filter("复古", Color(0xFFF5F5DC)),
        Filter("黑白", Color(0xFFE0E0E0)),
        Filter("暖色", Color(0xFFFFF3E0)),
        Filter("冷色", Color(0xFFE0F7FA))
    )
    val selectedFilter = remember { mutableStateOf(filters[0]) }

    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp)
    ) {
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            items(filters) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            color = it.previewColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = if (it == selectedFilter.value) 2.dp else 0.dp,
                            color = Color(0xFF81C784)
                        )
                        .clickable {
                            selectedFilter.value = it
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = it.name,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
    }
}

/**
 * 时间显示（半透明悬浮样式，1秒后自动隐藏）
 */
@Composable
private fun TimeDisplay(modifier: Modifier = Modifier) {
    // 获取当前 Activity 的上下文（关键：只有 Activity 能拿到 Window）
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // 1秒后隐藏系统状态栏
    LaunchedEffect(Unit) {
        // 延迟1秒
        kotlinx.coroutines.delay(1000)

        // 切换到主线程执行（UI 操作必须在主线程）
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            activity?.window?.let { window ->
                // 隐藏状态栏（包含系统时间、电量等）
                window.decorView.systemUiVisibility =
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or  // 全屏（隐藏状态栏）
                            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN  // 布局延伸到状态栏区域
                // 隐藏状态栏后，Activity 布局全屏显示
                window.setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }
        }
    }

    // 恢复状态栏（比如退出相机界面时），添加 DisposableEffect 自动恢复
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let { window ->
                // 恢复状态栏显示
                window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
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
        update = { previewView ->
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
            previewView.setOnTouchListener { _, event ->
                // 处理双指捏合变焦
                when (event.pointerCount) {
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

/**
 * 对焦指示器（对焦框）
 */
@Composable
private fun FocusIndicator(
    focusPointX: Float,
    focusPointY: Float,
    focusState: CameraController.FocusState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) { // 父容器仍需占满全屏（用于计算坐标）
        // 1. 获取屏幕真实尺寸（dp转px，避免不同密度屏幕偏移）
        val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp

        val focusPointSize = 8.dp
        val xOffset = (focusPointX * screenWidth.value - focusPointSize.value / 2).dp
        val yOffset = (focusPointY * screenHeight.value - focusPointSize.value / 2).dp

        // 4. 渲染小圆点对焦点（不同状态不同颜色）
        Box(
            modifier = Modifier
                .size(focusPointSize) // 小圆点尺寸（核心修改）
                .offset(x = xOffset, y = yOffset) // 精准定位到触摸点
                .clip(CircleShape) // 切成圆形（核心修改）
                .background(
                    color = when (focusState) {
                        CameraController.FocusState.Focusing -> Color.Yellow.copy(alpha = 0.8f) // 对焦中：半透黄
                        CameraController.FocusState.Locked -> Color.Green.copy(alpha = 0.8f)   // 锁定：半透绿
                        CameraController.FocusState.Failed -> Color.Red.copy(alpha = 0.8f)     // 失败：半透红
                        else -> Color.White.copy(alpha = 0.8f) // 兜底
                    }
                )
                // 可选：加一圈细边框，让圆点更清晰
                .border(
                    width = 1.dp,
                    color = Color.White,
                    shape = CircleShape
                )
        )
    }
}

/**
 * 相机控制区域
 */
@Composable
private fun CameraControlsLayer(
    viewModel: CameraViewModel,
    uiState: CameraUIState,
    voiceGuideEnabled: Boolean,
    isListening: Boolean,
    onTakePicture: () -> Unit,
    onToggleVoiceGuide: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVoicePanelExpanded = remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(
                Color.Black
            )
            .padding(top = 8.dp, bottom = 16.dp, start = 0.dp, end = 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 模式切换标签栏
        CameraModeTabs(
            modifier = Modifier.padding(0.dp)
        )

        // 变焦快速按钮和拍照按钮行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp, start = 0.dp, end = 0.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //TODO:增加语音识别功能
            //语音识别按钮
            ClickToggleVoiceButton(
                modifier = Modifier.size(80.dp)
                    .padding(10.dp),
                onVoiceStateChange = { isActive ->
                    if (isActive) {
                        println("语音识别已开启")
                        viewModel.startListening()
                    } else {
                        println("语音识别已关闭")
                        viewModel.stopListening()
                    }
                }
            )

            //拍照按钮
            ShutterButton(
                enabled = uiState is CameraUIState.Ready,
                onClick = onTakePicture,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
            )

            //摄像头转换按钮
            SwitchCameraButton(
                onSwitchCamera,
                modifier = Modifier
                    .size(80.dp)
                    .padding(10.dp)
            )
        }
    }
}

/**
 * 变焦滑块控件
 */
@Composable
private fun ZoomSlider(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            value = currentZoom,
            onValueChange = onZoomChange,
            valueRange = minZoom..maxZoom,
            steps = (maxZoom - minZoom).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(id = R.string.zoom_value, currentZoom),
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * 焦距展示按钮
 */
@Composable
private fun ZoomButton(
    currentZoom: Float,
    modifier: Modifier = Modifier
) {
    // 为缩放值添加平滑的数字过渡动画
    val animatedZoom = animateFloatAsState(
        targetValue = currentZoom,
        animationSpec = tween(durationMillis = 300)
    )

    Row(
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.15f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            )
            .shadow(
                elevation = 3.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.15f)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 变焦显示
        Text(
            text = String.format("%.1fx", animatedZoom.value),
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * 拍照按钮
 */
@Composable
private fun ShutterButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(96.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 外层细边框圆
        Box(
            modifier = Modifier
                .size(96.dp)
                .border(
                    width = 5.dp,
                    color = Color.White,
                    shape = CircleShape
                )
        )
        // 内层实心圆
        Box(
            modifier = Modifier
                .size(74.dp)
                .background(Color.White, CircleShape)
        )
    }
}

//前后摄像头转换按钮
@Composable
fun SwitchCameraButton(
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .background(Color.Gray, CircleShape)
            .clickable { onSwitchCamera() },
        contentAlignment = Alignment.Center
    ) {
        // 旋转箭头图标（使用系统自带图标）
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "切换摄像头",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ClickToggleVoiceButton(
    modifier: Modifier = Modifier,
    onVoiceStateChange: (Boolean) -> Unit // 状态变化回调：true=开启，false=关闭
) {
    // 记录语音按钮状态（false=原始，true=开启）
    var isVoiceActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(64.dp) // 按钮大小匹配图片
            // 背景色：原始=白色，开启=蓝色
            .background(
                color = if (isVoiceActive) Color(0xFF4A90E2) else Color.White,
                shape = CircleShape
            )
            // 边框：原始=浅灰细边框，开启=无边框
            .border(
                width = 1.dp,
                color = if (isVoiceActive) Color.Transparent else Color.Gray.copy(alpha = 0.3f),
                shape = CircleShape
            )
            // 点击切换状态
            .clickable(
                role = Role.Button,
                onClick = {
                    isVoiceActive = !isVoiceActive
                    onVoiceStateChange(isVoiceActive)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // 根据状态绘制不同图标
        if (isVoiceActive) {
            // 声波图标（保持之前的极简风格）
            Canvas(modifier = Modifier.size(24.dp)) {
                val strokeWidth = 1.5.dp.toPx()
                drawLine(color = Color.White, start = Offset(size.width * 0.2f, size.height * 0.4f), end = Offset(size.width * 0.2f, size.height * 0.6f), strokeWidth = strokeWidth)
                drawLine(color = Color.White, start = Offset(size.width * 0.5f, size.height * 0.2f), end = Offset(size.width * 0.5f, size.height * 0.8f), strokeWidth = strokeWidth)
                drawLine(color = Color.White, start = Offset(size.width * 0.8f, size.height * 0.3f), end = Offset(size.width * 0.8f, size.height * 0.7f), strokeWidth = strokeWidth)
            }
        } else {
            // 绘制麦克风
            Canvas(modifier = Modifier.size(26.dp)) {
                val strokeWidth = 2.dp.toPx()
                val centerX = size.width * 0.5f

                // 1. 话筒：竖向的圆角矩形
                val micWidth = size.width * 0.2f  // 宽度较小，呈现竖向
                val micHeight = size.height * 0.25f
                val micCornerRadius = micWidth / 2  // 圆角半径为宽度的一半，使其更圆润

                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(centerX - micWidth / 2, size.height * 0.15f),
                    size = Size(micWidth, micHeight),
                    cornerRadius = CornerRadius(micCornerRadius, micCornerRadius),
                    style = Stroke(width = strokeWidth)
                )

                // 2. 半圆形的底座（从0-180度，开口向上）
                val baseRadius = size.width * 0.15f
                val baseCenterY = size.height * 0.55f

                drawArc(
                    color = Color.Black,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(centerX - baseRadius, baseCenterY - baseRadius),
                    size = Size(baseRadius * 2, baseRadius * 2),
                    style = Stroke(width = strokeWidth)
                )

                // 3. 画一条竖线（从半圆中心点向下）
                drawLine(
                    color = Color.Black,
                    start = Offset(centerX, baseCenterY),
                    end = Offset(centerX, size.height * 0.8f),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}

/**
 * AI 建议显示框（预留占位，当前隐藏）
 */
@Composable
private fun AISuggestionBox(
    advice: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.ai_suggestion),
                color = Color(0xFFFFC107),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = advice,
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 语音识别结果显示框
 */
@Composable
private fun VoiceRecognitionResultBox(
    result: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("语音识别结果", color = Color(0xFF81C784), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(result, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

/**
 * 加载状态覆盖层
 */
@Composable
private fun LoadingOverlay(
    status: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = status,
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * 保存成功提示
 */
@Composable
private fun SaveSuccessOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.photo_taken),
                color = Color.Green,
                fontSize = 18.sp
            )
        }
    }
}

/**
 * 错误提示覆盖层
 */
@Composable
private fun ErrorOverlay(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = Color(0xFF1F1F1F),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.error),
                color = Color.Red,
                fontSize = 18.sp
            )
            Text(
                text = message,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = Color(0xFF81C784),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                        )
                        .clickable { onRetry() }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.retry),
                        color = Color.White
                    )
                }
            }
        }
    }
}

// 顶部控制栏
@Composable
private fun TopControllerBar(
    isMenuExpanded: Boolean,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 顶部控制栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(24.dp), // 增加padding以增加高度到1.5倍
            contentAlignment = Alignment.TopEnd
        ) {
            // 菜单按钮
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "菜单",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp) // 稍微增大图标
                    .clickable(onClick = onMenuClick)
            )
        }
        
        // 展开的菜单
        androidx.compose.animation.AnimatedVisibility(
            visible = isMenuExpanded,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp) // 高度增加到1.5倍
                    .background(Color.Black)
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text(
                        text = "AI语音助手",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "AI姿势指导",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

// 左侧隐藏式菜单
@Composable
private fun LeftHiddenMenu(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.padding(start = 10.dp)) {
        // 细横线提示
        Box(
            modifier = Modifier
                .width(6.dp) // 加粗到两倍
                .height(300.dp) // 加高到两倍
                .background(Color.White.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(7.dp)) // 添加圆角边框
                .clickable(onClick = onToggle)
        )
        
        // 展开的面板（使用固定高度的Box包裹，避免布局偏移）
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(300.dp) // 固定高度，即使隐藏也占据空间
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }),
                exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(16.dp)
                        .clickable(onClick = onToggle) // 点击关闭
                ) {
                    Text(
                        text = "当前还未生成AI建议~",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// 导入缺失的函数
fun Modifier.border(width: androidx.compose.ui.unit.Dp, color: Color) =
    this.then(
        Modifier.background(
            color = color,
            shape = RoundedCornerShape(0.dp)
        )
    )

fun Modifier.offset(x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp) =
    this.then(
        Modifier.padding(start = x, top = y)
    )
