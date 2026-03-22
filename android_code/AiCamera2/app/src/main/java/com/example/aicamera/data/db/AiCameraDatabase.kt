package com.example.aicamera.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.aicamera.data.db.dao.AlbumPhotoDao
import com.example.aicamera.data.db.entity.AlbumPhotoEntity

/**
 * App 本地数据库（Room）
 *
 * 目前仅用于保存拍摄照片的索引信息（photo 表）。
 * 后续可扩展更多表（例如 AI 手账、构图建议缓存等）。
 */
@Database(
    entities = [AlbumPhotoEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AiCameraDatabase : RoomDatabase() {

    abstract fun albumPhotoDao(): AlbumPhotoDao

    companion object {
        @Volatile
        private var INSTANCE: AiCameraDatabase? = null

        fun getInstance(context: Context): AiCameraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AiCameraDatabase::class.java,
                    "aicamera.db"
                )
                    // 目前版本 1，后续 schema 变化请用 migration 替代
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
