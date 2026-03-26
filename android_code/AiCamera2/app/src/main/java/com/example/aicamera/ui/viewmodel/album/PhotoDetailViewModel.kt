package com.example.aicamera.ui.viewmodel.album

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicamera.app.di.ServiceLocator
import com.example.aicamera.data.db.entity.AlbumPhotoEntity
import com.example.aicamera.data.network.aiPs.AiEditManager
import com.example.aicamera.data.network.aiPs.PictureRequirement
import com.example.aicamera.data.network.copywriter.AiWriteManager
import com.example.aicamera.data.network.copywriter.CopywriterRequirement
import com.example.aicamera.data.storage.FileManager
import com.example.aicamera.ui.uistate.album.PhotoDetailUiState
import java.io.File
import java.util.UUID
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
    private val albumRepository = ServiceLocator.provideAlbumRepository(application)
    private val fileManager = FileManager(application.applicationContext)
    private val copywritingRepository = ServiceLocator.provideCopywritingRepository(application)

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

            val uri = runCatching { photo.filePath.toUri() }.getOrNull()
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

    /** AI 修图：供 UI 调用的状态 */
    fun setAiEditStatus(isEditing: Boolean, message: String? = null) {
        _uiState.value = _uiState.value.copy(
            isAiEditing = isEditing,
            aiEditMessage = message
        )
    }

    /**
     * 调用 AI 修图接口：
     * - sessionId 自动生成
     * - imageFile 来自当前 photo（支持 content:// Uri 或绝对路径）
     * - requirement 必须至少填一项
     *
     * 成功后：
     * 1) 将 AiEditManager 下载到私有目录的图片导入系统相册（Pictures/AiCamera）
     * 2) 写入 Room（type=1 表示 P 图）
     */
    fun aiEditCurrentPhoto(requirement: PictureRequirement, onDone: (Boolean, String?) -> Unit) {
        val photo = _uiState.value.photo
        if (photo == null) {
            _uiState.value = _uiState.value.copy(aiEditMessage = "无可修图的照片")
            onDone(false, null)
            return
        }

        val hasReq = !requirement.filter.isNullOrBlank() ||
            !requirement.portrait.isNullOrBlank() ||
            !requirement.background.isNullOrBlank() ||
            !requirement.special.isNullOrBlank()

        if (!hasReq) {
            _uiState.value = _uiState.value.copy(aiEditMessage = "修图需求不能为空（至少填写一项）")
            onDone(false, null)
            return
        }

        viewModelScope.launch {
            setAiEditStatus(isEditing = true, message = null)

            val appContext = getApplication<Application>().applicationContext
            val sourceFile = withContext(Dispatchers.IO) {
                resolvePhotoToLocalFile(appContext, photo.filePath)
            }

            if (sourceFile == null || !sourceFile.exists()) {
                setAiEditStatus(isEditing = false, message = "无法读取原图文件")
                onDone(false, null)
                return@launch
            }

            val sessionId = UUID.randomUUID().toString()
            val manager = AiEditManager.getInstance(appContext)

            // AiEditManager 内部是 OkHttp 异步回调，这里用回调切回协程更新状态
            manager.editImage(sessionId, sourceFile, requirement) { result ->
                viewModelScope.launch {
                    if (result == null || !result.isSuccess()) {
                        setAiEditStatus(isEditing = false, message = result?.getErrorMsg() ?: "修图失败")
                        onDone(false, null)
                        return@launch
                    }

                    val savePath = result.getImageSavePath()
                    if (savePath.isNullOrBlank()) {
                        setAiEditStatus(isEditing = false, message = "修图成功但返回路径为空")
                        onDone(false, null)
                        return@launch
                    }

                    val editedFile = File(savePath)
                    val galleryUri = fileManager.importImageFileToGallery(editedFile)
                    if (galleryUri.isNullOrBlank()) {
                        setAiEditStatus(isEditing = false, message = "修图成功，但保存到系统相册失败")
                        onDone(false, null)
                        return@launch
                    }

                    // 入库：type = 1（P图）
                    runCatching {
                        albumRepository.insertPhoto(
                            AlbumPhotoEntity(
                                filePath = galleryUri,
                                type = 1,
                                text = null,
                                createTime = System.currentTimeMillis(),
                                width = photo.width,
                                height = photo.height,
                                fileSize = editedFile.length()
                            )
                        )
                    }.onFailure { e ->
                        Log.w(TAG, "修图结果已保存到系统相册，但写入数据库失败：${e.message}")
                    }

                    setAiEditStatus(isEditing = false, message = "AI修图成功")
                    onDone(true, savePath)
                }
            }
        }
    }

    /** AI 生成文案：供 UI 调用的状态 */
    fun setAiWriteStatus(isWriting: Boolean, message: String? = null) {
        _uiState.value = _uiState.value.copy(
            isAiWriting = isWriting,
            aiWriteMessage = message
        )
    }

    /**
     * 对当前照片生成文案，并将返回结果写入该照片的 text 字段。
     */
    fun aiWriteCurrentPhoto(requirement: CopywriterRequirement?, onDone: (Boolean, String?) -> Unit) {
        val photo = _uiState.value.photo
        if (photo == null) {
            setAiWriteStatus(isWriting = false, message = "无可生成文案的照片")
            onDone(false, null)
            return
        }

        viewModelScope.launch {
            setAiWriteStatus(isWriting = true, message = null)

            val appContext = getApplication<Application>().applicationContext
            val uri = runCatching { photo.filePath.toUri() }.getOrNull()

            if (uri == null) {
                setAiWriteStatus(isWriting = false, message = "无效图片Uri")
                onDone(false, null)
                return@launch
            }

            val sessionId = UUID.randomUUID().toString()
            val manager = AiWriteManager.getInstance(appContext)

            // AiWriteManager 内部是 OkHttp 异步回调，这里用回调切回协程更新状态
            manager.writeWithUris(
                sessionId = sessionId,
                imageUris = listOf(uri),
                requirement = requirement,
                callback = object : AiWriteManager.AiWriteCallback {
                    override fun onResult(result: com.example.aicamera.data.network.copywriter.AiWriteResult) {
                        viewModelScope.launch {
                            if (!result.success || result.content.isNullOrBlank()) {
                                setAiWriteStatus(
                                    isWriting = false,
                                    message = result.errorMessage ?: "文案生成失败"
                                )
                                onDone(false, null)
                                return@launch
                            }

                            val content = result.content

                            // 1) 写入 photo.text
                            val updated = runCatching {
                                withContext(Dispatchers.IO) {
                                    albumRepository.updateTextById(photo.id, content)
                                }
                            }.getOrElse { e ->
                                Log.w(TAG, "写入文案到 photo 表失败：${e.message}")
                                0
                            }

                            if (updated <= 0) {
                                setAiWriteStatus(isWriting = false, message = "文案生成成功，但写入照片失败")
                                onDone(false, null)
                                return@launch
                            }

                            // 2) 写入 copywriting + relation（失败不影响 photo.text）
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    copywritingRepository.createCopywritingForPhoto(
                                        albumPhotoId = photo.id,
                                        content = content
                                    )
                                }
                            }.onFailure { e ->
                                Log.w(TAG, "文案已写入 photo.text，但写入 copywriting/relation 失败：${e.message}")
                            }

                            // 刷新当前 photo（让 UI 立即展示最新 text）
                            val refreshed = withContext(Dispatchers.IO) { dao.getById(photo.id) }
                            _uiState.value = _uiState.value.copy(photo = refreshed)

                            setAiWriteStatus(isWriting = false, message = "文案已保存")
                            onDone(true, content)
                        }
                    }
                }
            )
        }
    }

    /**
     * 将 photo.filePath 解析为可读取的本地文件。
     * 兼容：
     * - content://...（从 ContentResolver 读出并复制到 cache）
     * - /storage/... 或 app 私有目录绝对路径
     */
    private suspend fun resolvePhotoToLocalFile(context: android.content.Context, filePath: String): File? {
        return withContext(Dispatchers.IO) {
            runCatching {
                if (filePath.startsWith("content://")) {
                    val uri = filePath.toUri()
                    val cacheFile = File(context.cacheDir, "ai_edit_input_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: return@runCatching null
                    cacheFile
                } else {
                    File(filePath)
                }
            }.getOrNull()
        }
    }
}
