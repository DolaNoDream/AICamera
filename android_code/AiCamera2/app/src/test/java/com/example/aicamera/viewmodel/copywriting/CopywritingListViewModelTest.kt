package com.example.aicamera.viewmodel.copywriting

import com.example.aicamera.app.di.ServiceLocator
import com.example.aicamera.data.repository.CopywritingRepository
import com.example.aicamera.data.repository.SeedResult
import com.example.aicamera.ui.viewmodel.copywriting.CopywritingListViewModel
import com.example.aicamera.viewmodel.ViewModelTestBase
import io.mockk.coEvery
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
class CopywritingListViewModelTest : ViewModelTestBase() {

    @Test
    fun `toggleSelect ignores non-positive id`() = runTest {
        val repo = mockk<CopywritingRepository>()
        coEvery { repo.seedMockDataIfNeeded() } returns SeedResult.SKIPPED_ALREADY_HAS_DATA
        every { repo.observeCopywritingsWithPhotoCount() } returns MutableSharedFlow(replay = 1)

        mockkObject(ServiceLocator)
        every { ServiceLocator.provideCopywritingRepository(any()) } returns repo

        val vm = CopywritingListViewModel(fakeApplication())
        advanceUntilIdle()

        vm.toggleSelect(0)
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
        assertFalse(vm.uiState.value.isSelectionMode)
    }

    @Test
    fun `enterSelectionMode and exitSelectionMode update state`() = runTest {
        val repo = mockk<CopywritingRepository>()
        coEvery { repo.seedMockDataIfNeeded() } returns SeedResult.SKIPPED_ALREADY_HAS_DATA
        every { repo.observeCopywritingsWithPhotoCount() } returns MutableSharedFlow(replay = 1)

        mockkObject(ServiceLocator)
        every { ServiceLocator.provideCopywritingRepository(any()) } returns repo

        val vm = CopywritingListViewModel(fakeApplication())
        advanceUntilIdle()

        vm.enterSelectionMode()
        assertTrue(vm.uiState.value.isSelectionMode)

        vm.toggleSelect(1)
        assertEquals(setOf(1L), vm.uiState.value.selectedIds)
        assertTrue(vm.uiState.value.isSelectionMode)

        vm.exitSelectionMode()
        assertFalse(vm.uiState.value.isSelectionMode)
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `content query updates`() = runTest {
        val repo = mockk<CopywritingRepository>()
        coEvery { repo.seedMockDataIfNeeded() } returns SeedResult.SKIPPED_ALREADY_HAS_DATA
        every { repo.observeCopywritingsWithPhotoCount() } returns MutableSharedFlow(replay = 1)

        mockkObject(ServiceLocator)
        every { ServiceLocator.provideCopywritingRepository(any()) } returns repo

        val vm = CopywritingListViewModel(fakeApplication())
        advanceUntilIdle()

        vm.onContentQueryChange("hi")
        assertEquals("hi", vm.uiState.value.contentQuery)

        vm.clearContentQuery()
        assertEquals("", vm.uiState.value.contentQuery)
    }
}
