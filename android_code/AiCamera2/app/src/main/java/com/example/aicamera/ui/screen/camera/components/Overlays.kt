package com.example.aicamera.ui.screen.camera.components

import androidx.compose.foundation.background
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
fun LoadingOverlay(
    status: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = status, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun SaveSuccessOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.photo_taken),
                color = Color.Green,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun ErrorOverlay(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(color = Color(0xFF1F1F1F), shape = RoundedCornerShape(12.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = stringResource(id = R.string.error), color = Color.Red, fontSize = 18.sp)
            Text(text = message, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(color = Color(0xFF81C784), shape = RoundedCornerShape(6.dp))
                        .padding(8.dp)
                        .then(Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    // 保留原调用语义（点击逻辑由调用方决定是否 wrap clickable）
                    Text(text = stringResource(id = R.string.retry), color = Color.White)
                }
            }
        }
    }
}
