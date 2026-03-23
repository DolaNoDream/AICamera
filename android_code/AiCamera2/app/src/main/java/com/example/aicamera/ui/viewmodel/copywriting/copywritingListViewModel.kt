package com.example.aicamera.ui.viewmodel.copywriting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicamera.app.di.ServiceLocator
import com.example.aicamera.data.repository.SeedResult
import com.example.aicamera.ui.uistate.copywriting.CopywritingListItem
import com.example.aicamera.ui.uistate.copywriting.CopywritingListUiState
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
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        items = list.map {
                            CopywritingListItem(
                                id = it.copywriting.id,
                                content = it.copywriting.content,
                                photoCount = it.photoCount
                            )
                        }
                    )
                }
        }
    }
}