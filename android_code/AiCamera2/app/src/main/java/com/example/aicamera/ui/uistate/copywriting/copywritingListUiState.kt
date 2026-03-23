package com.example.aicamera.ui.uistate.copywriting

data class CopywritingListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val items: List<CopywritingListItem> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),

    // 搜索：可多选组合
    val enableContentFilter: Boolean = true,
    val enableCreateTimeFilter: Boolean = false,
    val enableUpdateTimeFilter: Boolean = false,

    // 内容搜索（用户输入）
    val contentQuery: String = "",

    // 时间搜索（epoch millis，表示某天 00:00 的时间戳）
    val createDateFrom: Long? = null,
    val createDateTo: Long? = null,
    val updateDateFrom: Long? = null,
    val updateDateTo: Long? = null,

    // 排序
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
