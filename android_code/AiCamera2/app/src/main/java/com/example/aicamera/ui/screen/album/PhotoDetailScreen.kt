package com.example.aicamera.ui.screen.album

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.aicamera.ui.screen.animation.LoadingLottieOverlay
import com.example.aicamera.ui.viewmodel.album.PhotoDetailViewModel
import kotlinx.coroutines.launch

/**
 * 相片详情页。
 */
@Composable
fun PhotoDetailScreen(
    viewModel: PhotoDetailViewModel,
    photoId: Long,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val showDeleteDialog = remember { mutableStateOf(false) }

    LaunchedEffect(photoId){
        viewModel.load(photoId)
    }

    // AI 修图对话框状态
    val showAiEditDialog = remember { mutableStateOf(false) }
    val showEditResultDialog = remember { mutableStateOf(false) } // 结果弹窗
    val editedPhotoPath = remember { mutableStateOf<String?>(null) } // 存储生成图片的本地路径

    // AI 生成文案对话框状态
    val showAiWriteDialog = remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showResultDialog = remember { mutableStateOf(false) } // 结果抽屉
    val generatedText = remember { mutableStateOf("") } // 存储生成的文本

    LaunchedEffect(state.aiEditMessage) {
        val msg = state.aiEditMessage
        if (!msg.isNullOrBlank()) {
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    LaunchedEffect(state.aiWriteMessage) {
        val msg = state.aiWriteMessage
        if (!msg.isNullOrBlank()) {
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isLoading) showDeleteDialog.value = false
            },
            title = { Text(text = "确认删除？") },
            text = { Text(text = "删除后将从系统相册移除，且不可恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCurrentPhoto { ok ->
                            if (ok) {
                                showDeleteDialog.value = false
                                onBack?.invoke()
                            } else {
                                // 失败时留在当前页，错误文案由 state.errorMessage 展示
                                showDeleteDialog.value = false
                            }
                        }
                    },
                    enabled = !state.isLoading
                ) {
                    Text(text = "删除")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog.value = false },
                    enabled = !state.isLoading
                ) {
                    Text(text = "取消")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.fillMaxHeight(0.05f))
            /*
            // 顶部栏：返回 + 标题 + 删除 + AI修图 + 生成文案
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        enabled = !state.isLoading,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                /*
                Text(
                    text = "照片详情",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                */

                OutlinedButton(
                    onClick = { showAiWriteDialog.value = true },
                    enabled = state.photo != null && !state.isLoading && !state.isAiWriting && !state.isAiEditing,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        text = "AI文案",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { showAiEditDialog.value = true },
                    enabled = state.photo != null && !state.isLoading && !state.isAiEditing && !state.isAiWriting,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        text = "AI修图",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { showDeleteDialog.value = true },
                    enabled = state.photo != null && !state.isLoading,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        text = "删除",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }*/

            Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {

                // --- 1. 底层：可滚动的 LazyColumn ---
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    !state.errorMessage.isNullOrBlank() -> {
                        Text(
                            text = state.errorMessage ?: "加载失败",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                        )
                    }

                    state.photo != null -> {
                        val photo = state.photo!!
                        val uri = runCatching { photo.filePath.toUri() }.getOrNull()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            // 关键优化：给底部留出空间，防止内容被 Snackbar 遮挡
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            // Item 1: 图片区域。占满首屏。
                            item(key = "photo_image") {
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxHeight() // 关键：使图片项目占满父容器可用高度
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (uri != null) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().padding(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "无效图片路径",
                                            modifier = Modifier.align(Alignment.Center),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            // Item 2: 信息与文案区域。滑动后可见。
                            item(key = "photo_info") {
                                PhotoInfoSection(photo = photo)
                            }
                        }
                    }
                }
                PhotoDetailTopBar(
                    onBack = onBack,
                    isLoading = state.isLoading,
                    isBusy = state.isAiEditing || state.isAiWriting,
                    onWriteClick = { showAiWriteDialog.value = true },
                    onEditClick = { showAiEditDialog.value = true },
                    onDeleteClick = { showDeleteDialog.value = true },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            /*
            // 内容
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                !state.errorMessage.isNullOrBlank() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = state.errorMessage ?: "加载失败",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                state.photo != null -> {
                    val uri = runCatching { state.photo!!.filePath.toUri() }.getOrNull()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (uri != null) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        } else {
                            Text(
                                text = "无效图片路径",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    // 基础信息（后续可以改成更“系统相册”的样式）
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "filePath: ${state.photo!!.filePath}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "type: ${state.photo!!.type}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "text: ${state.photo!!.text ?: "无"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "size: ${state.photo!!.width ?: 0} x ${state.photo!!.height ?: 0}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }*/

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        enabled = !state.isLoading,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                OutlinedButton(
                    onClick = { showAiWriteDialog.value = true },
                    enabled = state.photo != null && !state.isLoading && !state.isAiWriting && !state.isAiEditing,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        text = "AI文案",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { showAiEditDialog.value = true },
                    enabled = state.photo != null && !state.isLoading && !state.isAiEditing && !state.isAiWriting,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        text = "AI修图",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { showDeleteDialog.value = true },
                    enabled = state.photo != null && !state.isLoading,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        text = "删除",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        //AI修图卡片
        if (showAiEditDialog.value) {
            AiEditRequirementSheet(
                onDismiss = {
                    // 如果不在处理中，允许关闭
                    if (!state.isAiEditing) showAiEditDialog.value = false
                },
                isProcessing = state.isAiEditing, // 由 ViewModel 的状态驱动锁定 UI
                onConfirm = { requirement ->
                    // 宿主关闭抽屉
                    showAiEditDialog.value = false

                    // 调用 viewModel 的修图请求
                    viewModel.aiEditCurrentPhoto(requirement) { ok, result->
                        if (ok) {
                            editedPhotoPath.value = result // 假设 result 包含 imageSavePath
                            showEditResultDialog.value = true
                            // 成功逻辑（例如：显示 Snackbar 提示，这部分你原有的代码已经处理了）
                        }
                    }
                }
            )
        }


        // AI修图结果展示 (AiEditResultDialog)
        if (showEditResultDialog.value) {
            AiEditResultDialog(
                editedPhotoPath = editedPhotoPath.value,
                onDismiss = { showEditResultDialog.value = false },
                //isSaving = state.isLoading
            )
        }

        // 修图加载动画
        AnimatedVisibility(
            visible = state.isAiEditing,
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
                LoadingLottieOverlay(isLoading = state.isAiEditing)
            }
        }

        // AI文案卡片
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
                    viewModel.aiWriteCurrentPhoto(requirement) { ok, result ->
                        if (ok && result != null) {
                            //  生成成功：存储文本，弹出结果抽屉
                            generatedText.value = result
                            showResultDialog.value = true
                            Log.d("AiWriteResult","原型$result,字符串后${generatedText.value}")
                            Log.d("isWriting","isAiWriting状态${state.isAiWriting}")
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
    }
}
