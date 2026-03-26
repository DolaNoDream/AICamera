package com.example.aicamera.ui.screen.album

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PhotoDetailTopBar(
    onBack: (() -> Unit)?,
    isLoading: Boolean,
    isBusy: Boolean,
    onWriteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f), // 轻微半透明背景，保证在不同图片下可见
        modifier = modifier.fillMaxWidth().statusBarsPadding() // 适配状态栏
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    enabled = !isLoading,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            } else { Spacer(modifier = Modifier.width(48.dp)) }

            Spacer(modifier = Modifier.weight(1f))

            // 操作按钮组
            val buttonEnabled = !isLoading && !isBusy

            ActionButton(text = "AI文案", enabled = buttonEnabled, onClick = onWriteClick)
            Spacer(modifier = Modifier.width(8.dp))
            ActionButton(text = "AI修图", enabled = buttonEnabled, onClick = onEditClick)
            Spacer(modifier = Modifier.width(8.dp))
            ActionButton(text = "删除", enabled = buttonEnabled, isError = true, onClick = onDeleteClick)
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isError: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, if (isError) MaterialTheme.colorScheme.error else Color.White),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isError) MaterialTheme.colorScheme.error else Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.3f)
        )
    ) { Text(text = text, style = MaterialTheme.typography.labelLarge) }
}

/**
 * 下方的信息与文案展示区域
 */
@Composable
fun PhotoInfoSection(photo: com.example.aicamera.data.db.entity.AlbumPhotoEntity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoItem(label = "文件路径", value = photo.filePath)
            InfoItem(label = "照片类型", value = when(photo.type) { 1 -> "精修图"; else -> "相机原图" })
            InfoItem(label = "尺寸", value = "${photo.width ?: 0} x ${photo.height ?: 0}")

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Text(text = "AI文案", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(
                text = photo.text ?: "暂无文案，点击上方按钮生成",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f, false))
    }
}