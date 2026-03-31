package com.example.aicamera.viewmodel.album

import com.example.aicamera.app.di.ServiceLocator
import com.example.aicamera.data.db.AiCameraDatabase
import com.example.aicamera.data.db.dao.AlbumPhotoDao
import com.example.aicamera.data.db.entity.AlbumPhotoEntity
import com.example.aicamera.data.repository.AlbumRepository
import com.example.aicamera.data.repository.CopywritingRepository
import com.example.aicamera.ui.viewmodel.album.PhotoDetailViewModel
import com.example.aicamera.viewmodel.ViewModelTestBase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoDetailViewModelTest : ViewModelTestBase() {

    @Test
    fun `load when dao returns null sets error`() = runTest {
        val dao = mockk<AlbumPhotoDao>()
        coEvery { dao.getById(1) } returns null

        val db = mockk<AiCameraDatabase>()
        every { db.albumPhotoDao() } returns dao

        val albumRepo = mockk<AlbumRepository>(relaxed = true)
        val copyRepo = mockk<CopywritingRepository>(relaxed = true)

        mockkObject(ServiceLocator)
        every { ServiceLocator.provideDatabase(any()) } returns db
        every { ServiceLocator.provideAlbumRepository(any()) } returns albumRepo
        every { ServiceLocator.provideCopywritingRepository(any()) } returns copyRepo

        // FileManager is constructed inside VM; mock it out to avoid touching Android storage.
        mockkConstructor(com.example.aicamera.data.storage.FileManager::class)
        coEvery { anyConstructed<com.example.aicamera.data.storage.FileManager>().importImageFileToGallery(any()) } returns ""

        val vm = PhotoDetailViewModel(fakeApplication())
        vm.load(1)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.photo)
        assertEquals("未找到照片记录", vm.uiState.value.errorMessage)
    }

    @Test
    fun `load when dao returns entity sets photo and clears error`() = runTest {
        val entity = AlbumPhotoEntity(id = 2, filePath = "content://2", type = 0)

        val dao = mockk<AlbumPhotoDao>()
        coEvery { dao.getById(2) } returns entity

        val db = mockk<AiCameraDatabase>()
        every { db.albumPhotoDao() } returns dao

        val albumRepo = mockk<AlbumRepository>(relaxed = true)
        val copyRepo = mockk<CopywritingRepository>(relaxed = true)

        mockkObject(ServiceLocator)
        every { ServiceLocator.provideDatabase(any()) } returns db
        every { ServiceLocator.provideAlbumRepository(any()) } returns albumRepo
        every { ServiceLocator.provideCopywritingRepository(any()) } returns copyRepo

        mockkConstructor(com.example.aicamera.data.storage.FileManager::class)
        coEvery { anyConstructed<com.example.aicamera.data.storage.FileManager>().importImageFileToGallery(any()) } returns ""

        val vm = PhotoDetailViewModel(fakeApplication())
        vm.load(2)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(entity, vm.uiState.value.photo)
        assertEquals(null, vm.uiState.value.errorMessage)
    }

    @Test
    fun `deleteCurrentPhoto when photo is null returns false and sets error`() = runTest {
        val dao = mockk<AlbumPhotoDao>(relaxed = true)
        val db = mockk<AiCameraDatabase>()
        every { db.albumPhotoDao() } returns dao

        val albumRepo = mockk<AlbumRepository>(relaxed = true)
        val copyRepo = mockk<CopywritingRepository>(relaxed = true)

        mockkObject(ServiceLocator)
        every { ServiceLocator.provideDatabase(any()) } returns db
        every { ServiceLocator.provideAlbumRepository(any()) } returns albumRepo
        every { ServiceLocator.provideCopywritingRepository(any()) } returns copyRepo

        mockkConstructor(com.example.aicamera.data.storage.FileManager::class)

        val vm = PhotoDetailViewModel(fakeApplication())

        var result: Boolean? = null
        vm.deleteCurrentPhoto { ok -> result = ok }
        advanceUntilIdle()

        assertEquals(false, result)
        assertEquals("无可删除的照片", vm.uiState.value.errorMessage)
    }
}
