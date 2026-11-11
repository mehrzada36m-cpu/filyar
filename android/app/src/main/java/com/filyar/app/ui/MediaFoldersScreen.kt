package com.filyar.app.ui

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

private data class MediaFolder(
    val path: String,
    val count: Int,
    val size: Long,
    val newest: RemoteFile,
)

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

    val folders = remember(m.files, kind, m.includeHidden) {
        m.files
            .filter { it.extension in kind.extensions }
            .filter { m.includeHidden || isUserMedia(it.parent, root) }
            .groupBy { it.parent }
            .map { (dir, list) ->
                MediaFolder(dir, list.size, list.sumOf { it.size }, list.maxByOrNull { it.lastModified } ?: list.first())
            }
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
                    IconButton(onClick = vm::scanMedia, enabled = !m.scanning) { Icon(Icons.Filled.Refresh, "اسکن دوباره") }
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
            when {
                m.error != null && !m.scanning && folders.isEmpty() -> StateMessage(kindIcon(kind), "جستجو ممکن نشد", m.error)
                folders.isEmpty() && !m.scanning -> StateMessage(kindIcon(kind), "پوشه‌ای پیدا نشد", "هیچ پوشه‌ای با این نوع فایل در حافظه نیست.")
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    items(folders, key = { it.path }) { f ->
                        FolderRow(f, vm, root) { vm.showMediaFolder(f.path) }
                    }
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
private fun FolderRow(f: MediaFolder, vm: BrowserViewModel, root: String, onClick: () -> Unit) {
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
