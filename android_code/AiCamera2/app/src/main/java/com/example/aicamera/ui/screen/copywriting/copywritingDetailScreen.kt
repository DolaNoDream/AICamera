package com.example.aicamera.ui.screen.copywriting

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.aicamera.ui.viewmodel.copywriting.CopywritingDetailViewModel

@Composable
fun CopywritingDetailScreen(
    viewModel: CopywritingDetailViewModel,
    copywritingId: Long,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onPhotoClick: ((photoId: Long) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(copywritingId) {
        viewModel.load(copywritingId)
    }

    val view = LocalView.current
    val density = LocalDensity.current
    val statusBarTopPaddingDp = remember(view, density) {
        val insets = ViewCompat.getRootWindowInsets(view)
        val px = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        with(density) { px.toDp() }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
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
                    text = "文案详情",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(48.dp))
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

                    else -> {
                        // 整页可滚动：长文案 + 大量照片都能上下滑动查看
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
                                            text = "关联照片：${state.photos.size} 张",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = state.content,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            // 不截断，允许长文案完整显示并随页面滚动
                                            overflow = TextOverflow.Clip
                                        )
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
                                        text = "关联照片",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                // 在 LazyColumn 里用“三列分组 Row”来渲染网格，避免嵌套 LazyVerticalGrid 造成滚动冲突/测量异常
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
                                                    .clickable(
                                                        enabled = onPhotoClick != null,
                                                        indication = LocalIndication.current,
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        onClick = { onPhotoClick?.invoke(photo.id) }
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

                                        // 不足 3 张时补齐占位，保持网格对齐
                                        repeat(3 - rowPhotos.size) {
                                            Spacer(modifier = Modifier.weight(1f))
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
