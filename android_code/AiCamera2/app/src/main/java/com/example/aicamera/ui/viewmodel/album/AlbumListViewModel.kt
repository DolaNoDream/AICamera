package com.example.aicamera.ui.viewmodel.album

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicamera.app.di.ServiceLocator
import com.example.aicamera.data.network.copywriter.AiWriteManager
import com.example.aicamera.data.network.copywriter.CopywriterRequirement
import com.example.aicamera.ui.uistate.album.AlbumListUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 相册列表 VM。
 */
class AlbumListViewModel(application: Application) : AndroidViewModel(application) {

    private val albumRepository = ServiceLocator.provideAlbumRepository(application)
    private val copywritingRepository = ServiceLocator.provideCopywritingRepository(application)

    private val _uiState = MutableStateFlow(AlbumListUiState())
    val uiState: StateFlow<AlbumListUiState> = _uiState.asStateFlow()

    init {
        observePhotos()
    }

    private fun observePhotos() {
        viewModelScope.launch {
            albumRepository.observeAllPhotos()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "加载相册失败"
                    )
                }
                .collect { list ->
                    // 如果照片被删除，顺便清理 selection
                    val existingIds = list.map { it.id }.toSet()
                    val newSelected = _uiState.value.selectedPhotoIds.intersect(existingIds)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        photos = list,
                        selectedPhotoIds = newSelected
                    )
                }
        }
    }

    fun toggleSelect(photoId: Long) {
        val current = _uiState.value.selectedPhotoIds
        _uiState.value = _uiState.value.copy(
            selectedPhotoIds = if (current.contains(photoId)) current - photoId else current + photoId,
            aiWriteMessage = null,
            lastGeneratedCopywritingId = null
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedPhotoIds = emptySet(),
            aiWriteMessage = null,
            lastGeneratedCopywritingId = null
        )
    }

    private fun setAiWriteStatus(isWriting: Boolean, message: String? = null, copywritingId: Long? = null) {
        _uiState.value = _uiState.value.copy(
            isAiWriting = isWriting,
            aiWriteMessage = message,
            lastGeneratedCopywritingId = copywritingId
        )
    }

    /**
     * 以「已选照片 + requirement」生成文案：
     * - 自动生成 sessionId
     * - 调用后端 /ai/write
     * - 新文案写入 copywriting 表
     * - 写入 copywriting_albumphoto_relation（多张）
     * - 同步把文案写入选中照片的 photo.text（便于在照片详情直接看到）
     */
    fun aiWriteForSelectedPhotos(requirement: CopywriterRequirement?, onDone: (Boolean, Long?) -> Unit) {
        val ids = _uiState.value.selectedPhotoIds.toList()
        if (ids.isEmpty()) {
            setAiWriteStatus(isWriting = false, message = "请先选择照片")
            onDone(false, null)
            return
        }

        val photos = _uiState.value.photos.filter { ids.contains(it.id) }
        val uris = photos.mapNotNull { p ->
            runCatching { p.filePath.toUri() }.getOrNull()
        }

        if (uris.isEmpty()) {
            setAiWriteStatus(isWriting = false, message = "所选照片路径无效")
            onDone(false, null)
            return
        }

        viewModelScope.launch {
            setAiWriteStatus(isWriting = true, message = null, copywritingId = null)

            val sessionId = UUID.randomUUID().toString()
            val appContext = getApplication<Application>().applicationContext
            val manager = AiWriteManager.getInstance(appContext)

            manager.writeWithUris(
                sessionId = sessionId,
                imageUris = uris,
                requirement = requirement,
                callback = object : AiWriteManager.AiWriteCallback {
                    override fun onResult(result: com.example.aicamera.data.network.copywriter.AiWriteResult) {
                        viewModelScope.launch {
                            if (!result.success || result.content.isNullOrBlank()) {
                                setAiWriteStatus(isWriting = false, message = result.errorMessage ?: "文案生成失败")
                                onDone(false, null)
                                return@launch
                            }

                            val content = result.content

                            val copywritingId = withContext(Dispatchers.IO) {
                                // 1) 写入 copywriting + relation（必须成功）
                                val id = copywritingRepository.createCopywritingForPhotos(ids, content)

                                // 2) 同步写入 photo.text（失败不影响文案列表/详情）
                                runCatching {
                                    albumRepository.updateTextByIds(ids, content)
                                }

                                id
                            }

                            setAiWriteStatus(isWriting = false, message = "文案已生成并保存", copywritingId = copywritingId)
                            onDone(true, copywritingId)
                        }
                    }
                }
            )
        }
    }

    /**
     * 批量删除当前选中的照片。
     * 说明：copywriting_albumphoto_relation 表对 photo(id) 有外键 CASCADE，删除 photo 行会自动清理关联记录。
     */
    fun deleteSelectedPhotos(onDone: (Boolean, Int) -> Unit) {
        val ids = _uiState.value.selectedPhotoIds.toList().distinct().filter { it > 0 }
        if (ids.isEmpty()) {
            setAiWriteStatus(isWriting = false, message = "请先选择要删除的照片")
            onDone(false, 0)
            return
        }

        viewModelScope.launch {
            // 删除期间禁用按钮（复用 isAiWriting 作为 busy 标记，避免额外字段）
            setAiWriteStatus(isWriting = true, message = null, copywritingId = null)

            val deleted = withContext(Dispatchers.IO) {
                albumRepository.deletePhotosByIds(ids)
            }

            // 清空选择（observePhotos 也会基于 existingIds 自动修正，这里主动清掉体验更好）
            _uiState.update {
                it.copy(
                    selectedPhotoIds = emptySet(),
                    isAiWriting = false,
                    aiWriteMessage = if (deleted > 0) "已删除 $deleted 张照片" else "删除失败",
                    lastGeneratedCopywritingId = null
                )
            }

            onDone(deleted > 0, deleted)
        }
    }
}
