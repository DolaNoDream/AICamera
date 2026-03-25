package com.example.aicamera.ui.uistate.copywriting

import com.example.aicamera.data.db.entity.AlbumPhotoEntity

data class CopywritingDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val copywritingId: Long = 0L,
    val content: String = "",
    val createTime: Long = 0L,
    val updateTime: Long = 0L,
    val photos: List<AlbumPhotoEntity> = emptyList(),
    val isEditing: Boolean = false,
    val editContent: String = "",
    val isAddingPhotos: Boolean = false,
    val candidatePhotos: List<AlbumPhotoEntity> = emptyList(),
    val selectedAddPhotoIds: Set<Long> = emptySet(),
    val pendingRemovePhotoId: Long? = null
)
