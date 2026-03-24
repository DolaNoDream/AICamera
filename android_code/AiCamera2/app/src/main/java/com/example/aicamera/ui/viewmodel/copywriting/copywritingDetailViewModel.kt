package com.example.aicamera.ui.viewmodel.copywriting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicamera.app.di.ServiceLocator
import com.example.aicamera.ui.uistate.copywriting.CopywritingDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CopywritingDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val copywritingRepository = ServiceLocator.provideCopywritingRepository(application)

    private val _uiState = MutableStateFlow(CopywritingDetailUiState())
    val uiState: StateFlow<CopywritingDetailUiState> = _uiState.asStateFlow()

    fun load(copywritingId: Long) {
        if (copywritingId <= 0) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "无效的文案ID"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            copywritingId = copywritingId
        )

        viewModelScope.launch {
            runCatching {
                copywritingRepository.getCopywritingById(copywritingId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "加载文案失败"
                )
            }.onSuccess { entity ->
                if (entity == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "文案不存在"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        content = entity.content
                    )
                }
            }
        }

        viewModelScope.launch {
            copywritingRepository.observePhotosForCopywriting(copywritingId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "加载关联照片失败"
                    )
                }
                .collect { photos ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        photos = photos
                    )
                }
        }
    }
}