package com.filyar.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filyar.app.RemoteFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ViewerBg = Color(0xFF050807)
private val ViewerFg = Color(0xFFFFFFFF)

@Composable
fun ImageViewerScreen(file: RemoteFile, vm: BrowserViewModel, onClose: () -> Unit) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(file.path) {
        val b = vm.backend
        val bmp = withContext(Dispatchers.IO) { runCatching { Thumbnails.decodeSampled(b, file.path, 2048) }.getOrNull() }
        bitmap = bmp?.asImageBitmap()
        failed = bmp == null
    }
    BackHandler(onBack = onClose)

    Box(
        Modifier
            .fillMaxSize()
            .background(ViewerBg),
    ) {
        val bmp = bitmap
        when {
            bmp != null -> Image(
                bitmap = bmp,
                contentDescription = file.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        })
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 8f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
            )
            failed -> StateMessage(Icons.Filled.BrokenImage, "تصویر باز نشد", null)
            else -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = ViewerFg)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(ViewerBg.copy(alpha = 0.85f), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = ViewerFg) }
            Text(
                file.name,
                color = ViewerFg,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { vm.share(listOf(file)) }) { Icon(Icons.Filled.Share, "اشتراک‌گذاری", tint = ViewerFg) }
            IconButton(onClick = { vm.openExternal(file) }) { Icon(Icons.AutoMirrored.Filled.OpenInNew, "باز کردن با", tint = ViewerFg) }
        }
    }
}
