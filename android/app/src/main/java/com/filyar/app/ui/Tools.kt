package com.filyar.app.ui

import android.net.Uri
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Slider
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import android.media.MediaPlayer
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.filyar.app.RemoteFile
import com.filyar.app.core.faNum
import com.filyar.app.core.formatSize
import java.io.File

/** Built-in player for video and audio files the app can read directly. */
@Composable
fun PlayerScreen(file: RemoteFile, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val isAudio = kindOf(file) == FileKind.Audio
    var failed by remember(file.path) { mutableStateOf(false) }
    val videoRef = remember { arrayOfNulls<VideoView>(1) }
    val controllerRef = remember { arrayOfNulls<MediaController>(1) }
    DisposableEffect(file.path) {
        onDispose {
            runCatching { controllerRef[0]?.hide() }
            runCatching { videoRef[0]?.stopPlayback() }
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val vv = VideoView(ctx)
                val mc = MediaController(ctx)
                mc.setAnchorView(vv)
                vv.setMediaController(mc)
                vv.setOnPreparedListener {
                    vv.start()
                    if (isAudio) mc.show(0)
                }
                vv.setOnErrorListener { _, _, _ ->
                    failed = true
                    true
                }
                vv.setVideoURI(Uri.fromFile(File(file.path)))
                videoRef[0] = vv
                controllerRef[0] = mc
                val box = FrameLayout(ctx)
                box.addView(
                    vv,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER,
                    ),
                )
                box
            },
        )
        if (isAudio) {
            Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(96.dp))
                Spacer(Modifier.height(16.dp))
                Text(file.name, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        if (failed) {
            Text("پخش این فایل ممکن نشد", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = Color.White)
        }
    }
}

