package com.example.aicamera.ui.screen.camera.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aicamera.R

@Composable
fun ZoomSlider(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Slider(
            value = currentZoom,
            onValueChange = onZoomChange,
            valueRange = minZoom..maxZoom,
            steps = (maxZoom - minZoom).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(id = R.string.zoom_value, currentZoom),
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

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
            .background(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format("%.1fx", animatedZoom.value),
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
