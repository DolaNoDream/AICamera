package com.example.aicamera.app.di

import android.content.Context
import com.example.aicamera.data.db.AiCameraDatabase
import com.example.aicamera.data.repository.AlbumRepository
import com.example.aicamera.data.repository.AlbumRepositoryImpl

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
}
