package com.example.aicamera.ui.uistate.copywriting

data class CopywritingListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val items: List<CopywritingListItem> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val query: String = "",
    val sort: CopywritingSort = CopywritingSort.CreateTimeDesc
) {
    val selectedCount: Int get() = selectedIds.size
}

enum class CopywritingSort {
    CreateTimeDesc,
    UpdateTimeDesc
}

data class CopywritingListItem(
    val id: Long,
    val content: String,
    val photoCount: Int,
    val createTime: Long,
    val updateTime: Long
)
