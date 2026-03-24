package com.example.aicamera.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 文案表实体
 *
 * 对齐后端“AI 成文”结果的本地缓存。
 *
 * 说明：
 * - 时间字段使用 epoch millis（Long），与 [AlbumPhotoEntity] 保持一致，避免额外 TypeConverter。
 */
@Entity(
    tableName = "copywriting",
    indices = [
        Index(value = ["create_time"]),
        Index(value = ["update_time"])
    ]
)
data class CopywritingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 文案内容 */
    @ColumnInfo(name = "content")
    val content: String,

    /** 创建时间（epoch millis） */
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),

    /** 更新时间（epoch millis） */
    @ColumnInfo(name = "update_time")
    val updateTime: Long = System.currentTimeMillis()
)
