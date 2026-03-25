package com.example.aicamera.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// 定义一套圆角尺寸，模仿系统应用的柔和圆角
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), // 极小圆角（标签、小图标）
    small = RoundedCornerShape(8.dp),     // 小圆角（按钮、输入框）
    medium = RoundedCornerShape(12.dp),   // 中圆角（卡片、对话框）
    large = RoundedCornerShape(16.dp),    // 大圆角（大卡片、底部弹窗）
    extraLarge = RoundedCornerShape(28.dp) // 超大圆角（特殊组件）
)