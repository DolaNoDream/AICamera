package com.example.aicamera.ui.nav

/**
 * 导航路由常量。
 */
object Routes {
    const val Camera = "camera"
    const val AlbumList = "album_list"

    const val PhotoDetail = "photo_detail/{photoId}"

    const val CopywritingList = "copywriting_list"

    const val CopywritingDetail = "copywriting_detail/{copywritingId}"

    fun photoDetail(photoId: Long): String = "photo_detail/$photoId"

    fun copywritingDetail(copywritingId: Long): String = "copywriting_detail/$copywritingId"
}
