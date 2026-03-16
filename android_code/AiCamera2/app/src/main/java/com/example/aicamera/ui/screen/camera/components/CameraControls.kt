package com.example.aicamera.ui.screen.camera.components

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

@Composable
fun CameraControlsLayer(
    uiState: CameraState,
    onTakePicture: () -> Unit,
    onSwitchCamera: () -> Unit,
    selectedMode: CameraMode,
    modes: List<CameraMode>,
    onModeSelected: (CameraMode) -> Unit,
    onVoiceStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black)
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CameraModeTabs(
            modes = modes,
            selectedMode = selectedMode,
            onModeSelected = onModeSelected,
            modifier = Modifier.padding(0.dp)
        )

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
                onVoiceStateChange = onVoiceStateChange
            )

            ShutterButton(
                enabled = uiState == CameraState.Ready,
                onClick = onTakePicture,
                modifier = Modifier
                    .size(96.dp)
            )

            SwitchCameraButton(
                onSwitchCamera = onSwitchCamera,
                modifier = Modifier
                    .size(80.dp)
                    .padding(10.dp)
            )
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
            .clickable(enabled = enabled, onClick = onClick),
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
            .background(Color.Gray, CircleShape)
            .clickable { onSwitchCamera() },
        contentAlignment = Alignment.Center
    ) {
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
    onVoiceStateChange: (Boolean) -> Unit
) {
    var isVoiceActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(64.dp)
            .background(
                color = if (isVoiceActive) Color(0xFF4A90E2) else Color.White,
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = if (isVoiceActive) Color.Transparent else Color.Gray.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(
                role = Role.Button,
                onClick = {
                    isVoiceActive = !isVoiceActive
                    onVoiceStateChange(isVoiceActive)
                }
            ),
        contentAlignment = Alignment.Center
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
