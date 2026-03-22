package com.example.aicamera.ui.screen.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.aicamera.R
import com.example.aicamera.ui.uistate.camera.CameraMode
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp

@Composable
fun CameraModeTabs(
    modes: List<CameraMode>,
    selectedMode: CameraMode,
    onModeSelected: (CameraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(2.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            items(modes) { mode ->
                val label = when (mode) {
                    CameraMode.Standard -> stringResource(id = R.string.camera_mode_standard)
                    CameraMode.AiSuggestion -> stringResource(id = R.string.camera_mode_ai_suggestion)
                    CameraMode.AiPose -> stringResource(id = R.string.camera_mode_ai_pose)
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = if (mode == selectedMode) Color.White.copy(alpha = 0.10f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable(
                            indication = LocalIndication.current,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onModeSelected(mode) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (mode == selectedMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
