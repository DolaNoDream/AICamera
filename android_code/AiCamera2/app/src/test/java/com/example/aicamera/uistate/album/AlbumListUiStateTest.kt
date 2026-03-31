package com.example.aicamera.uistate.album

import com.example.aicamera.ui.uistate.album.AlbumListUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumListUiStateTest {

    @Test
    fun default_values_are_expected() {
        val state = AlbumListUiState()

        assertTrue(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.photos.isEmpty())
        assertNull(state.photoTypeFilter)
        assertTrue(state.selectedPhotoIds.isEmpty())
        assertFalse(state.isAiWriting)
        assertNull(state.aiWriteMessage)
        assertNull(state.lastGeneratedCopywritingId)
    }

    @Test
    fun copy_updates_only_target_fields() {
        val old = AlbumListUiState(
            isLoading = false,
            selectedPhotoIds = setOf(1L, 2L),
            aiWriteMessage = "old",
            lastGeneratedCopywritingId = 10L
        )

        val new = old.copy(
            selectedPhotoIds = old.selectedPhotoIds + 3L,
            aiWriteMessage = null,
            lastGeneratedCopywritingId = null
        )

        assertEquals(setOf(1L, 2L), old.selectedPhotoIds)
        assertEquals("old", old.aiWriteMessage)
        assertEquals(10L, old.lastGeneratedCopywritingId)

        assertEquals(setOf(1L, 2L, 3L), new.selectedPhotoIds)
        assertNull(new.aiWriteMessage)
        assertNull(new.lastGeneratedCopywritingId)
        assertEquals(old.isLoading, new.isLoading)
        assertEquals(old.photoTypeFilter, new.photoTypeFilter)
    }
}
