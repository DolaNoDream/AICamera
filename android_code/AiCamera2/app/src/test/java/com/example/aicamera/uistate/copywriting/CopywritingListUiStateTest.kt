package com.example.aicamera.uistate.copywriting

import com.example.aicamera.ui.uistate.copywriting.CopywritingListUiState
import com.example.aicamera.ui.uistate.copywriting.CopywritingSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CopywritingListUiStateTest {

    @Test
    fun default_values_are_expected() {
        val state = CopywritingListUiState()

        assertTrue(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.items.isEmpty())
        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedIds.isEmpty())
        assertEquals(0, state.selectedCount)

        assertTrue(state.enableContentFilter)
        assertFalse(state.enableCreateTimeFilter)
        assertFalse(state.enableUpdateTimeFilter)

        assertEquals("", state.contentQuery)
        assertNull(state.createDateFrom)
        assertNull(state.createDateTo)
        assertNull(state.updateDateFrom)
        assertNull(state.updateDateTo)

        assertEquals(CopywritingSort.CreateTimeDesc, state.sort)
    }

    @Test
    fun selected_count_tracks_selected_ids_size() {
        val state = CopywritingListUiState(selectedIds = setOf(1L, 2L, 3L))

        assertEquals(3, state.selectedCount)
    }

    @Test
    fun copy_updates_only_target_fields() {
        val old = CopywritingListUiState(
            isLoading = false,
            selectedIds = setOf(10L),
            contentQuery = "before"
        )

        val new = old.copy(
            isSelectionMode = true,
            selectedIds = old.selectedIds + 20L,
            contentQuery = "after",
            enableCreateTimeFilter = true,
            sort = CopywritingSort.UpdateTimeDesc
        )

        assertFalse(old.isSelectionMode)
        assertEquals(setOf(10L), old.selectedIds)
        assertEquals("before", old.contentQuery)
        assertFalse(old.enableCreateTimeFilter)
        assertEquals(CopywritingSort.CreateTimeDesc, old.sort)

        assertTrue(new.isSelectionMode)
        assertEquals(setOf(10L, 20L), new.selectedIds)
        assertEquals(2, new.selectedCount)
        assertEquals("after", new.contentQuery)
        assertTrue(new.enableCreateTimeFilter)
        assertEquals(CopywritingSort.UpdateTimeDesc, new.sort)
        assertEquals(old.isLoading, new.isLoading)
    }
}
