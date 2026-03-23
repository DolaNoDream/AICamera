package com.example.aicamera.ui.screen.album

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import com.example.aicamera.data.network.copywriter.CopywriterRequirement
import com.example.aicamera.ui.viewmodel.album.AlbumListViewModel
import kotlinx.coroutines.launch

/**
 * 相册列表页：支持多选照片生成文案。
 */
@Composable
fun AlbumListScreen(
    viewModel: AlbumListViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onPhotoClick: ((photoId: Long) -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val view = LocalView.current
    val density = LocalDensity.current
    val statusBarTopPaddingDp = remember(view, density) {
        val insets = ViewCompat.getRootWindowInsets(view)
        val px = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        with(density) { px.toDp() }
    }

    val showAiWriteDialog = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf(false) }

    // requirement 输入（复用 PhotoDetail 的字段定义，仅放一个弹窗即可）
    val writeType = remember { mutableStateOf("") }
    val writeEmotion = remember { mutableStateOf("") }
    val writeTheme = remember { mutableStateOf("") }
    val writeStyle = remember { mutableStateOf("") }
    val writeLength = remember { mutableStateOf("") }
    val writeSpecial = remember { mutableStateOf("") }
    val writeCustom = remember { mutableStateOf("") }

    LaunchedEffect(state.aiWriteMessage) {
        val msg = state.aiWriteMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (showAiWriteDialog.value) {
        AlertDialog(
            onDismissRequest = { if (!state.isAiWriting) showAiWriteDialog.value = false },
            title = {
                Text(
                    text = "多图生成文案（已选 ${state.selectedPhotoIds.size} 张）",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = writeType.value,
                        onValueChange = { writeType.value = it },
                        singleLine = true,
                        label = { Text("类型(type)") },
                        enabled = !state.isAiWriting
                    )
                    OutlinedTextField(
                        value = writeEmotion.value,
                        onValueChange = { writeEmotion.value = it },
                        singleLine = true,
                        label = { Text("情感(emotion)") },
                        enabled = !state.isAiWriting
                    )
                    OutlinedTextField(
                        value = writeTheme.value,
                        onValueChange = { writeTheme.value = it },
                        singleLine = true,
                        label = { Text("主题(theme)") },
                        enabled = !state.isAiWriting
                    )
                    OutlinedTextField(
                        value = writeStyle.value,
                        onValueChange = { writeStyle.value = it },
                        singleLine = true,
                        label = { Text("风格(style)") },
                        enabled = !state.isAiWriting
                    )
                    OutlinedTextField(
                        value = writeLength.value,
                        onValueChange = { writeLength.value = it },
                        singleLine = true,
                        label = { Text("长度(length)") },
                        enabled = !state.isAiWriting
                    )
                    OutlinedTextField(
                        value = writeSpecial.value,
                        onValueChange = { writeSpecial.value = it },
                        singleLine = true,
                        label = { Text("特殊要求(special)") },
                        enabled = !state.isAiWriting
                    )
                    OutlinedTextField(
                        value = writeCustom.value,
                        onValueChange = { writeCustom.value = it },
                        singleLine = false,
                        label = { Text("自定义(custom)") },
                        enabled = !state.isAiWriting
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !state.isAiWriting,
                    onClick = {
                        val requirement = CopywriterRequirement(
                            type = writeType.value.trim().ifBlank { null },
                            emotion = writeEmotion.value.trim().ifBlank { null },
                            theme = writeTheme.value.trim().ifBlank { null },
                            style = writeStyle.value.trim().ifBlank { null },
                            length = writeLength.value.trim().ifBlank { null },
                            special = writeSpecial.value.trim().ifBlank { null },
                            custom = writeCustom.value.trim().ifBlank { null },
                        )

                        val effectiveReq = if (
                            requirement.type.isNullOrBlank() &&
                            requirement.emotion.isNullOrBlank() &&
                            requirement.theme.isNullOrBlank() &&
                            requirement.style.isNullOrBlank() &&
                            requirement.length.isNullOrBlank() &&
                            requirement.special.isNullOrBlank() &&
                            requirement.custom.isNullOrBlank()
                        ) null else requirement

                        viewModel.aiWriteForSelectedPhotos(effectiveReq) { ok, _ ->
                            scope.launch {
                                if (ok) {
                                    showAiWriteDialog.value = false
                                    // 清空输入
                                    writeType.value = ""
                                    writeEmotion.value = ""
                                    writeTheme.value = ""
                                    writeStyle.value = ""
                                    writeLength.value = ""
                                    writeSpecial.value = ""
                                    writeCustom.value = ""
                                    // 生成成功后保留选择，方便用户继续；也可在这里 clear
                                }
                            }
                        }
                    }
                ) {
                    Text(text = if (state.isAiWriting) "生成中..." else "生成并保存")
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !state.isAiWriting,
                    onClick = { showAiWriteDialog.value = false }
                ) {
                    Text("取消")
                }
            }
        )
    }

    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { if (!state.isAiWriting) showDeleteDialog.value = false },
            title = { Text("确认删除") },
            text = { Text("将删除已选 ${state.selectedPhotoIds.size} 张照片（仅删除本地数据库记录，关联表会自动清理）。是否继续？") },
            confirmButton = {
                TextButton(
                    enabled = !state.isAiWriting,
                    onClick = {
                        viewModel.deleteSelectedPhotos { ok, _ ->
                            scope.launch {
                                if (ok) {
                                    showDeleteDialog.value = false
                                }
                            }
                        }
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isAiWriting,
                    onClick = { showDeleteDialog.value = false }
                ) {
                    Text("取消")
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(statusBarTopPaddingDp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Text(
                        text = "相册",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }

                    !state.errorMessage.isNullOrBlank() && state.photos.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = state.errorMessage ?: "加载失败",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    state.photos.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "暂无照片，请先拍照",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.photos, key = { it.id }) { photo ->
                                val uri = runCatching { photo.filePath.toUri() }.getOrNull()
                                val selected = state.selectedPhotoIds.contains(photo.id)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .combinedClickable(
                                            enabled = !state.isAiWriting,
                                            indication = LocalIndication.current,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = {
                                                if (state.selectedPhotoIds.isNotEmpty()) {
                                                    // 多选模式：点击切换选择
                                                    viewModel.toggleSelect(photo.id)
                                                } else {
                                                    // 非多选模式：进入详情
                                                    onPhotoClick?.invoke(photo.id)
                                                }
                                            },
                                            onLongClick = {
                                                // 长按进入多选/切换选择
                                                viewModel.toggleSelect(photo.id)
                                            }
                                        )
                                ) {
                                    if (uri != null) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = "无效路径",
                                            modifier = Modifier.align(Alignment.Center),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    if (selected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                        )
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(22.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }

            // 右下角悬浮按钮（更显眼）：多图生成文案
            if (state.selectedPhotoIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    // 批量删除
                    Button(
                        enabled = state.selectedPhotoIds.isNotEmpty() && !state.isAiWriting,
                        onClick = { showDeleteDialog.value = true },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("删除")
                    }

                    // 清空选择
                    Button(
                        enabled = state.selectedPhotoIds.isNotEmpty() && !state.isAiWriting,
                        onClick = { viewModel.clearSelection() },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("取消")
                    }

                    Button(
                        enabled = !state.isAiWriting,
                        onClick = { showAiWriteDialog.value = true },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (state.isAiWriting) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text("多图生成文案")
                    }
                }
            }
        }
    }
}
