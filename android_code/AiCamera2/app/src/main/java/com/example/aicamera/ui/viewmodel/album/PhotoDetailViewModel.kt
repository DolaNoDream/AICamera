package com.example.aicamera.ui.viewmodel.album

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicamera.app.di.ServiceLocator
import com.example.aicamera.ui.uistate.album.PhotoDetailUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 相片详情 VM。
 */
class PhotoDetailViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PhotoDetailVM"
    }

    private val dao = ServiceLocator.provideDatabase(application).albumPhotoDao()

    private val _uiState = MutableStateFlow(PhotoDetailUiState())
    val uiState: StateFlow<PhotoDetailUiState> = _uiState.asStateFlow()

    /**
     * 加载相片详情。
     */
    fun load(photoId: Long) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val photo = dao.getById(photoId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    photo = photo,
                    errorMessage = if (photo == null) "未找到照片记录" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "加载失败"
                )
            }
        }
    }

    /**
     * 删除当前照片：
     * 1) 从 MediaStore 删除（使系统相册中也消失）
     * 2) 从 Room 删除记录
     *
     * @return true = 删除成功（或至少 MediaStore 删除成功），false = 删除失败
     */
    fun deleteCurrentPhoto(onResult: (Boolean) -> Unit) {
        val photo = _uiState.value.photo
        if (photo == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "无可删除的照片")
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val appContext = getApplication<Application>().applicationContext
            val resolver = appContext.contentResolver

            val uri = runCatching { Uri.parse(photo.filePath) }.getOrNull()
            if (uri == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "无效图片Uri")
                onResult(false)
                return@launch
            }

            try {
                // 删除 MediaStore
                val deletedRows = withContext(Dispatchers.IO) {
                    resolver.delete(uri, null, null)
                }

                if (deletedRows <= 0) {
                    Log.w(TAG, "MediaStore 删除返回 $deletedRows 行：$uri")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "删除失败：系统相册未删除（可能无权限或图片已不存在）"
                    )
                    onResult(false)
                    return@launch
                }

                // 删除 Room 记录（即使 DB 删除失败，图片也已从系统相册删掉）
                runCatching {
                    withContext(Dispatchers.IO) {
                        dao.deleteById(photo.id)
                    }
                }.onFailure { e ->
                    Log.w(TAG, "图片已从系统相册删除，但删除数据库记录失败：${e.message}")
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
                onResult(true)
            } catch (e: SecurityException) {
                // Android 11+ 对非本应用创建的媒体可能会抛 RecoverableSecurityException
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "删除被系统拒绝：${e.message}"
                )
                onResult(false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "删除失败：${e.message}"
                )
                onResult(false)
            }
        }
    }
}