/** Shows what an APK is and whether it is new or an update, then hands it to the system installer. */
@Composable
fun ApkInstallDialog(file: RemoteFile, vm: BrowserViewModel) {
    var info by remember(file.path) { mutableStateOf<ApkInfo?>(null) }
    var loaded by remember(file.path) { mutableStateOf(false) }
    LaunchedEffect(file.path) {
        info = vm.apkInfo(file)
        loaded = true
    }
    AlertDialog(
        onDismissRequest = vm::dismissApkPrompt,
        title = { Text(info?.label ?: file.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val i = info
                if (i != null) {
                    Text(i.pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("نسخه ${i.versionName.ifEmpty { "؟" }} • ${formatSize(file.size)}")
                    val installed = i.installedCode
                    Text(
                        when {
                            installed == null -> "نصب‌نشده؛ نصب تازه"
                            i.versionCode > installed -> "به‌روزرسانی از نسخه‌ی ${i.installedName ?: "؟"}"
                            i.versionCode == installed -> "همین نسخه الان نصب است"
                            else -> "قدیمی‌تر از نسخه‌ی نصب‌شده (${i.installedName ?: "؟"})"
                        },
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (!loaded) {
                    Text("در حال خواندن…")
                } else {
                    Text("${formatSize(file.size)} • اطلاعات برنامه خوانده نشد")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                vm.dismissApkPrompt()
                vm.installApk(file)
            }) { Text("نصب") }
        },
        dismissButton = { TextButton(onClick = vm::dismissApkPrompt) { Text("بستن") } },
    )
}

/** Lists what is inside a ZIP and offers to extract it next to the file. */
@Composable
fun ZipPreviewDialog(file: RemoteFile, vm: BrowserViewModel) {
    var entries by remember(file.path) { mutableStateOf<List<Pair<String, Long>>?>(null) }
    LaunchedEffect(file.path) { entries = vm.zipEntries(file) }
    AlertDialog(
        onDismissRequest = vm::dismissZipPrompt,
        title = { Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            val list = entries
            if (list == null) {
                Text("در حال خواندن…")
            } else if (list.isEmpty()) {
                Text("فایل خالی است یا خوانده نشد.")
            } else {
                Column {
                    Text(
                        "${faNum(list.size)} مورد • ${formatSize(file.size)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(list.take(300)) { (name, size) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (size >= 0) {
                                    Text(
                                        formatSize(size),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                            }
                        }
                        if (list.size > 300) {
                            item { Text("و ${faNum(list.size - 300)} مورد دیگر…", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                vm.dismissZipPrompt()
                vm.extract(file)
            }) { Text("استخراج") }
        },
        dismissButton = { TextButton(onClick = vm::dismissZipPrompt) { Text("بستن") } },
    )
}

private fun clock(ms: Int): String {
    val total = ms / 1000
    return "${faNum(total / 60)}:${faNum(total % 60).padStart(2, '۰')}"
}

/** Soft shadow drawn only outside a rounded shape, so translucent surfaces stay clean. */
fun Modifier.softShadow(radius: Dp): Modifier = this.drawBehind {
    val r = radius.toPx()
    val hole = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }
    clipPath(hole, clipOp = ClipOp.Difference) {
        listOf(14.dp to 0.06f, 8.dp to 0.10f, 3.dp to 0.16f).forEach { (grow, alpha) ->
            val g = grow.toPx()
            drawRoundRect(
                color = Color.Black.copy(alpha = alpha),
                topLeft = Offset(-g, -g + 6.dp.toPx()),
                size = Size(size.width + 2 * g, size.height + 2 * g),
                cornerRadius = CornerRadius(r + g),
            )
        }
    }
}

/** The audio that plays in the mini player. It lives in the ViewModel, so music keeps playing across screens. */
class MiniAudio {
    var file by mutableStateOf<RemoteFile?>(null)
    var ready by mutableStateOf(false)
    var playing by mutableStateOf(false)
    var failed by mutableStateOf(false)
    var duration by mutableStateOf(0)
    var position by mutableStateOf(0)
    private var player: MediaPlayer? = null

    fun play(f: RemoteFile) {
        release()
        file = f
        ready = false
        playing = false
        failed = false
        duration = 0
        position = 0
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setDataSource(f.path)
            mp.setOnPreparedListener {
                duration = it.duration
                ready = true
                it.start()
                playing = true
            }
            mp.setOnCompletionListener {
                playing = false
                position = duration
            }
            mp.setOnErrorListener { _, _, _ ->
                failed = true
                playing = false
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            failed = true
        }
    }

    fun toggle() {
        val mp = player ?: return
        if (!ready) return
        if (playing) {
            mp.pause()
            playing = false
        } else {
            mp.start()
            playing = true
        }
    }

    fun seekFraction(fraction: Float) {
        val mp = player ?: return
        if (ready && duration > 0) {
            val target = (fraction.coerceIn(0f, 1f) * duration).toInt()
            mp.seekTo(target)
            position = target
        }
    }

    fun tick() {
        if (playing && ready) position = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
    }

    fun close() {
        release()
        file = null
    }

    private fun release() {
        runCatching { player?.release() }
        player = null
    }
}

/**
 * Slim, frosted audio capsule. Every screen places it right under its header, so it pushes the content down
 * instead of covering cards or folders. The × stops the music.
 */
@Composable
fun MiniPlayerBar(audio: MiniAudio) {
    val f = audio.file ?: return
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    LaunchedEffect(audio.playing, audio.ready, f.path) {
        while (audio.playing && audio.ready) {
            if (!dragging) audio.tick()
            delay(300)
        }
    }

    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(26.dp)
    val rim = Brush.linearGradient(
        listOf(Color.White.copy(alpha = if (dark) 0.30f else 1f), Color.White.copy(alpha = if (dark) 0.06f else 0.35f)),
    )
    CompositionLocalProvider(LocalContentColor provides scheme.onSurface) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(scheme.surfaceContainerHigh.copy(alpha = 0.92f), scheme.surfaceContainer.copy(alpha = 0.80f))))
                .border(1.dp, rim, shape)
                .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.MusicNote, null, tint = scheme.primary, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(10.dp))
                Text(
                    f.name.substringBeforeLast('.'),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (audio.failed) "پخش ممکن نشد" else clock(if (dragging) (dragValue * audio.duration).toInt() else audio.position),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
                IconButton(onClick = { audio.toggle() }, modifier = Modifier.size(36.dp)) {
                    Icon(if (audio.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (audio.playing) "توقف" else "پخش", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { audio.close() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, "بستن پخش‌کننده", modifier = Modifier.size(20.dp))
                }
            }
            // Thin seek line, left to right; tap or drag to jump.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                val shown = (if (dragging) dragValue else if (audio.duration > 0) audio.position.toFloat() / audio.duration else 0f).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .padding(horizontal = 6.dp)
                        .pointerInput(audio.ready, audio.duration) {
                            detectTapGestures { o -> audio.seekFraction(o.x / size.width) }
                        }
                        .pointerInput(audio.ready, audio.duration) {
                            detectHorizontalDragGestures(
                                onDragStart = { o ->
                                    dragging = true
                                    dragValue = (o.x / size.width).coerceIn(0f, 1f)
                                },
                                onDragEnd = {
                                    audio.seekFraction(dragValue)
                                    dragging = false
                                },
                                onDragCancel = { dragging = false },
                            ) { change, _ -> dragValue = (change.position.x / size.width).coerceIn(0f, 1f) }
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = 0.15f)))
                    Box(Modifier.fillMaxWidth(shown).height(3.dp).clip(CircleShape).background(scheme.primary))
                }
            }
        }
    }
}
