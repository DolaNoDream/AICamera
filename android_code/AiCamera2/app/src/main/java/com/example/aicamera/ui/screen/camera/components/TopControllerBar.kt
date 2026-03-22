package com.example.aicamera.ui.screen.camera.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.remember

@Composable
fun TopControllerBar(
    isMenuExpanded: Boolean,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(24.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .clickable(
                        indication = LocalIndication.current,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onMenuClick
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "菜单",
                    tint = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        AnimatedVisibility(
            visible = isMenuExpanded,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text(
                        text = "AI语音助手",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "AI姿势指导",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
