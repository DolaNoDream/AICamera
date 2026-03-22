package com.example.aicamera.ui.screen.album

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.aicamera.ui.viewmodel.album.PhotoDetailViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults

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

    LaunchedEffect(photoId) {
        viewModel.load(photoId)
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
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏：返回 + 标题 + 删除
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
                    val uri = runCatching { Uri.parse(state.photo!!.filePath) }.getOrNull()

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
