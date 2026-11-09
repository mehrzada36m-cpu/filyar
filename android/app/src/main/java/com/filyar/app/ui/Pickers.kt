package com.filyar.app.ui

import androidx.compose.material.icons.filled.Folder
import com.filyar.app.core.FolderModel
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filyar.app.RemoteFile
import com.filyar.app.core.FolderStyle
import com.filyar.app.ui.theme.Filyar

/** Lets the user walk through folders and pick one (used to add quick-access entries). */
@Composable
fun FolderPickerDialog(
    startPath: String,
    rootPath: String,
    list: suspend (String) -> Result<List<RemoteFile>>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    var path by remember { mutableStateOf(startPath) }
    var folders by remember { mutableStateOf<List<RemoteFile>?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(path) {
        folders = null
        failed = false
        list(path)
            .onSuccess { folders = it }
            .onFailure {
                failed = true
                folders = emptyList()
            }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب پوشه") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { path = path.substringBeforeLast('/').ifEmpty { rootPath } },
                        enabled = path != rootPath && path.startsWith(rootPath),
                    ) { Icon(Icons.Filled.ArrowUpward, "پوشه‌ی بالاتر") }
                    Text(
                        path.removePrefix(rootPath).ifEmpty { "حافظه داخلی" },
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(Modifier.heightIn(min = 120.dp, max = 320.dp).fillMaxWidth()) {
                    val shown = folders
                    when {
                        shown == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        failed -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.FolderOff, null, tint = MaterialTheme.colorScheme.outline)
                            Text(
                                "این پوشه باز نشد. برای Android/data باید Shizuku روشن باشد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        shown.isEmpty() -> Text(
                            "پوشه‌ای در اینجا نیست",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        else -> LazyColumn {
                            items(shown, key = { it.path }) { f ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { path = f.path }
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    FolderGlyph(f.path, f.name, 36.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(path) }) { Text("انتخاب این پوشه") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } },
    )
}

/** Choose an icon and colour for one folder. */
@Composable
fun FolderStyleDialog(
    name: String,
    current: FolderStyle?,
    onDismiss: () -> Unit,
    onSave: (FolderStyle?) -> Unit,
) {
    var icon by remember { mutableStateOf(current?.icon?.takeIf { it.isNotEmpty() }) }
    var color by remember { mutableIntStateOf(current?.color ?: -1) }
    var model by remember { mutableStateOf(current?.model) }
    val tint = FolderIcons.colorAt(color) ?: Filyar.tokens.folder
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("آیکون «$name»", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    val shown = icon?.let { FolderIcons.byKey(it) } ?: FolderIcons.builtinFor(name) ?: Icons.Filled.Folder
                    FolderFrame(model ?: LocalViewPrefs.current.folderModel, tint, 56.dp) {
                        Icon(shown, null, tint = tint, modifier = Modifier.size(if ((model ?: LocalViewPrefs.current.folderModel) == FolderModel.Plain) 52.dp else 32.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(52.dp),
                    modifier = Modifier.height(190.dp),
                ) {
                    items(FolderIcons.choices, key = { it.first }) { (key, vec) ->
                        val sel = icon == key
                        Box(
                            Modifier
                                .padding(3.dp)
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (sel) tint.copy(alpha = 0.2f) else Color.Transparent)
                                .then(if (sel) Modifier.border(2.dp, tint, RoundedCornerShape(14.dp)) else Modifier)
                                .clickable { icon = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(vec, null, tint = if (sel) tint else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("رنگ", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                val indices = listOf(-1) + FolderIcons.palette.indices.toList()
                indices.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                        row.forEach { i ->
                            val c = FolderIcons.colorAt(i) ?: Filyar.tokens.folder
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .then(
                                        if (i == color) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape) else Modifier,
                                    )
                                    .clickable { color = i },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (i == color) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("شکل", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf<Pair<FolderModel?, String>>(
                        null to "پیش‌فرض",
                        FolderModel.Tile to "کاشی",
                        FolderModel.Circle to "دایره",
                        FolderModel.Plain to "ساده",
                        FolderModel.Outline to "خطی",
                    ).forEach { (m, label) ->
                        FilterChip(selected = model == m, onClick = { model = m }, label = { Text(label) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(FolderStyle(icon ?: "", color, model)) }) { Text("ذخیره") } },
        dismissButton = {
            Row {
                if (current != null) TextButton(onClick = { onSave(null) }) { Text("حالت خودکار") }
                TextButton(onClick = onDismiss) { Text("لغو") }
            }
        },
    )
}
