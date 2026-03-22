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
}
