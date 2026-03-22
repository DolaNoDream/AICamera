package com.example.aicamera.ui.screen.album

import android.net.Uri
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.aicamera.ui.viewmodel.album.AlbumListViewModel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat

/**
 * 相册列表页：展示本地 Room(photo 表) 中记录的所有图片。
 *
 * 说明：目前只展示“本 App 保存后入库”的图片；若你希望展示系统相册全量，可后续做 MediaStore 同步。
 */
@Composable
fun AlbumListScreen(
    viewModel: AlbumListViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onPhotoClick: ((photoId: Long) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    // 状态栏占位（更接近系统相册的顶部留白）
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

            // 顶部栏：返回 + 标题
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

                Spacer(modifier = Modifier.width(48.dp))
            }

            // 内容区
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
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

                    state.photos.isEmpty() -> {
                        Text(
                            text = "暂无照片",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.photos, key = { it.id }) { item ->
                                val uri = runCatching { Uri.parse(item.filePath) }.getOrNull()

                                Card(
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clickable(
                                            enabled = onPhotoClick != null,
                                            indication = LocalIndication.current,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = { onPhotoClick?.invoke(item.id) }
                                        )
                                ) {
                                    if (uri != null) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Text(
                                                text = "无效路径",
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .padding(8.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium
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
