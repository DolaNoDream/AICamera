package com.example.aicamera.ui.uistate.copywriting

import com.example.aicamera.data.db.entity.AlbumPhotoEntity

data class CopywritingDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val copywritingId: Long = 0L,
    val content: String = "",
    val photos: List<AlbumPhotoEntity> = emptyList()
)
