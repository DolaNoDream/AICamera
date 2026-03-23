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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CopywritingDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val copywritingRepository = ServiceLocator.provideCopywritingRepository(application)
    private val albumRepository = ServiceLocator.provideAlbumRepository(application)

    private val _uiState = MutableStateFlow(CopywritingDetailUiState())
    val uiState: StateFlow<CopywritingDetailUiState> = _uiState.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun load(copywritingId: Long) {
        if (copywritingId <= 0) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "无效的文案ID"
            )
            return
        }

        _deleted.value = false

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            copywritingId = copywritingId,
            isAddingPhotos = false,
            candidatePhotos = emptyList(),
            selectedAddPhotoIds = emptySet(),
            pendingRemovePhotoId = null
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
                        isLoading = false,
                        content = entity.content,
                        createTime = entity.createTime,
                        updateTime = entity.updateTime,
                        isEditing = false,
                        editContent = entity.content
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
                    // 如果当前在“添加照片”流程中，需要同步把已关联的照片从候选集合里排除
                    val cur = _uiState.value
                    val linkedIds = photos.map { it.id }.toSet()

                    _uiState.value = cur.copy(
                        isLoading = false,
                        photos = photos,
                        candidatePhotos = if (cur.isAddingPhotos) cur.candidatePhotos.filter { it.id !in linkedIds } else cur.candidatePhotos,
                        selectedAddPhotoIds = if (cur.isAddingPhotos) cur.selectedAddPhotoIds - linkedIds else cur.selectedAddPhotoIds
                    )
                }
        }
    }

    fun enterEdit() {
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            editContent = _uiState.value.content
        )
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(
            isEditing = false,
            editContent = _uiState.value.content
        )
    }

    fun onEditContentChange(newValue: String) {
        _uiState.value = _uiState.value.copy(editContent = newValue)
    }

    fun requestRemovePhoto(albumPhotoId: Long) {
        if (albumPhotoId <= 0) return
        _uiState.value = _uiState.value.copy(pendingRemovePhotoId = albumPhotoId)
    }

    fun cancelRemovePhoto() {
        _uiState.value = _uiState.value.copy(pendingRemovePhotoId = null)
    }

    fun confirmRemovePhoto() {
        val copywritingId = _uiState.value.copywritingId
        val albumPhotoId = _uiState.value.pendingRemovePhotoId
        if (copywritingId <= 0 || albumPhotoId == null || albumPhotoId <= 0) return

        viewModelScope.launch {
            runCatching {
                copywritingRepository.removePhotoFromCopywriting(copywritingId, albumPhotoId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "移除关联失败",
                    pendingRemovePhotoId = null
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(pendingRemovePhotoId = null)
            }
        }
    }

    fun saveEdit() {
        val id = _uiState.value.copywritingId
        val newContent = _uiState.value.editContent.trim()
        if (id <= 0) return
        if (newContent.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "文案内容不能为空")
            return
        }

        viewModelScope.launch {
            runCatching {
                copywritingRepository.updateCopywritingContent(id, newContent)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "修改失败"
                )
            }.onSuccess {
                // 更新 updateTime，用于页面展示
                _uiState.value = _uiState.value.copy(
                    content = newContent,
                    isEditing = false,
                    errorMessage = null,
                    updateTime = System.currentTimeMillis()
                )
            }
        }
    }

    fun startAddPhotos() {
        val id = _uiState.value.copywritingId
        if (id <= 0) return

        viewModelScope.launch {
            runCatching {
                // 候选照片 = 相册全部照片 - 已关联照片
                val all = albumRepository.observeAllPhotos().first()
                val linkedIds = _uiState.value.photos.map { it.id }.toSet()
                all.filter { it.id !in linkedIds }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "加载相册照片失败"
                )
            }.onSuccess { candidates ->
                _uiState.value = _uiState.value.copy(
                    isAddingPhotos = true,
                    candidatePhotos = candidates,
                    selectedAddPhotoIds = emptySet(),
                    errorMessage = null
                )
            }
        }
    }

    fun cancelAddPhotos() {
        _uiState.value = _uiState.value.copy(
            isAddingPhotos = false,
            candidatePhotos = emptyList(),
            selectedAddPhotoIds = emptySet()
        )
    }

    fun toggleSelectAddPhoto(photoId: Long) {
        if (photoId <= 0) return
        val cur = _uiState.value
        val newSet = cur.selectedAddPhotoIds.toMutableSet().apply {
            if (contains(photoId)) remove(photoId) else add(photoId)
        }
        _uiState.value = cur.copy(selectedAddPhotoIds = newSet)
    }

    fun confirmAddPhotos() {
        val id = _uiState.value.copywritingId
        val ids = _uiState.value.selectedAddPhotoIds.toList()
        if (id <= 0 || ids.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                copywritingRepository.addPhotosToCopywriting(id, ids)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "添加照片失败"
                )
            }.onSuccess {
                // 关联成功后，photos 会通过 Flow 自动刷新；这里只需退出添加模式
                cancelAddPhotos()
            }
        }
    }

    fun delete() {
        val id = _uiState.value.copywritingId
        if (id <= 0) return

        viewModelScope.launch {
            runCatching {
                copywritingRepository.deleteCopywritingById(id)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "删除失败"
                )
            }.onSuccess {
                _deleted.value = true
            }
        }
    }
}
