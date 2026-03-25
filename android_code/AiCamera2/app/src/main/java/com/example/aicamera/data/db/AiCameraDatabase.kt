package com.example.aicamera.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.aicamera.data.db.dao.AlbumPhotoDao
import com.example.aicamera.data.db.dao.CopywritingAlbumPhotoRelationDao
import com.example.aicamera.data.db.dao.CopywritingDao
import com.example.aicamera.data.db.entity.AlbumPhotoEntity
import com.example.aicamera.data.db.entity.CopywritingAlbumPhotoRelationEntity
import com.example.aicamera.data.db.entity.CopywritingEntity

/**
 * App 本地数据库（Room）
 *
 * 目前仅用于保存拍摄照片的索引信息（photo 表）。
 * 后续可扩展更多表（例如 AI 手账、构图建议缓存等）。
 */
@Database(
    entities = [
        AlbumPhotoEntity::class,
        CopywritingEntity::class,
        CopywritingAlbumPhotoRelationEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AiCameraDatabase : RoomDatabase() {

    abstract fun albumPhotoDao(): AlbumPhotoDao

    abstract fun copywritingDao(): CopywritingDao

    abstract fun copywritingAlbumPhotoRelationDao(): CopywritingAlbumPhotoRelationDao

    companion object {
        @Volatile
        private var INSTANCE: AiCameraDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 文案表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS copywriting (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        create_time INTEGER NOT NULL,
                        update_time INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_copywriting_create_time ON copywriting(create_time)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_copywriting_update_time ON copywriting(update_time)")

                // 关联表：单向级联删除（删除 parent 自动清理关联表记录）
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS copywriting_albumphoto_relation (
                        copywriting_id INTEGER NOT NULL,
                        albumphoto_id INTEGER NOT NULL,
                        create_time INTEGER NOT NULL,
                        PRIMARY KEY(copywriting_id, albumphoto_id),
                        FOREIGN KEY(copywriting_id) REFERENCES copywriting(id) ON DELETE CASCADE,
                        FOREIGN KEY(albumphoto_id) REFERENCES photo(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_copywriting_albumphoto_relation_copywriting_id ON copywriting_albumphoto_relation(copywriting_id)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_copywriting_albumphoto_relation_albumphoto_id ON copywriting_albumphoto_relation(albumphoto_id)"
                )
            }
        }

        fun getInstance(context: Context): AiCameraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AiCameraDatabase::class.java,
                    "aicamera.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
