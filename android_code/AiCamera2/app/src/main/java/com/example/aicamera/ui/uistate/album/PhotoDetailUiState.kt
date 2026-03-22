package com.example.aicamera.ui.uistate.album

import com.example.aicamera.data.db.entity.AlbumPhotoEntity

/**
 * 相片详情 UIState
 */
data class PhotoDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val photo: AlbumPhotoEntity? = null
)
