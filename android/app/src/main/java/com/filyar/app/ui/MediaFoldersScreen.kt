package com.filyar.app.ui

import kotlin.math.roundToInt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Album
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filyar.app.RemoteFile
import com.filyar.app.core.faNum
import com.filyar.app.core.formatSize

/** Android/data and Android/obb hold app caches, not the user's own media. */
private fun isUserMedia(dir: String, root: String): Boolean {
    val rel = dir.removePrefix(root).trimStart('/')
    if (rel == "Android/data" || rel.startsWith("Android/data/")) return false
    if (rel == "Android/obb" || rel.startsWith("Android/obb/")) return false
    return rel.split('/').none { it.startsWith(".") }
}

/** Every folder that holds images, videos or music, with how many files each has. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaFoldersScreen(vm: BrowserViewModel, snackbar: SnackbarHostState) {
    val m by vm.media.collectAsStateWithLifecycle()
    val kind = m.kind ?: return
    BackHandler { vm.closeMedia() }
    val root = vm.rootPath
    val stats by vm.stats.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refreshStats() }

    val folders = remember(m.folders, kind, m.includeHidden) {
        m.folders[kind].orEmpty()
            .filter { m.includeHidden || isUserMedia(it.path, root) }
            .sortedByDescending { it.newest.lastModified }
    }
    val total = folders.sumOf { it.count }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(onClick = vm::closeMedia) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } },
                title = {
                    Column {
                        Text(kind.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (m.scanning) "در حال جستجو…" else "${faNum(folders.size)} پوشه • ${faNum(total)} فایل",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = vm::rescanMedia, enabled = !m.scanning) { Icon(Icons.Filled.Refresh, "اسکن دوباره") }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (m.scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                FilterChip(
                    selected = m.includeHidden,
                    onClick = { vm.setMediaIncludeHidden(!m.includeHidden) },
                    label = { Text("شامل Android/data و پوشه‌های مخفی") },
                )
            }
            LazyColumn(state = vm.mediaListState(kind), contentPadding = PaddingValues(bottom = 32.dp)) {
                item(key = "header") {
                    if (kind == MediaKind.Audio) {
                        AudioCategories(stats, vm)
                    } else {
                        UsageHeader(kind, stats, storage) { vm.openAllMedia(kind) }
                    }
                }
                item(key = "foldersTitle") {
                    Text(
                        "پوشه‌ها",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                if (m.error != null && !m.scanning && folders.isEmpty()) {
                    item(key = "error") { EmptyNote(m.error ?: "") }
                } else if (folders.isEmpty() && !m.scanning) {
                    item(key = "empty") { EmptyNote("هیچ پوشه‌ای با این نوع فایل در حافظه نیست.") }
                }
                items(folders, key = { it.path }) { f ->
                    FolderRow(f, vm, root) { vm.showMediaFolder(f.path) }
                }
            }
        }
    }
}

private fun kindIcon(kind: MediaKind) = when (kind) {
    MediaKind.Images -> Icons.Filled.Image
    MediaKind.Videos -> Icons.Filled.Movie
    MediaKind.Audio -> Icons.Filled.MusicNote
}

@Composable
private fun FolderRow(f: MediaFolderInfo, vm: BrowserViewModel, root: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The newest file stands in as the folder's preview.
        FileIcon(f.newest, vm.backend, 56.dp, selected = false)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(f.path.substringAfterLast('/'), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${faNum(f.count)} فایل • ${formatSize(f.size)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                f.path.removePrefix(root).trimStart('/').ifEmpty { "حافظه داخلی" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
    )
}

private fun percent(part: Long, total: Long): String {
    if (total <= 0L || part <= 0L) return "${faNum(0)}٪"
    val p = part * 100.0 / total
    return if (p < 1.0) "<${faNum(1)}٪" else "${faNum(p.roundToInt())}٪"
}

/** How much of the storage this kind of file takes, as a donut with a legend, plus a "view all" button. */
@Composable
private fun UsageHeader(kind: MediaKind, stats: MediaStats, storage: LongArray?, onViewAll: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val total = storage?.getOrNull(0) ?: 0L
    val free = storage?.getOrNull(1) ?: 0L
    val kindBytes = if (kind == MediaKind.Images) stats.imageBytes else stats.videoBytes
    if (total <= 0L) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            TextButton(onClick = onViewAll) { Text("مشاهده‌ی همه‌ی ${kind.title}") }
        }
        return
    }
    val other = (total - free - kindBytes).coerceAtLeast(0L)
    val parts = listOf(kindBytes, other, free)
    val colors = listOf(scheme.primary, scheme.outline, scheme.surfaceVariant)
    val labels = listOf(
        "${kind.title} (${formatSize(kindBytes)})",
        "سایر فایل‌ها (${formatSize(other)})",
        "فضای خالی (${formatSize(free)})",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(92.dp)) {
            val stroke = 16.dp.toPx()
            var start = -90f
            parts.forEachIndexed { i, v ->
                val sweep = 360f * v / total
                drawArc(
                    color = colors[i],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke),
                )
                start += sweep
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            parts.forEachIndexed { i, v ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(colors[i]))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${percent(v, total)}  ${labels[i]}",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        TextButton(onClick = onViewAll) { Text("مشاهده‌ی همه") }
    }
}

private class AudioCat(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val count: Int,
    val onClick: () -> Unit,
)

/** The music screen's shortcuts: artists, albums, all tracks, and the alarm/ringtone/notification/podcast lists. */
@Composable
private fun AudioCategories(stats: MediaStats, vm: BrowserViewModel) {
    val cats = listOf(
        AudioCat("هنرمندان", Icons.Filled.Mic, Color(0xFF3FA34D), stats.artists) { vm.openAudioGroups(true) },
        AudioCat("آلبوم‌ها", Icons.Filled.Album, Color(0xFF14A38B), stats.albums) { vm.openAudioGroups(false) },
        AudioCat("همه‌ی آهنگ‌ها", Icons.Filled.PlayArrow, Color(0xFF1EA7D6), stats.tracks) { vm.openAudioList(AudioList.Tracks) },
        AudioCat("زنگ هشدار", Icons.Filled.Alarm, Color(0xFFE91E63), stats.alarms) { vm.openAudioList(AudioList.Alarms) },
        AudioCat("زنگ تماس", Icons.Filled.Call, Color(0xFF6C4DD3), stats.ringtones) { vm.openAudioList(AudioList.Ringtones) },
        AudioCat("اعلان‌ها", Icons.Filled.Notifications, Color(0xFF9C27B0), stats.notifications) { vm.openAudioList(AudioList.Notifications) },
        AudioCat("پادکست‌ها", Icons.Filled.Headset, Color(0xFF3F51B5), stats.podcasts) { vm.openAudioList(AudioList.Podcasts) },
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
        cats.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { c ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = c.onClick)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(c.color), contentAlignment = Alignment.Center) {
                            Icon(c.icon, null, tint = Color.White)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(c.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (c.count > 0) faNum(c.count) else "هیچ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 1.dp),
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
