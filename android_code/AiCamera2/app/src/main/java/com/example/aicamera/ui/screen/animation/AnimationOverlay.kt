package com.example.aicamera.ui.screen.animation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.example.aicamera.R

/**
 * 全屏语音激活动画
 */
@Composable
fun VoiceActiveLottieOverlay(isVisible: Boolean) {
    if (!isVisible) return

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.voice_active))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever // 永久循环
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)), // 背景半透明遮罩
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(240.dp) // 语音动画可以大一些
        )
    }
}

/**
 * 居中加载动画
 */
@Composable
fun LoadingLottieOverlay(isLoading: Boolean) {
    if (!isLoading) return

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 如果需要背景遮罩可以加，加载动画通常建议背景更透明或无背景
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(100.dp)
        )
    }
}