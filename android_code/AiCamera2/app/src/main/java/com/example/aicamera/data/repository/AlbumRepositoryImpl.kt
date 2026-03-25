package com.example.aicamera.data.repository

import com.example.aicamera.data.db.dao.AlbumPhotoDao
import com.example.aicamera.data.db.entity.AlbumPhotoEntity
import kotlinx.coroutines.flow.Flow

/** Repository 实现（骨架） */
class AlbumRepositoryImpl(
    private val albumPhotoDao: AlbumPhotoDao
) : AlbumRepository {

    override fun observeAllPhotos(): Flow<List<AlbumPhotoEntity>> {
        return albumPhotoDao.observeAll()
    }

    override suspend fun insertPhoto(entity: AlbumPhotoEntity): Long {
        return albumPhotoDao.insertIgnore(entity)
    }

    override suspend fun updateTextById(id: Long, text: String): Int {
        return albumPhotoDao.updateTextById(id, text)
    }

    override suspend fun updateTextByIds(ids: List<Long>, text: String): Int {
        return albumPhotoDao.updateTextByIds(ids, text)
    }

    override suspend fun getPhotosByIds(ids: List<Long>): List<AlbumPhotoEntity> {
        return if (ids.isEmpty()) emptyList() else albumPhotoDao.getByIds(ids)
    }

    override suspend fun deletePhotosByIds(ids: List<Long>): Int {
        return albumPhotoDao.deleteByIds(ids)
    }
}
