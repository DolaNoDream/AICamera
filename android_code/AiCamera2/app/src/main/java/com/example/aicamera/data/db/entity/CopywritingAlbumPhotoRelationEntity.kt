package com.example.aicamera.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 文案-照片 关联表（多对多）
 *
 * 单向级联删除：
 * - 删除文案(copywriting) => 自动删除所有关联记录（CASCADE）
 * - 删除照片(photo)       => 自动删除所有关联记录（CASCADE）
 * - 删除关联记录不会影响文案/照片本体
 */
@Entity(
    tableName = "copywriting_albumphoto_relation",
    primaryKeys = ["copywriting_id", "albumphoto_id"],
    indices = [
        Index(value = ["copywriting_id"]),
        Index(value = ["albumphoto_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CopywritingEntity::class,
            parentColumns = ["id"],
            childColumns = ["copywriting_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AlbumPhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumphoto_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CopywritingAlbumPhotoRelationEntity(
    @ColumnInfo(name = "copywriting_id")
    val copywritingId: Long,

    @ColumnInfo(name = "albumphoto_id")
    val albumPhotoId: Long,

    /** 建立关联的时间（epoch millis） */
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis()
)
