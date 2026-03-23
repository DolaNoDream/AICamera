package com.example.aicamera.ui.uistate.album

import com.example.aicamera.data.db.entity.AlbumPhotoEntity

/**
 * 相册列表 UIState
 */
data class AlbumListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val photos: List<AlbumPhotoEntity> = emptyList(),

    /** 已选中的 photo.id 集合 */
    val selectedPhotoIds: Set<Long> = emptySet(),

    /** 生成文案中 */
    val isAiWriting: Boolean = false,

    /** 生成/保存后的提示 */
    val aiWriteMessage: String? = null,

    /** 最近一次生成并保存的文案ID（可用于跳转文案详情） */
    val lastGeneratedCopywritingId: Long? = null,
)
