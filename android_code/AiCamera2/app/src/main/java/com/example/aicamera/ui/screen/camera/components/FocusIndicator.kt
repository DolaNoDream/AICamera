package com.example.aicamera.ui.screen.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.aicamera.data.camera.CameraController

@Composable
fun FocusIndicator(
    focusPointX: Float,
    focusPointY: Float,
    focusState: CameraController.FocusState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp

        val focusPointSize = 8.dp
        val xOffset = (focusPointX * screenWidth.value - focusPointSize.value / 2).dp
        val yOffset = (focusPointY * screenHeight.value - focusPointSize.value / 2).dp

        Box(
            modifier = Modifier
                .size(focusPointSize)
                .offset(x = xOffset, y = yOffset)
                .clip(CircleShape)
                .background(
                    color = when (focusState) {
                        CameraController.FocusState.Focusing -> Color.Yellow.copy(alpha = 0.8f)
                        CameraController.FocusState.Locked -> Color.Green.copy(alpha = 0.8f)
                        CameraController.FocusState.Failed -> Color.Red.copy(alpha = 0.8f)
                        else -> Color.White.copy(alpha = 0.8f)
                    }
                )
                .border(width = 1.dp, color = Color.White, shape = CircleShape)
        )
    }
}
