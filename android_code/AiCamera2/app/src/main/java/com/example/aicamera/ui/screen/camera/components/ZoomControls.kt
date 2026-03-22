package com.example.aicamera.ui.screen.camera.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun ZoomButton(
    currentZoom: Float,
    modifier: Modifier = Modifier
) {
    val animatedZoom = animateFloatAsState(
        targetValue = currentZoom,
        animationSpec = tween(durationMillis = 300)
    )

    Row(
        modifier = modifier
            .background(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format(Locale.US, "%.1fx", animatedZoom.value),
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
