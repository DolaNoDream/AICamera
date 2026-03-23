package com.example.aicamera.ui.viewmodel.copywriting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicamera.app.di.ServiceLocator
import com.example.aicamera.data.repository.SeedResult
import com.example.aicamera.ui.uistate.copywriting.CopywritingListItem
import com.example.aicamera.ui.uistate.copywriting.CopywritingListUiState
import com.example.aicamera.ui.uistate.copywriting.CopywritingSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CopywritingListViewModel(application: Application) : AndroidViewModel(application) {

    private val copywritingRepository = ServiceLocator.provideCopywritingRepository(application)

    private val _uiState = MutableStateFlow(CopywritingListUiState())
    val uiState: StateFlow<CopywritingListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 尝试 seed 3 条模拟数据
            when (copywritingRepository.seedMockDataIfNeeded()) {
                SeedResult.SEEDED,
                SeedResult.SKIPPED_ALREADY_HAS_DATA -> {
                    // ok
                }

                SeedResult.SKIPPED_NO_PHOTOS -> {
                    // 页面仍然可展示，只是提醒用户先拍照/入库
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "当前没有照片，请先拍照或入库照片后再查看文案列表"
                    )
                }
            }
        }

        observeList()
    }

    fun enterSelectionMode() {
        _uiState.value = _uiState.value.copy(isSelectionMode = true)
    }

    fun exitSelectionMode() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedIds = emptySet()
        )
    }

    fun toggleSelect(id: Long) {
        if (id <= 0) return
        val cur = _uiState.value
        val newSet = cur.selectedIds.toMutableSet().apply {
            if (contains(id)) remove(id) else add(id)
        }
        _uiState.value = cur.copy(
            isSelectionMode = true,
            selectedIds = newSet
        )
    }

    fun selectAll() {
        val allIds = _uiState.value.items.map { it.id }.filter { it > 0 }.toSet()
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedIds = allIds
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                copywritingRepository.deleteCopywritingsByIds(ids)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "删除失败"
                )
            }.onSuccess {
                // list 会通过 Flow 自动刷新；这里退出选择模式即可
                exitSelectionMode()
            }
        }
    }

    fun createNewCopywriting(onCreated: (copywritingId: Long) -> Unit) {
        viewModelScope.launch {
            runCatching {
                // 默认给一个占位，用户可在详情页编辑
                copywritingRepository.createCopywriting(content = "新文案")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "新建失败"
                )
            }.onSuccess { id ->
                if (id > 0) onCreated(id)
            }
        }
    }

    fun onQueryChange(newValue: String) {
        _uiState.value = _uiState.value.copy(query = newValue)
    }

    fun setSort(sort: CopywritingSort) {
        _uiState.value = _uiState.value.copy(sort = sort)
    }

    private fun observeList() {
        viewModelScope.launch {
            copywritingRepository.observeCopywritingsWithPhotoCount()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "加载文案失败"
                    )
                }
                .collect { list ->
                    val rawItems = list.map {
                        CopywritingListItem(
                            id = it.copywriting.id,
                            content = it.copywriting.content,
                            photoCount = it.photoCount,
                            createTime = it.copywriting.createTime,
                            updateTime = it.copywriting.updateTime
                        )
                    }

                    val cur = _uiState.value

                    val query = cur.query.trim()
                    val filtered = if (query.isBlank()) {
                        rawItems
                    } else {
                        // 支持：按文案内容 / 创建时间 / 更新时间（都用字符串 contains）
                        rawItems.filter { item ->
                            val ct = if (item.createTime > 0) item.createTime.toString() else ""
                            val ut = if (item.updateTime > 0) item.updateTime.toString() else ""
                            item.content.contains(query, ignoreCase = true) ||
                                ct.contains(query) ||
                                ut.contains(query)
                        }
                    }

                    val sorted = when (cur.sort) {
                        CopywritingSort.CreateTimeDesc -> filtered.sortedByDescending { it.createTime }
                        CopywritingSort.UpdateTimeDesc -> filtered.sortedByDescending { it.updateTime }
                    }

                    // 当数据源变化时，移除已不存在的选中项，避免“幽灵选中”
                    val validIds = sorted.map { it.id }.toSet()
                    val newSelected = cur.selectedIds.intersect(validIds)

                    _uiState.value = cur.copy(
                        isLoading = false,
                        errorMessage = null,
                        items = sorted,
                        selectedIds = newSelected,
                        isSelectionMode = cur.isSelectionMode && newSelected.isNotEmpty()
                    )
                }
        }
    }
}