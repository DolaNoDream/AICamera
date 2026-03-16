package com.example.aicamera.ui.screen.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aicamera.R

@Composable
fun AiSuggestionStatusBox(
    recognizedText: String,
    guideText: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color = Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.ai_suggestion_header),
                    color = Color(0xFF81C784),
                    fontSize = 12.sp
                )
                Text(
                    text = stringResource(id = R.string.clear),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onClear() }
                )
            }

            val voiceText = if (recognizedText.isNotBlank()) recognizedText else stringResource(id = R.string.voice_empty_hint)
            val guideHint = if (guideText.isNotBlank()) guideText else stringResource(id = R.string.guide_empty_hint)

            Text(
                text = stringResource(id = R.string.voice_label_prefix, voiceText),
                color = Color.White,
                fontSize = 12.sp
            )
            Text(
                text = stringResource(id = R.string.guide_label_prefix, guideHint),
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}
