package com.example.aicamera.ui.screen.copywriting

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import com.example.aicamera.ui.viewmodel.copywriting.CopywritingDetailViewModel
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CopywritingDetailScreen(
    viewModel: CopywritingDetailViewModel,
    copywritingId: Long,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onPhotoClick: ((photoId: Long) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val deleted by viewModel.deleted.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(copywritingId) {
        viewModel.load(copywritingId)
    }

    LaunchedEffect(deleted) {
        if (deleted) {
            onBack?.invoke()
        }
    }

    val view = LocalView.current
    val density = LocalDensity.current
    val statusBarTopPaddingDp = remember(view, density) {
        val insets = ViewCompat.getRootWindowInsets(view)
        val px = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        with(density) { px.toDp() }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(text = "删除文案") },
            text = { Text(text = "确定删除这条文案吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete()
                    }
                ) {
                    Text(text = "删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = "取消")
                }
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        )
    }

    val dateFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }

    val pendingRemoveId = state.pendingRemovePhotoId
    if (pendingRemoveId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelRemovePhoto() },
            title = { Text(text = "移除关联照片") },
            text = { Text(text = "确定移除这张关联照片吗？不会删除照片本体。") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRemovePhoto() }) {
                    Text(text = "移除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelRemovePhoto() }) {
                    Text(text = "取消")
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
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
                        text = if (state.isAddingPhotos) "添加关联照片" else "文案详情",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // 右侧：编辑/保存/取消 + 删除（不再在这里放“新增关联照片”）
                    if (!state.isLoading && state.errorMessage.isNullOrBlank()) {
                        if (state.isAddingPhotos) {
                            IconButton(
                                onClick = { viewModel.confirmAddPhotos() },
                                enabled = state.selectedAddPhotoIds.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "确认添加",
                                    tint = if (state.selectedAddPhotoIds.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.cancelAddPhotos() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "取消添加",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            if (state.isEditing) {
                                IconButton(onClick = { viewModel.saveEdit() }) {
                                    Icon(
                                        imageVector = Icons.Filled.Save,
                                        contentDescription = "保存",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { viewModel.cancelEdit() }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "取消编辑",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                IconButton(onClick = { viewModel.enterEdit() }) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "修改",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    when {
                        state.isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        !state.errorMessage.isNullOrBlank() -> {
                            Text(
                                text = state.errorMessage ?: "加载失败",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        state.isAddingPhotos -> {
                            if (state.candidatePhotos.isEmpty()) {
                                Text(
                                    text = "没有可添加的照片（可能都已关联，或相册为空）",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
                                ) {
                                    item { Spacer(modifier = Modifier.height(6.dp)) }

                                    items(state.candidatePhotos, key = { it.id }) { photo ->
                                        val selected = state.selectedAddPhotoIds.contains(photo.id)
                                        val uri = runCatching { photo.filePath.toUri() }.getOrNull()

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(
                                                    indication = LocalIndication.current,
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    onClick = { viewModel.toggleSelectAddPhoto(photo.id) }
                                                ),
                                            shape = MaterialTheme.shapes.large,
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (uri != null) {
                                                    AsyncImage(
                                                        model = uri,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(64.dp)
                                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(64.dp)
                                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    ) {
                                                        Text(
                                                            text = "无效",
                                                            modifier = Modifier.align(Alignment.Center),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = photo.text?.takeIf { it.isNotBlank() } ?: "（无描述）",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = photo.filePath,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Icon(
                                                    imageVector = if (selected) Icons.Filled.Check else Icons.Filled.Close,
                                                    contentDescription = if (selected) "已选中" else "未选中",
                                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
                            ) {
                                item(key = "copywriting_card") {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.large,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "创建时间：" + (if (state.createTime > 0) dateFormatter.format(Date(state.createTime)) else "-") ,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "最新修改：" + (if (state.updateTime > 0) dateFormatter.format(Date(state.updateTime)) else "-") ,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "关联照片：${state.photos.size} 张",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))

                                            if (state.isEditing) {
                                                TextField(
                                                    value = state.editContent,
                                                    onValueChange = viewModel::onEditContentChange,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    placeholder = { Text(text = "请输入文案内容") },
                                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                    keyboardActions = KeyboardActions(onDone = { viewModel.saveEdit() })
                                                )
                                            } else {
                                                Text(
                                                    text = state.content,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    overflow = TextOverflow.Clip
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // 复制按钮：放在文案内容之后
                                            IconButton(
                                                onClick = {
                                                    val textToCopy = if (state.isEditing) state.editContent else state.content
                                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                                    scope.launch { snackbarHostState.showSnackbar("已复制") }
                                                },
                                                enabled = (if (state.isEditing) state.editContent else state.content).isNotBlank(),
                                                modifier = Modifier.wrapContentWidth(align = Alignment.End)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.ContentCopy,
                                                    contentDescription = "复制文案",
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                if (state.photos.isEmpty()) {
                                    item(key = "empty_photos") {
                                        Text(
                                            text = "暂无关联照片",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                } else {
                                    item(key = "photos_title") {
                                        Text(
                                            text = "关联照片（长按可移除）",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    val rows = state.photos.chunked(3)
                                    items(rows, key = { row -> row.joinToString("_") { it.id.toString() } }) { rowPhotos ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowPhotos.forEach { photo ->
                                                val uri = runCatching { photo.filePath.toUri() }.getOrNull()

                                                Card(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .combinedClickable(
                                                            enabled = true,
                                                            onClick = {
                                                                onPhotoClick?.invoke(photo.id)
                                                            },
                                                            onLongClick = {
                                                                viewModel.requestRemovePhoto(photo.id)
                                                            }
                                                        ),
                                                    shape = MaterialTheme.shapes.large,
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                                ) {
                                                    if (uri != null) {
                                                        AsyncImage(
                                                            model = uri,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(110.dp)
                                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(110.dp)
                                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        ) {
                                                            Text(
                                                                text = "无效路径",
                                                                modifier = Modifier
                                                                    .align(Alignment.Center)
                                                                    .padding(8.dp),
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            repeat(3 - rowPhotos.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }

                                // 新增关联照片按钮：放在所有关联图片的后面（无关联图片也一样会出现在底部）
                                item(key = "add_photos_entry") {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = { viewModel.startAddPhotos() },
                                                onLongClick = {}
                                            ),
                                        shape = MaterialTheme.shapes.large,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "新增关联照片",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
