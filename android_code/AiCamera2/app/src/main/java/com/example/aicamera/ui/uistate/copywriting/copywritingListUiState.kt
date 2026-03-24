package com.example.aicamera.ui.uistate.copywriting

data class CopywritingListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val items: List<CopywritingListItem> = emptyList()
)

data class CopywritingListItem(
    val id: Long,
    val content: String,
    val photoCount: Int
)
