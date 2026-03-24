package com.example.aicamera.ui.viewmodel.copywriting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicamera.app.di.ServiceLocator
import com.example.aicamera.data.repository.SeedResult
import com.example.aicamera.ui.uistate.copywriting.CopywritingListItem
import com.example.aicamera.ui.uistate.copywriting.CopywritingListUiState
import com.example.aicamera.ui.uistate.copywriting.CopywritingSort
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CopywritingListViewModel(application: Application) : AndroidViewModel(application) {

    private val copywritingRepository = ServiceLocator.provideCopywritingRepository(application)

    private val _uiState = MutableStateFlow(CopywritingListUiState())
    val uiState: StateFlow<CopywritingListUiState> = _uiState.asStateFlow()

    private var lastRawItems: List<CopywritingListItem> = emptyList()

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

    fun onContentQueryChange(newValue: String) {
        _uiState.value = _uiState.value.copy(contentQuery = newValue)
    }

    fun clearContentQuery() {
        _uiState.value = _uiState.value.copy(contentQuery = "")
    }

    fun toggleContentFilter(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enableContentFilter = enabled)
    }

    fun toggleCreateTimeFilter(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enableCreateTimeFilter = enabled)
    }

    fun toggleUpdateTimeFilter(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enableUpdateTimeFilter = enabled)
    }

    fun setCreateDateFrom(value: Long?) {
        _uiState.value = _uiState.value.copy(createDateFrom = value)
    }

    fun setCreateDateTo(value: Long?) {
        _uiState.value = _uiState.value.copy(createDateTo = value)
    }

    fun setUpdateDateFrom(value: Long?) {
        _uiState.value = _uiState.value.copy(updateDateFrom = value)
    }

    fun setUpdateDateTo(value: Long?) {
        _uiState.value = _uiState.value.copy(updateDateTo = value)
    }

    fun clearTimeFilters() {
        _uiState.value = _uiState.value.copy(
            createDateFrom = null,
            createDateTo = null,
            updateDateFrom = null,
            updateDateTo = null
        )
    }

    fun setSort(sort: CopywritingSort) {
        _uiState.value = _uiState.value.copy(sort = sort)
        // 排序属于展示逻辑，立即生效
        applySearch()
    }

    /**
     * 显式点击“搜索”按钮后才进行筛选（避免用户输入完不知道下一步）。
     */
    fun applySearch() {
        val cur = _uiState.value

        fun localDayStart(millis: Long): Long {
            val cal = Calendar.getInstance() // local timezone
            cal.timeInMillis = millis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun localDayEndExclusive(millis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            return cal.timeInMillis
        }

        fun inLocalDayRange(epochMillis: Long, from: Long?, to: Long?): Boolean {
            val fromLocal = from?.let { localDayStart(it) }
            val toEndExclusiveLocal = to?.let { localDayEndExclusive(it) }

            if (fromLocal != null && epochMillis < fromLocal) return false
            if (toEndExclusiveLocal != null && epochMillis >= toEndExclusiveLocal) return false
            return true
        }

        val filtered = lastRawItems.filter { item ->
            var ok = true

            if (cur.enableContentFilter) {
                val q = cur.contentQuery.trim()
                if (q.isNotBlank()) {
                    ok = true && item.content.contains(q, ignoreCase = true)
                }
            }

            if (cur.enableCreateTimeFilter) {
                ok = ok && inLocalDayRange(item.createTime, cur.createDateFrom, cur.createDateTo)
            }

            if (cur.enableUpdateTimeFilter) {
                ok = ok && inLocalDayRange(item.updateTime, cur.updateDateFrom, cur.updateDateTo)
            }

            ok
        }

        val sorted = when (cur.sort) {
            CopywritingSort.CreateTimeDesc -> filtered.sortedByDescending { it.createTime }
            CopywritingSort.UpdateTimeDesc -> filtered.sortedByDescending { it.updateTime }
        }

        val validIds = sorted.map { it.id }.toSet()
        val newSelected = cur.selectedIds.intersect(validIds)

        _uiState.value = cur.copy(
            items = sorted,
            selectedIds = newSelected,
            isSelectionMode = cur.isSelectionMode && newSelected.isNotEmpty()
        )
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
                    lastRawItems = list.map {
                        CopywritingListItem(
                            id = it.copywriting.id,
                            content = it.copywriting.content,
                            photoCount = it.photoCount,
                            createTime = it.copywriting.createTime,
                            updateTime = it.copywriting.updateTime
                        )
                    }

                    // 数据源变更时：保持现有筛选条件，重新应用一次
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                    applySearch()
                }
        }
    }
}