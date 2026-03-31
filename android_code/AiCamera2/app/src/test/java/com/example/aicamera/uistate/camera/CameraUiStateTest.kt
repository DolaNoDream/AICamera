package com.example.aicamera.uistate.camera

import androidx.camera.core.CameraSelector
import com.example.aicamera.ui.uistate.camera.CameraMode
import com.example.aicamera.ui.uistate.camera.CameraState
import com.example.aicamera.ui.uistate.camera.CameraUiState
import com.example.aicamera.ui.uistate.camera.ZoomUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraUiStateTest {

    @Test
    fun default_values_are_expected() {
        val state = CameraUiState()

        assertEquals(CameraState.Idle, state.cameraState)
        assertNull(state.errorMessage)

        assertFalse(state.voiceGuideEnabled)
        assertFalse(state.isListening)
        assertEquals("", state.voiceRecognitionResult)
        assertEquals("", state.lastUserIntent)

        assertEquals("", state.poseGuideText)
        assertEquals("", state.poseSuggestionText)
        assertEquals("", state.poseImageUrl)
        assertFalse(state.poseLoading)
        assertNull(state.poseErrorMessage)

        assertEquals(CameraSelector.LENS_FACING_BACK, state.currentLensFacing)
        assertEquals(ZoomUi(), state.zoom)

        assertEquals(0.5f, state.focusPointX)
        assertEquals(0.5f, state.focusPointY)

        assertEquals(CameraMode.Standard, state.selectedMode)
        assertFalse(state.isMenuExpanded)
        assertFalse(state.isLeftPanelExpanded)
        assertFalse(state.isAiVoiceBoxVisible)
    }

    @Test
    fun ai_voice_box_visibility_depends_on_mode() {
        val standard = CameraUiState(selectedMode = CameraMode.Standard)
        val suggestion = CameraUiState(selectedMode = CameraMode.AiSuggestion)
        val pose = CameraUiState(selectedMode = CameraMode.AiPose)

        assertFalse(standard.isAiVoiceBoxVisible)
        assertTrue(suggestion.isAiVoiceBoxVisible)
        assertTrue(pose.isAiVoiceBoxVisible)
    }

    @Test
    fun copy_updates_only_target_fields() {
        val old = CameraUiState(selectedMode = CameraMode.Standard)

        val new = old.copy(
            cameraState = CameraState.Ready,
            poseLoading = true,
            selectedMode = CameraMode.AiSuggestion,
            zoom = ZoomUi(currentZoom = 2f, minZoom = 1f, maxZoom = 10f)
        )

        assertEquals(CameraState.Idle, old.cameraState)
        assertFalse(old.poseLoading)
        assertEquals(CameraMode.Standard, old.selectedMode)

        assertEquals(CameraState.Ready, new.cameraState)
        assertTrue(new.poseLoading)
        assertEquals(CameraMode.AiSuggestion, new.selectedMode)
        assertEquals(2f, new.zoom.currentZoom)
        assertTrue(new.isAiVoiceBoxVisible)
    }
}
