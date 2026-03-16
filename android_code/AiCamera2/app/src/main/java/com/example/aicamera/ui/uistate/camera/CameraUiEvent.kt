package com.example.aicamera.ui.uistate.camera

/**
 * CameraUiEvent：一次性事件（One-off events）。
 *
 * 为什么需要：
 * - Snackbar/Toast/导航/打开系统设置 这类事件不应该放在 UiState 里（否则旋转屏幕/重组会重复触发）。
 * - ViewModel 通过 Event 流发出一次性动作，UI 收到后消费。
 */
sealed interface CameraUiEvent {
    data class ShowSnackbar(val message: String) : CameraUiEvent
    data class NavigateToPhotoDetail(val photoId: Long) : CameraUiEvent
    data object NavigateToAlbumList : CameraUiEvent
}
