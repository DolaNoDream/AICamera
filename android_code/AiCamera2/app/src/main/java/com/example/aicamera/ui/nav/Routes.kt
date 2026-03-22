package com.example.aicamera.ui.nav

/**
 * 导航路由常量。
 */
object Routes {
    const val Camera = "camera"
    const val AlbumList = "album_list"

    const val PhotoDetail = "photo_detail/{photoId}"

    fun photoDetail(photoId: Long): String = "photo_detail/$photoId"
}
