package com.example.aicamera.ui.screen.album

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.aicamera.data.network.aiPs.PictureRequirement
import com.example.aicamera.data.network.copywriter.CopywriterRequirement
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
    val reqFilter = remember { mutableStateOf("") }
    val reqPortrait = remember { mutableStateOf("") }
    val reqBackground = remember { mutableStateOf("") }
    val reqSpecial = remember { mutableStateOf("") }

    // AI 生成文案对话框状态
    val showAiWriteDialog = remember { mutableStateOf(false) }
    val writeType = remember { mutableStateOf("") }
    val writeEmotion = remember { mutableStateOf("") }
    val writeTheme = remember { mutableStateOf("") }
    val writeStyle = remember { mutableStateOf("") }
    val writeLength = remember { mutableStateOf("") }
    val writeSpecial = remember { mutableStateOf("") }
    val writeCustom = remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    if (showAiEditDialog.value) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isAiEditing) showAiEditDialog.value = false
            },
            title = { Text(text = "AI修图") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "请至少填写一项修图需求（不能为空）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = reqFilter.value,
                        onValueChange = { reqFilter.value = it },
                        singleLine = true,
                        label = { Text("滤镜") },
                        enabled = !state.isAiEditing
                    )
                    OutlinedTextField(
                        value = reqPortrait.value,
                        onValueChange = { reqPortrait.value = it },
                        singleLine = true,
                        label = { Text("人像") },
                        enabled = !state.isAiEditing
                    )
                    OutlinedTextField(
                        value = reqBackground.value,
                        onValueChange = { reqBackground.value = it },
                        singleLine = true,
                        label = { Text("背景") },
                        enabled = !state.isAiEditing
                    )
                    OutlinedTextField(
                        value = reqSpecial.value,
                        onValueChange = { reqSpecial.value = it },
                        singleLine = true,
                        label = { Text("特殊要求") },
                        enabled = !state.isAiEditing
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !state.isAiEditing,
                    onClick = {
                        val requirement = PictureRequirement().apply {
                            filter = reqFilter.value.trim().ifBlank { null }
                            portrait = reqPortrait.value.trim().ifBlank { null }
                            background = reqBackground.value.trim().ifBlank { null }
                            special = reqSpecial.value.trim().ifBlank { null }
                        }

                        val hasReq = !requirement.filter.isNullOrBlank() ||
                            !requirement.portrait.isNullOrBlank() ||
                            !requirement.background.isNullOrBlank() ||
                            !requirement.special.isNullOrBlank()

                        if (!hasReq) {
                            scope.launch { snackbarHostState.showSnackbar("requirement 不能为空（至少填写一项）") }
                            return@Button
                        }

                        viewModel.aiEditCurrentPhoto(requirement) { ok ->
                            if (ok) {
                                showAiEditDialog.value = false
                                // 清空输入
                                reqFilter.value = ""
                                reqPortrait.value = ""
                                reqBackground.value = ""
                                reqSpecial.value = ""
                            }
                        }
                    }
                ) {
                    Text(text = if (state.isAiEditing) "处理中..." else "开始修图")
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !state.isAiEditing,
                    onClick = { showAiEditDialog.value = false }
                ) {
                    Text(text = "取消")
                }
            }
        )
    }

    if (showAiWriteDialog.value) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isAiWriting) showAiWriteDialog.value = false
            },
            title = { Text(text = "AI生成文案") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "可选：填写成文需求（不填则按默认生成）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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

                        // 所有字段都为空时，传 null，让后端走默认逻辑
                        val effectiveReq = if (
                            requirement.type.isNullOrBlank() &&
                            requirement.emotion.isNullOrBlank() &&
                            requirement.theme.isNullOrBlank() &&
                            requirement.style.isNullOrBlank() &&
                            requirement.length.isNullOrBlank() &&
                            requirement.special.isNullOrBlank() &&
                            requirement.custom.isNullOrBlank()
                        ) {
                            null
                        } else {
                            requirement
                        }

                        viewModel.aiWriteCurrentPhoto(effectiveReq) { ok ->
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

                Text(
                    text = "照片详情",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = { showAiWriteDialog.value = true },
                    enabled = state.photo != null && !state.isLoading && !state.isAiWriting && !state.isAiEditing,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        text = "生成文案",
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
            }
        }
    }
}
