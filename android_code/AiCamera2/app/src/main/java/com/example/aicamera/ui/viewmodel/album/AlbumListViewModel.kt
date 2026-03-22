package com.example.aicamera.ui.viewmodel.album

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicamera.app.di.ServiceLocator
import com.example.aicamera.ui.uistate.album.AlbumListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * 相册列表 VM。
 */
class AlbumListViewModel(application: Application) : AndroidViewModel(application) {

    private val albumRepository = ServiceLocator.provideAlbumRepository(application)

    private val _uiState = MutableStateFlow(AlbumListUiState())
    val uiState: StateFlow<AlbumListUiState> = _uiState.asStateFlow()

    init {
        observeAlbum()
    }

    private fun observeAlbum() {
        viewModelScope.launch {
            albumRepository.observeAllPhotos()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "加载相册失败"
                    )
                }
                .collect { list ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        photos = list
                    )
                }
        }
    }
}


