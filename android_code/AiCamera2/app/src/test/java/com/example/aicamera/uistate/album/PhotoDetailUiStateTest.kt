package com.example.aicamera.uistate.album

import com.example.aicamera.ui.uistate.album.PhotoDetailUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoDetailUiStateTest {

    @Test
    fun default_values_are_expected() {
        val state = PhotoDetailUiState()

        assertTrue(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.photo)

        assertFalse(state.isAiEditing)
        assertEquals("", state.aiEditMessage)

        assertFalse(state.isAiWriting)
        assertEquals("", state.aiWriteMessage)
    }

    @Test
    fun copy_updates_only_target_fields() {
        val old = PhotoDetailUiState(isLoading = false)

        val new = old.copy(
            isAiEditing = true,
            aiEditMessage = "editing",
            isAiWriting = true,
            aiWriteMessage = "writing"
        )

        assertFalse(old.isAiEditing)
        assertEquals("", old.aiEditMessage)
        assertFalse(old.isAiWriting)
        assertEquals("", old.aiWriteMessage)

        assertTrue(new.isAiEditing)
        assertEquals("editing", new.aiEditMessage)
        assertTrue(new.isAiWriting)
        assertEquals("writing", new.aiWriteMessage)
    }
}
