package com.example.aicamera.ui.uistate.album

import com.example.aicamera.data.db.entity.AlbumPhotoEntity

/**
 * 相册列表 UIState
 */
data class AlbumListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val photos: List<AlbumPhotoEntity> = emptyList()
)
