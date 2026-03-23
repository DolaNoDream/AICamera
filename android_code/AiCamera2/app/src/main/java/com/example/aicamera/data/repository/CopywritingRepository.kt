package com.example.aicamera.data.repository

import com.example.aicamera.data.db.entity.AlbumPhotoEntity
import com.example.aicamera.data.db.entity.CopywritingEntity
import kotlinx.coroutines.flow.Flow

interface CopywritingRepository {

    /** 监听全部文案（按创建时间倒序） */
    fun observeAllCopywritings(): Flow<List<CopywritingEntity>>

    /** 文案 + 关联的照片数量（用于列表展示） */
    fun observeCopywritingsWithPhotoCount(): Flow<List<CopywritingWithCount>>

    /** 获取文案详情 */
    suspend fun getCopywritingById(id: Long): CopywritingEntity?

    /** 监听某条文案关联的照片列表 */
    fun observePhotosForCopywriting(copywritingId: Long): Flow<List<AlbumPhotoEntity>>

    /**
     * 插入 3 条模拟数据（若已插入过则不会重复插入）。
     *
     * 注意：会从 photo 表中读取真实的 id 作为关联。
     */
    suspend fun seedMockDataIfNeeded(): SeedResult

    /**
     * 新建一条文案并关联到某张照片。
     * @return copywritingId
     */
    suspend fun createCopywritingForPhoto(albumPhotoId: Long, content: String): Long

    /**
     * 新建一条文案并关联到多张照片（用于相册多选生成文案）。
     * @return copywritingId
     */
    suspend fun createCopywritingForPhotos(albumPhotoIds: List<Long>, content: String): Long
}

data class CopywritingWithCount(
    val copywriting: CopywritingEntity,
    val photoCount: Int
)

enum class SeedResult {
    SEEDED,
    SKIPPED_NO_PHOTOS,
    SKIPPED_ALREADY_HAS_DATA
}
