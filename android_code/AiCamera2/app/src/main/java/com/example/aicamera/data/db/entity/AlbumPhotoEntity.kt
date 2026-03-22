package com.example.aicamera.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * photo 表实体
 *
 * 说明：
 * - 这里将 create_time 以 epoch millis（Long）形式保存，避免额外 TypeConverter，且更好排序/筛选。
 * - file_path 目前存储 MediaStore 的 contentUri（如 content://...），这同样也是“系统相册真实路径”的可靠引用。
 */
@Entity(
    tableName = "photo",
    indices = [
        Index(value = ["file_path"], unique = true),
        Index(value = ["create_time"]) // 常用：按时间倒序
    ]
)
data class AlbumPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 文件本地存储路径 / 或 MediaStore contentUri 字符串（推荐，适配分区存储） */
    @ColumnInfo(name = "file_path")
    val filePath: String,

    /** 0:原图 1:P图 2:手账 */
    @ColumnInfo(name = "type")
    val type: Int = 0,

    /** 文本描述（后续可用于手账/AI备注） */
    @ColumnInfo(name = "text")
    val text: String? = null,

    /** 创建时间（epoch millis） */
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "width")
    val width: Int? = null,

    @ColumnInfo(name = "height")
    val height: Int? = null,

    /** 文件大小（字节） */
    @ColumnInfo(name = "file_size")
    val fileSize: Long? = null
)
