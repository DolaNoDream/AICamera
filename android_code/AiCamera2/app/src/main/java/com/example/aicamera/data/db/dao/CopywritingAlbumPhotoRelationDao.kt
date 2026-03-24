package com.example.aicamera.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.aicamera.data.db.entity.AlbumPhotoEntity
import com.example.aicamera.data.db.entity.CopywritingAlbumPhotoRelationEntity
import com.example.aicamera.data.db.entity.CopywritingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CopywritingAlbumPhotoRelationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: CopywritingAlbumPhotoRelationEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(entities: List<CopywritingAlbumPhotoRelationEntity>): List<Long>

    @Query("DELETE FROM copywriting_albumphoto_relation WHERE copywriting_id = :copywritingId AND albumphoto_id = :albumPhotoId")
    suspend fun deleteOne(copywritingId: Long, albumPhotoId: Long): Int

    @Query("DELETE FROM copywriting_albumphoto_relation WHERE copywriting_id = :copywritingId")
    suspend fun deleteByCopywritingId(copywritingId: Long): Int

    @Query("DELETE FROM copywriting_albumphoto_relation WHERE albumphoto_id = :albumPhotoId")
    suspend fun deleteByAlbumPhotoId(albumPhotoId: Long): Int

    @Query("SELECT * FROM copywriting_albumphoto_relation WHERE copywriting_id = :copywritingId ORDER BY create_time DESC")
    fun observeRelationsByCopywritingId(copywritingId: Long): Flow<List<CopywritingAlbumPhotoRelationEntity>>

    @Query("SELECT * FROM copywriting_albumphoto_relation WHERE albumphoto_id = :albumPhotoId ORDER BY create_time DESC")
    fun observeRelationsByAlbumPhotoId(albumPhotoId: Long): Flow<List<CopywritingAlbumPhotoRelationEntity>>

    @Transaction
    @Query(
        """
        SELECT c.*
        FROM copywriting c
        INNER JOIN copywriting_albumphoto_relation r
            ON c.id = r.copywriting_id
        WHERE r.albumphoto_id = :albumPhotoId
        ORDER BY r.create_time DESC
        """
    )
    fun observeCopywritingsForPhoto(albumPhotoId: Long): Flow<List<CopywritingEntity>>

    @Transaction
    @Query(
        """
        SELECT p.*
        FROM photo p
        INNER JOIN copywriting_albumphoto_relation r
            ON p.id = r.albumphoto_id
        WHERE r.copywriting_id = :copywritingId
        ORDER BY r.create_time DESC
        """
    )
    fun observePhotosForCopywriting(copywritingId: Long): Flow<List<AlbumPhotoEntity>>

    @Query("SELECT * FROM copywriting_albumphoto_relation")
    fun observeAllRelations(): Flow<List<CopywritingAlbumPhotoRelationEntity>>
}
