package com.example.aicamera.ui.screen.album

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.aicamera.ui.screen.animation.LoadingLottieOverlay
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
    onNavigateToCopywriting: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()

    // 根据筛选过滤后的展示列表
    val displayPhotos = remember(state.photos, state.photoTypeFilter) {
        when (val t = state.photoTypeFilter) {
            null -> state.photos
            else -> state.photos.filter { it.type == t }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    val showAiWriteDialog = remember { mutableStateOf(false) }
    val showResultDialog = remember { mutableStateOf(false) } // 结果抽屉
    val generatedText = remember { mutableStateOf("") } // 存储生成的文本
    val showDeleteDialog = remember { mutableStateOf(false) }


    LaunchedEffect(state.aiWriteMessage) {
        val msg = state.aiWriteMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
        }
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
                Spacer(modifier = Modifier.fillMaxHeight(0.05f))

                //顶部返回+相册页标识
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

                // 选择栏：全部图 + 相机图 + 精修图 + 文案入口
                Row{
                    //展示albumphoto的全部图片
                    IconButton(
                        onClick = { viewModel.setPhotoTypeFilter(null) },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "全部",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    //展示albumphoto的用相机拍摄的照片（photo表中type字段为0)
                    IconButton(
                        onClick = { viewModel.setPhotoTypeFilter(0) },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "照片",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    //展示albumphoto的p过的照片（photo表中type字段为1)
                    IconButton(
                        onClick = { viewModel.setPhotoTypeFilter(1) },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "修图",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // 右侧入口：文案列表
                    if (onNavigateToCopywriting != null) {
                        IconButton(
                            onClick = onNavigateToCopywriting,
                            modifier = Modifier.size(40.dp),
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
                }



                when {
                    /*state.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }*/

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
                            items(displayPhotos, key = { it.id }) { photo ->
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
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.25f
                                                    )
                                                )
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

            if (state.isAiWriting) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.6f) // 半透明黑色背景
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Log.d("Animation","isAiWriting状态变化，但是动画未加载")
                        LoadingLottieOverlay(isLoading = state.isAiWriting)
                    }
                }
            }
            // 使用 AnimatedVisibility 确保动画显示不受 Dialog 关闭的影响
            AnimatedVisibility(
                visible = state.isAiWriting,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    //  Lottie 组件
                    LoadingLottieOverlay(isLoading = state.isAiWriting)
                }
            }


            if (showAiWriteDialog.value) {
                CopywriterRequirementSheet(
                    onDismiss = { showAiWriteDialog.value = false },
                    isProcessing = state.isAiWriting,
                    onConfirm = { requirement ->
                        showAiWriteDialog.value = false
                        //state.isAiWriting = true
                        viewModel.aiWriteForSelectedPhotos(requirement) { ok, id, result ->
                            if (ok && result != null) {
                                //  生成成功：存储文本，弹出结果抽屉
                                generatedText.value = result.toString()
                                showResultDialog.value = true
                                Log.d("AiWriteResult","原型$result,字符串后${generatedText.value}")
                                Log.d("isWriting","isAiWriting状态${state.isAiWriting}")
                                // 4. 可选：成功后清空选择
                                viewModel.clearSelection()
                            }
                        }
                    }
                )
            }
            // 结果卡片
            if (showResultDialog.value) {
                CopywriterResultSheet(
                    generatedText = generatedText.value,
                    onDismiss = { showResultDialog.value = false }
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

            // 右下角悬浮按钮（更显眼）：多图生成文案
            if (state.selectedPhotoIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    //全选
                    Button(
                        enabled = state.selectedPhotoIds.isNotEmpty() && !state.isAiWriting,
                        onClick = { viewModel.selectAll() },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text("全选")
                    }

                    // 清空选择
                    Button(
                        enabled = state.selectedPhotoIds.isNotEmpty() && !state.isAiWriting,
                        onClick = { viewModel.clearSelection() },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text("取消")
                    }

                    // 批量删除
                    Button(
                        enabled = state.selectedPhotoIds.isNotEmpty() && !state.isAiWriting,
                        onClick = { showDeleteDialog.value = true },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text("删除")
                    }

                    //批量生成文案
                    Button(
                        enabled = !state.isAiWriting,
                        onClick = { showAiWriteDialog.value = true },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text("文案")
                    }
                }
            }
        }
    }
}
