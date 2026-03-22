package com.example.aicamera.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.aicamera.data.db.entity.AlbumPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumPhotoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: AlbumPhotoEntity): Long

    @Query("SELECT * FROM photo ORDER BY create_time DESC")
    fun observeAll(): Flow<List<AlbumPhotoEntity>>

    @Query("SELECT * FROM photo WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AlbumPhotoEntity?

    @Query("DELETE FROM photo WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("UPDATE photo SET text = :text WHERE id = :id")
    fun updateTextById(id: Long, text: String): Int

    @Query("UPDATE photo SET text = :text WHERE id IN (:ids)")
    fun updateTextByIds(ids: List<Long>, text: String): Int
}
