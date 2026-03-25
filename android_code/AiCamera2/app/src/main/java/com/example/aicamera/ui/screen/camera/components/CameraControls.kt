package com.example.aicamera.ui.screen.camera.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.aicamera.ui.uistate.camera.CameraMode
import com.example.aicamera.ui.uistate.camera.CameraState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults

@Composable
fun CameraControlsLayer(
    uiState: CameraState,
    onTakePicture: () -> Unit,
    onSwitchCamera: () -> Unit,
    selectedMode: CameraMode,
    modes: List<CameraMode>,
    onModeSelected: (CameraMode) -> Unit,
    //onVoiceStateChange: (Boolean) -> Unit,  //星火语音激活函数
    modifier: Modifier = Modifier,
    onOpenGallery: () -> Unit?, //打开相册的回调
    isVoiceActive: Boolean,     //语音激活参数
    onVoiceClick: () -> Unit,   //语音点击事件
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraModeTabs(
                modes = modes,
                selectedMode = selectedMode,
                onModeSelected = onModeSelected,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(12.dp)) // 两个组件之间的间距

            SwitchCameraButton(
                onSwitchCamera = onSwitchCamera,
                modifier = Modifier
                    .size(width = 44.dp, height = 36.dp) // 高度与 CameraModeTabs 保持一致
                    .background(
                        color = Color.White.copy(alpha = 0.10f), // 使用相同的半透明背景
                        shape = RoundedCornerShape(12.dp)
                    )

            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClickToggleVoiceButton(
                modifier = Modifier
                    .size(80.dp)
                    .padding(10.dp),
                //onVoiceStateChange = onVoiceStateChange,
                onClick = onVoiceClick,
                isActive = isVoiceActive,
            )

            ShutterButton(
                enabled = uiState == CameraState.Ready,
                onClick = onTakePicture,
                modifier = Modifier
                    .size(96.dp)
            )

            //相册按钮
            IconButton(
                onClick = {onOpenGallery?.invoke()} ,
                modifier = Modifier.size(56.dp), // 增大点击区域
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.16f), // 参照您提供的旧样式
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Photo, // 参照您提供的旧图标
                    contentDescription = "打开相册",
                    modifier = Modifier.size(28.dp)
                )
            }

        }
    }
}

@Composable
fun ShutterButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .border(width = 5.dp, color = Color.White, shape = CircleShape)
        )
        Box(
            modifier = Modifier
                .size(74.dp)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun SwitchCameraButton(
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .background(Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable(
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onSwitchCamera
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "切换摄像头",
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
        )
    }

}

// 修改语音按钮
@Composable
fun ClickToggleVoiceButton(
    isActive: Boolean, // 状态由外部传入
    onClick: () -> Unit, // 点击事件回调
    modifier: Modifier = Modifier,
    //onVoiceStateChange: (Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .size(56.dp) // 建议统一尺寸
            .background(
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                shape = CircleShape
            )
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.18f), shape = CircleShape)
            .clickable(onClick = {
                onClick()
                Log.d("语音状态显示","$isActive")
            }),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            // 使用系统内置图标，切换 Mic 和波纹图标
            imageVector = if (isActive) Icons.Default.SettingsVoice else Icons.Default.Mic,
            contentDescription = "语音状态",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

/*
@Composable
fun ClickToggleVoiceButton(
    modifier: Modifier = Modifier,
    onVoiceStateChange: (Boolean) -> Unit
) {
    var isVoiceActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                color = if (isVoiceActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.12f),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = CircleShape
            )
            .clickable(
                role = Role.Button,
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    isVoiceActive = !isVoiceActive
                    onVoiceStateChange(isVoiceActive)
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isVoiceActive) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val strokeWidth = 1.5.dp.toPx()
                drawLine(color = Color.White, start = Offset(size.width * 0.2f, size.height * 0.4f), end = Offset(size.width * 0.2f, size.height * 0.6f), strokeWidth = strokeWidth)
                drawLine(color = Color.White, start = Offset(size.width * 0.5f, size.height * 0.2f), end = Offset(size.width * 0.5f, size.height * 0.8f), strokeWidth = strokeWidth)
                drawLine(color = Color.White, start = Offset(size.width * 0.8f, size.height * 0.3f), end = Offset(size.width * 0.8f, size.height * 0.7f), strokeWidth = strokeWidth)
            }
        } else {
            Canvas(modifier = Modifier.size(26.dp)) {
                val strokeWidth = 2.dp.toPx()
                val centerX = size.width * 0.5f

                val micWidth = size.width * 0.2f
                val micHeight = size.height * 0.25f
                val micCornerRadius = micWidth / 2

                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(centerX - micWidth / 2, size.height * 0.15f),
                    size = Size(micWidth, micHeight),
                    cornerRadius = CornerRadius(micCornerRadius, micCornerRadius),
                    style = Stroke(width = strokeWidth)
                )

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
*/