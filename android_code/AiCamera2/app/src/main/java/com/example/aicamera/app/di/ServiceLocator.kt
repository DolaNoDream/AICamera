package com.example.aicamera.app.di

import android.content.Context
import com.example.aicamera.data.db.AiCameraDatabase
import com.example.aicamera.data.repository.AlbumRepository
import com.example.aicamera.data.repository.AlbumRepositoryImpl
import com.example.aicamera.data.repository.CopywritingRepository
import com.example.aicamera.data.repository.CopywritingRepositoryImpl

/**
 * 简易 ServiceLocator。
 *
 * 目标：在不引入 Hilt/Koin 的情况下，提供最小可用的单例依赖（DB/Repository）。
 */
object ServiceLocator {

    @Volatile
    private var database: AiCameraDatabase? = null

    fun provideDatabase(context: Context): AiCameraDatabase {
        return database ?: synchronized(this) {
            database ?: AiCameraDatabase.getInstance(context).also { database = it }
        }
    }

    @Volatile
    private var albumRepository: AlbumRepository? = null

    fun provideAlbumRepository(context: Context): AlbumRepository {
        return albumRepository ?: synchronized(this) {
            albumRepository ?: AlbumRepositoryImpl(
                provideDatabase(context).albumPhotoDao()
            ).also { albumRepository = it }
        }
    }

    @Volatile
    private var copywritingRepository: CopywritingRepository? = null

    fun provideCopywritingRepository(context: Context): CopywritingRepository {
        return copywritingRepository ?: synchronized(this) {
            copywritingRepository ?: run {
                val db = provideDatabase(context)
                CopywritingRepositoryImpl(
                    database = db,
                    albumPhotoDao = db.albumPhotoDao(),
                    copywritingDao = db.copywritingDao(),
                    relationDao = db.copywritingAlbumPhotoRelationDao()
                )
            }.also { copywritingRepository = it }
        }
    }
}
