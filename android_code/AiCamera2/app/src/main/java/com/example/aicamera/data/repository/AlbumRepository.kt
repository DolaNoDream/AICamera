package com.example.aicamera.data.repository

import com.example.aicamera.data.db.entity.AlbumPhotoEntity
import kotlinx.coroutines.flow.Flow

/** Repository（骨架） */
interface AlbumRepository {

    /** 监听全部照片（按时间倒序） */
    fun observeAllPhotos(): Flow<List<AlbumPhotoEntity>>

    /** 保存 photo 记录（插入时自动去重：同 filePath 不会重复入库） */
    suspend fun insertPhoto(entity: AlbumPhotoEntity): Long
}
