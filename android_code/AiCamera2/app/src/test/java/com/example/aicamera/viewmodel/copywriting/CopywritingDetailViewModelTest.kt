package com.example.aicamera.viewmodel.copywriting

import com.example.aicamera.app.di.ServiceLocator
import com.example.aicamera.data.repository.AlbumRepository
import com.example.aicamera.data.repository.CopywritingRepository
import com.example.aicamera.ui.viewmodel.copywriting.CopywritingDetailViewModel
import com.example.aicamera.viewmodel.ViewModelTestBase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CopywritingDetailViewModelTest : ViewModelTestBase() {

    @Test
    fun `load with invalid id sets error immediately`() = runTest {
        val copyRepo = mockk<CopywritingRepository>(relaxed = true)
        val albumRepo = mockk<AlbumRepository>(relaxed = true)

        mockkObject(ServiceLocator)
        every { ServiceLocator.provideCopywritingRepository(any()) } returns copyRepo
        every { ServiceLocator.provideAlbumRepository(any()) } returns albumRepo

        val vm = CopywritingDetailViewModel(fakeApplication())
        vm.load(0)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("无效的文案ID", vm.uiState.value.errorMessage)
    }

    @Test
    fun `enterEdit and cancelEdit toggles editing flags`() = runTest {
        val copyRepo = mockk<CopywritingRepository>(relaxed = true)
        val albumRepo = mockk<AlbumRepository>(relaxed = true)

        // avoid flows doing work
        every { copyRepo.observePhotosForCopywriting(any()) } returns MutableSharedFlow(replay = 1)

        mockkObject(ServiceLocator)
        every { ServiceLocator.provideCopywritingRepository(any()) } returns copyRepo
        every { ServiceLocator.provideAlbumRepository(any()) } returns albumRepo

        val vm = CopywritingDetailViewModel(fakeApplication())

        // preset content
        vm.load(1)
        advanceUntilIdle()
        vm.enterEdit()
        assertTrue(vm.uiState.value.isEditing)

        vm.onEditContentChange("x")
        assertEquals("x", vm.uiState.value.editContent)

        vm.cancelEdit()
        assertFalse(vm.uiState.value.isEditing)
    }

    @Test
    fun `requestRemovePhoto and cancelRemovePhoto`() = runTest {
        val copyRepo = mockk<CopywritingRepository>(relaxed = true)
        val albumRepo = mockk<AlbumRepository>(relaxed = true)
        every { copyRepo.observePhotosForCopywriting(any()) } returns MutableSharedFlow(replay = 1)

        mockkObject(ServiceLocator)
        every { ServiceLocator.provideCopywritingRepository(any()) } returns copyRepo
        every { ServiceLocator.provideAlbumRepository(any()) } returns albumRepo

        val vm = CopywritingDetailViewModel(fakeApplication())
        vm.requestRemovePhoto(10)
        assertEquals(10L, vm.uiState.value.pendingRemovePhotoId)

        vm.cancelRemovePhoto()
        assertEquals(null, vm.uiState.value.pendingRemovePhotoId)
    }
}
