package com.example.aicamera.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.aicamera.data.db.entity.CopywritingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CopywritingDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CopywritingEntity): Long

    @Update
    suspend fun update(entity: CopywritingEntity): Int

    @Query("SELECT * FROM copywriting ORDER BY create_time DESC")
    fun observeAll(): Flow<List<CopywritingEntity>>

    @Query("SELECT * FROM copywriting WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CopywritingEntity?

    @Query("DELETE FROM copywriting WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM copywriting WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    @Query("UPDATE copywriting SET content = :content, update_time = :updateTime WHERE id = :id")
    suspend fun updateContentById(id: Long, content: String, updateTime: Long = System.currentTimeMillis()): Int
}
