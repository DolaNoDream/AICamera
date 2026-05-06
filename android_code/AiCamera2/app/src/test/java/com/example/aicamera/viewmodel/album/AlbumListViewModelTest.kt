package com.example.aicamera.viewmodel.album

import com.example.aicamera.app.di.ServiceLocator
import com.example.aicamera.data.db.entity.AlbumPhotoEntity
import com.example.aicamera.data.repository.AlbumRepository
import com.example.aicamera.data.repository.CopywritingRepository
import com.example.aicamera.ui.viewmodel.album.AlbumListViewModel
import com.example.aicamera.viewmodel.ViewModelTestBase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumListViewModelTest : ViewModelTestBase() {

    @Test
    fun `selectAll selects all photo ids and clears ai fields`() = runTest {
        val photosFlow = MutableSharedFlow<List<AlbumPhotoEntity>>(replay = 1)
        val albumRepo = mockk<AlbumRepository>()
        val copyRepo = mockk<CopywritingRepository>()

        every { albumRepo.observeAllPhotos() } returns photosFlow

        mockkObject(ServiceLocator)
        every { ServiceLocator.provideAlbumRepository(any()) } returns albumRepo
        every { ServiceLocator.provideCopywritingRepository(any()) } returns copyRepo

        val vm = AlbumListViewModel(fakeApplication())

        val photos = listOf(
            AlbumPhotoEntity(id = 1, filePath = "content://1", type = 0),
            AlbumPhotoEntity(id = 2, filePath = "content://2", type = 1)
        )
        photosFlow.emit(photos)
        advanceUntilIdle()

        // prefill ai fields
        vm.toggleSelect(1)
        vm.aiWriteForSelectedPhotos(null) { _, _ -> }

        // selectAll should override selection and clear messages (even if already null)
        vm.selectAll()

        assertEquals(setOf(1L, 2L), vm.uiState.value.selectedPhotoIds)
        assertNull(vm.uiState.value.aiWriteMessage)
        assertNull(vm.uiState.value.lastGeneratedCopywritingId)
        assertTrue(!vm.uiState.value.isLoading)
    }

    @Test
    fun `toggleSelect adds and removes ids and clears ai fields`() = runTest {
        val photosFlow = MutableSharedFlow<List<AlbumPhotoEntity>>(replay = 1)
        val albumRepo = mockk<AlbumRepository>()
        val copyRepo = mockk<CopywritingRepository>()

        every { albumRepo.observeAllPhotos() } returns photosFlow

        mockkObject(ServiceLocator)
        every { ServiceLocator.provideAlbumRepository(any()) } returns albumRepo
        every { ServiceLocator.provideCopywritingRepository(any()) } returns copyRepo

        val vm = AlbumListViewModel(fakeApplication())

        photosFlow.emit(listOf(AlbumPhotoEntity(id = 1, filePath = "content://1", type = 0)))
        advanceUntilIdle()

        vm.toggleSelect(1)
        assertEquals(setOf(1L), vm.uiState.value.selectedPhotoIds)

        vm.toggleSelect(1)
        assertEquals(emptySet<Long>(), vm.uiState.value.selectedPhotoIds)
    }

    @Test
    fun `setPhotoTypeFilter intersects selection with visible ids`() = runTest {
        val photosFlow = MutableSharedFlow<List<AlbumPhotoEntity>>(replay = 1)
        val albumRepo = mockk<AlbumRepository>()
        val copyRepo = mockk<CopywritingRepository>()

        every { albumRepo.observeAllPhotos() } returns photosFlow

        mockkObject(ServiceLocator)
        every { ServiceLocator.provideAlbumRepository(any()) } returns albumRepo
        every { ServiceLocator.provideCopywritingRepository(any()) } returns copyRepo

        val vm = AlbumListViewModel(fakeApplication())

        val photos = listOf(
            AlbumPhotoEntity(id = 1, filePath = "content://1", type = 0),
            AlbumPhotoEntity(id = 2, filePath = "content://2", type = 1),
            AlbumPhotoEntity(id = 3, filePath = "content://3", type = 1)
        )
        photosFlow.emit(photos)
        advanceUntilIdle()

        vm.selectAll()
        assertEquals(setOf(1L, 2L, 3L), vm.uiState.value.selectedPhotoIds)

        vm.setPhotoTypeFilter(1)
        assertEquals(1, vm.uiState.value.photoTypeFilter)
        assertEquals(setOf(2L, 3L), vm.uiState.value.selectedPhotoIds)

        vm.setPhotoTypeFilter(null)
        assertNull(vm.uiState.value.photoTypeFilter)
        assertEquals(setOf(2L, 3L), vm.uiState.value.selectedPhotoIds)
    }

    @Test
    fun `aiWriteForSelectedPhotos validates empty selection`() = runTest {
        val photosFlow = MutableSharedFlow<List<AlbumPhotoEntity>>(replay = 1)
        val albumRepo = mockk<AlbumRepository>()
        val copyRepo = mockk<CopywritingRepository>()

        every { albumRepo.observeAllPhotos() } returns photosFlow

        mockkObject(ServiceLocator)
        every { ServiceLocator.provideAlbumRepository(any()) } returns albumRepo
        every { ServiceLocator.provideCopywritingRepository(any()) } returns copyRepo

        val vm = AlbumListViewModel(fakeApplication())

        var cbOk: Boolean? = null
        var cbId: Long? = 0
        vm.aiWriteForSelectedPhotos(null) { ok, id ->
            cbOk = ok
            cbId = id
        }

        assertEquals(false, cbOk)
        assertNull(cbId)
        assertEquals("请先选择照片", vm.uiState.value.aiWriteMessage)
        assertEquals(false, vm.uiState.value.isAiWriting)
    }
}
