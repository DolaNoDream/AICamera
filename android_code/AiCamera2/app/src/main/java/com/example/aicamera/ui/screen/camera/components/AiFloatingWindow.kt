package com.example.aicamera.ui.screen.camera.components


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aicamera.ui.uistate.camera.FloatingWindowStatus
import com.example.aicamera.ui.uistate.camera.FloatingWindowPosition

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AiFloatingWindow(
    status: FloatingWindowStatus,
    position: FloatingWindowPosition,
    voiceText: String,
    onIconClick: () -> Unit,      // 点击圆形图标
    onButtonClick: () -> Unit,    // 点击展开后的按钮
    modifier: Modifier = Modifier,
    offset: IntOffset,              // 传入当前偏移量
    onDrag: (Offset) -> Unit,       // 拖动回调
    onDragEnd: () -> Unit,          //拖动结束回调

) {
    var haptic = LocalHapticFeedback.current

    // 整体容器，使用 animateContentSize 实现平滑的尺寸变化动画
    Row(
        modifier = modifier
            .offset { offset } // 应用偏移量，让组件随手指移动
            .pointerInput(Unit) {
                // 检测长按后的拖动
                detectDragGesturesAfterLongPress(
                    onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onDragEnd = { },
                    onDragCancel = { },
                    onDrag = { change, dragAmount ->
                        change.consume() // 消费掉事件，不传递给下层
                        onDrag(dragAmount)
                    }
                )
            }
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                // 展开状态下是圆角矩形，折叠状态下是圆形
                shape = if (status == FloatingWindowStatus.Activated) RoundedCornerShape(24.dp) else CircleShape
            )
            .animateContentSize(animationSpec = tween(durationMillis = 300)) // 尺寸变化动画
            .padding(4.dp), // 内边距，让内部组件离边缘有一定距离
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // --- 1. 圆形图标部分 (总是显示) ---
        Box(
            modifier = Modifier
                .size(48.dp) // 图标大小
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onIconClick() }, // 点击切换状态
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AllInclusive, // 麦克风图标
                contentDescription = "AI 语音",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        // --- 2. 横向展开模块 (仅在 Activated 状态下显示) ---
        // 使用 AnimatedVisibility 实现内容出现/消失的动画
        AnimatedVisibility(
            visible = status == FloatingWindowStatus.Activated,
            // 根据位置决定是从左滑出还是从右滑出
            enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { fullWidth ->
                if (position == FloatingWindowPosition.Left) -fullWidth else fullWidth
            },
            exit = fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { fullWidth ->
                if (position == FloatingWindowPosition.Left) -fullWidth else fullWidth
            }
        ) {
            // 展开后的内容区
            Column(
                modifier = Modifier
                    .widthIn(min = 150.dp, max = 250.dp) // 限制展开宽度
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp) // 组件之间的间距
            ) {
                // 语音转文字内容
                Text(
                    text = voiceText,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 3, // 最多显示3行
                    overflow = TextOverflow.Ellipsis, // 超出显示省略号
                    style = MaterialTheme.typography.bodyMedium
                )

                // 处理按钮
                Button(
                    onClick = onButtonClick,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "应用建议",
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}