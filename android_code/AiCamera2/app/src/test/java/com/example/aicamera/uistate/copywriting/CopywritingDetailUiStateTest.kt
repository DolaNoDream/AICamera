package com.example.aicamera.uistate.copywriting

import com.example.aicamera.ui.uistate.copywriting.CopywritingDetailUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CopywritingDetailUiStateTest {

    @Test
    fun default_values_are_expected() {
        val state = CopywritingDetailUiState()

        assertTrue(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(0L, state.copywritingId)
        assertEquals("", state.content)
        assertEquals(0L, state.createTime)
        assertEquals(0L, state.updateTime)
        assertTrue(state.photos.isEmpty())
        assertFalse(state.isEditing)
        assertEquals("", state.editContent)
        assertFalse(state.isAddingPhotos)
        assertTrue(state.candidatePhotos.isEmpty())
        assertTrue(state.selectedAddPhotoIds.isEmpty())
        assertNull(state.pendingRemovePhotoId)
    }

    @Test
    fun copy_updates_only_target_fields() {
        val old = CopywritingDetailUiState(
            isLoading = false,
            copywritingId = 5L,
            content = "hello"
        )

        val new = old.copy(
            isEditing = true,
            editContent = "hello updated",
            isAddingPhotos = true,
            selectedAddPhotoIds = setOf(1L, 2L),
            pendingRemovePhotoId = 9L
        )

        assertFalse(old.isEditing)
        assertEquals("", old.editContent)
        assertFalse(old.isAddingPhotos)
        assertTrue(old.selectedAddPhotoIds.isEmpty())
        assertNull(old.pendingRemovePhotoId)

        assertTrue(new.isEditing)
        assertEquals("hello updated", new.editContent)
        assertTrue(new.isAddingPhotos)
        assertEquals(setOf(1L, 2L), new.selectedAddPhotoIds)
        assertEquals(9L, new.pendingRemovePhotoId)
        assertEquals(old.copywritingId, new.copywritingId)
        assertEquals(old.content, new.content)
    }
}
