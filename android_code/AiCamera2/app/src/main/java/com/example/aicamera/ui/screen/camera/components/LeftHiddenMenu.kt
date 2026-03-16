package com.example.aicamera.ui.screen.camera.components

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.aicamera.R
import com.example.aicamera.data.storage.ImageDownloadHelper

@Composable
fun LeftHiddenMenu(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    poseGuideText: String,
    poseSuggestionText: String,
    poseImageUrl: String,
    isLoading: Boolean,
    errorMessage: String?,
    showConfirmButton: Boolean,
    onConfirm: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val poseImageBitmap = produceState<Bitmap?>(initialValue = null, poseImageUrl) {
        value = null
        if (poseImageUrl.isNotBlank()) {
            val file = ImageDownloadHelper.downloadToCache(context, poseImageUrl)
            value = file?.let { ImageDownloadHelper.decodeBitmap(it) }
        }
    }.value

    Row(modifier = modifier.padding(start = 10.dp)) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(300.dp)
                .background(Color.White.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(7.dp))
                .clickable(onClick = onToggle)
        )

        Box(
            modifier = Modifier
                .width(200.dp)
                .height(300.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(16.dp)
                        .clickable(onClick = onToggle)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.pose_guide_title),
                                color = Color(0xFFFFC107),
                                fontSize = 14.sp
                            )
                            Text(
                                text = stringResource(id = R.string.clear),
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { onClear() }
                            )
                        }

                        when {
                            isLoading -> {
                                Text(
                                    text = stringResource(id = R.string.pose_guide_loading),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                            !errorMessage.isNullOrBlank() -> {
                                Text(
                                    text = stringResource(id = R.string.pose_guide_error, errorMessage),
                                    color = Color(0xFFFF5252),
                                    fontSize = 12.sp
                                )
                            }
                            poseSuggestionText.isBlank() && poseImageUrl.isBlank() -> {
                                Text(
                                    text = stringResource(id = R.string.pose_guide_empty),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                            else -> {
                                if (poseImageBitmap != null) {
                                    AndroidView(
                                        factory = { ctx ->
                                            ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                                        },
                                        update = { view ->
                                            (view as ImageView).setImageBitmap(poseImageBitmap)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )
                                } else if (poseImageUrl.isNotBlank()) {
                                    Text(
                                        text = stringResource(id = R.string.pose_guide_image_loading),
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }

                                if (poseSuggestionText.isNotBlank()) {
                                    Text(
                                        text = poseSuggestionText,
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        if (showConfirmButton) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = Color(0xFF81C784), shape = RoundedCornerShape(8.dp))
                                    .clickable { onConfirm() }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.pose_confirm),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
