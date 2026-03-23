package com.example.aicamera.data.repository

import androidx.room.withTransaction
import com.example.aicamera.data.db.AiCameraDatabase
import com.example.aicamera.data.db.dao.AlbumPhotoDao
import com.example.aicamera.data.db.dao.CopywritingAlbumPhotoRelationDao
import com.example.aicamera.data.db.dao.CopywritingDao
import com.example.aicamera.data.db.entity.CopywritingAlbumPhotoRelationEntity
import com.example.aicamera.data.db.entity.CopywritingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class CopywritingRepositoryImpl(
    private val database: AiCameraDatabase,
    private val albumPhotoDao: AlbumPhotoDao,
    private val copywritingDao: CopywritingDao,
    private val relationDao: CopywritingAlbumPhotoRelationDao
) : CopywritingRepository {

    override fun observeAllCopywritings(): Flow<List<CopywritingEntity>> {
        return copywritingDao.observeAll()
    }

    override fun observeCopywritingsWithPhotoCount(): Flow<List<CopywritingWithCount>> {
        // 简单做法：把所有 copywriting 和 relation 表合并，按 copywriting_id 计数。
        return copywritingDao.observeAll().combine(
            relationDao.observeAllRelations()
        ) { copywritings, relations ->
            val countMap = relations.groupingBy { it.copywritingId }.eachCount()
            copywritings.map { c ->
                CopywritingWithCount(
                    copywriting = c,
                    photoCount = countMap[c.id] ?: 0
                )
            }
        }
    }

    override suspend fun seedMockDataIfNeeded(): SeedResult {
        // 已有文案则不再重复 seed
        val existing = copywritingDao.observeAll().first()
        if (existing.isNotEmpty()) return SeedResult.SKIPPED_ALREADY_HAS_DATA

        val photos = albumPhotoDao.observeAll().first()
        if (photos.isEmpty()) return SeedResult.SKIPPED_NO_PHOTOS

        val photoIds = photos.take(3).map { it.id }

        database.withTransaction {
            val now = System.currentTimeMillis()

            val c1Id = copywritingDao.insert(
                CopywritingEntity(
                    content = "【模拟文案 1】今天的光线刚刚好，随手一拍就很出片。",
                    createTime = now,
                    updateTime = now
                )
            )
            val c2Id = copywritingDao.insert(
                CopywritingEntity(
                    content = "【模拟文案 2】把快乐装进相册里，回头看都是惊喜。",
                    createTime = now + 1000,
                    updateTime = now + 1000
                )
            )
            val c3Id = copywritingDao.insert(
                CopywritingEntity(
                    content = "【模拟文案 3】风景在路上，你在我镜头里。",
                    createTime = now + 2000,
                    updateTime = now + 2000
                )
            )

            // 建立关联：确保 albumphoto_id 对应真实 photo.id
            val relations = buildList {
                if (photoIds.isNotEmpty()) {
                    add(CopywritingAlbumPhotoRelationEntity(copywritingId = c1Id, albumPhotoId = photoIds[0]))
                }
                if (photoIds.size >= 2) {
                    add(CopywritingAlbumPhotoRelationEntity(copywritingId = c2Id, albumPhotoId = photoIds[1]))
                }
                if (photoIds.size >= 3) {
                    add(CopywritingAlbumPhotoRelationEntity(copywritingId = c3Id, albumPhotoId = photoIds[2]))
                } else if (photoIds.size == 2) {
                    add(CopywritingAlbumPhotoRelationEntity(copywritingId = c3Id, albumPhotoId = photoIds[0]))
                } else {
                    add(CopywritingAlbumPhotoRelationEntity(copywritingId = c2Id, albumPhotoId = photoIds[0]))
                    add(CopywritingAlbumPhotoRelationEntity(copywritingId = c3Id, albumPhotoId = photoIds[0]))
                }
            }
            relationDao.insertIgnoreAll(relations)
        }

        return SeedResult.SEEDED
    }

    override suspend fun getCopywritingById(id: Long): CopywritingEntity? {
        return copywritingDao.getById(id)
    }

    override fun observePhotosForCopywriting(copywritingId: Long): Flow<List<com.example.aicamera.data.db.entity.AlbumPhotoEntity>> {
        return relationDao.observePhotosForCopywriting(copywritingId)
    }

    override suspend fun createCopywritingForPhoto(albumPhotoId: Long, content: String): Long {
        require(albumPhotoId > 0) { "albumPhotoId must be > 0" }
        require(content.isNotBlank()) { "content must not be blank" }

        val now = System.currentTimeMillis()
        return database.withTransaction {
            val copywritingId = copywritingDao.insert(
                CopywritingEntity(
                    content = content,
                    createTime = now,
                    updateTime = now
                )
            )

            relationDao.insertIgnore(
                CopywritingAlbumPhotoRelationEntity(
                    copywritingId = copywritingId,
                    albumPhotoId = albumPhotoId,
                    createTime = now
                )
            )

            copywritingId
        }
    }
}
