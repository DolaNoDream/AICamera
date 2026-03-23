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

    /** 新建一条“纯文案”（不关联照片） */
    suspend fun createCopywriting(content: String): Long

    /** 为现有文案追加关联照片（重复关联会被忽略） */
    suspend fun addPhotosToCopywriting(copywritingId: Long, albumPhotoIds: List<Long>): Int

    /** 删除一条文案（会同时清理关联关系，取决于外键/级联设置） */
    suspend fun deleteCopywritingById(copywritingId: Long): Int

    /** 批量删除文案 */
    suspend fun deleteCopywritingsByIds(copywritingIds: List<Long>): Int

    /** 修改文案内容 */
    suspend fun updateCopywritingContent(copywritingId: Long, newContent: String): Int

    /** 移除某条文案的一张关联照片（仅删除关联关系，不删除 photo 本体） */
    suspend fun removePhotoFromCopywriting(copywritingId: Long, albumPhotoId: Long): Int
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
