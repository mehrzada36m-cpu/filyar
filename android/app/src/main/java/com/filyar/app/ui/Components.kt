package com.filyar.app.ui

import com.filyar.app.ui.theme.AccentColors
import androidx.compose.ui.graphics.lerp
import com.filyar.app.core.FolderModel
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import com.filyar.app.core.HomeStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filyar.app.RemoteFile
import com.filyar.app.core.FileBackend
import com.filyar.app.core.FolderStyle
import com.filyar.app.core.ItemSize
import com.filyar.app.core.ViewPrefs
import com.filyar.app.ui.theme.Filyar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.filyar.app.core.faNum
import com.filyar.app.core.formatDateShort
import com.filyar.app.core.formatSize

fun metaLine(f: RemoteFile): String = if (f.isDirectory) {
    val c = if (f.childCount < 0) "بدون دسترسی" else "${faNum(f.childCount)} مورد"
    "$c  •  ${formatDateShort(f.lastModified)}"
} else {
    "${formatSize(f.size)}  •  ${formatDateShort(f.lastModified)}"
}

/** Folder looks and display settings, provided once at the top of the screen. */
val LocalFolderStyles = staticCompositionLocalOf<Map<String, FolderStyle>> { emptyMap() }
val LocalViewPrefs = staticCompositionLocalOf { ViewPrefs() }

val ItemSize.rowIcon: Dp
    get() = when (this) {
        ItemSize.Compact -> 38.dp
        ItemSize.Standard -> 46.dp
        ItemSize.Large -> 58.dp
    }

val ItemSize.gridCell: Dp
    get() = when (this) {
        ItemSize.Compact -> 86.dp
        ItemSize.Standard -> 108.dp
        ItemSize.Large -> 140.dp
    }

val ItemSize.gridIcon: Dp
    get() = when (this) {
        ItemSize.Compact -> 46.dp
        ItemSize.Standard -> 64.dp
        ItemSize.Large -> 88.dp
    }

/** The shape behind a folder icon, chosen in settings. */
@Composable
fun FolderFrame(model: FolderModel, tint: Color, size: Dp, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = if (model == FolderModel.Circle) CircleShape else RoundedCornerShape(size * 0.3f)
    val base = modifier.size(size)
    val framed = when (model) {
        FolderModel.Plain -> base
        FolderModel.Outline -> base.clip(shape).border(1.5.dp, tint.copy(alpha = 0.7f), shape)
        else -> base.clip(shape).background(tint.copy(alpha = 0.14f))
    }
    Box(framed, contentAlignment = Alignment.Center, content = content)
}

/**
 * A folder's icon: the user's choice, else one matched to its name. When the folder belongs to an
 * installed app, that app's icon is added as a small badge in the corner so the folder still reads
 * as a folder.
 */
@Composable
fun FolderGlyph(path: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    val custom = LocalFolderStyles.current[path]
    val prefs = LocalViewPrefs.current
    val appIcons = prefs.appFolderIcons
    val context = LocalContext.current
    val customIcon = custom?.icon?.let { FolderIcons.byKey(it) }
    val builtin = FolderIcons.builtinFor(name)
    val tryApp = customIcon == null && builtin == null && appIcons
    val px = with(LocalDensity.current) { (size * 0.42f).roundToPx() }
    var app by remember(name, px, tryApp) {
        mutableStateOf(if (tryApp) AppIcons.cached(name, px)?.asImageBitmap() else null)
    }
    if (tryApp && app == null) {
        LaunchedEffect(name, px) {
            app = withContext(Dispatchers.IO) { AppIcons.folderIcon(context, name, px) }?.asImageBitmap()
        }
    }
    val tint = custom?.color?.let { FolderIcons.colorAt(it) } ?: Filyar.tokens.folder
    val a = app
    val icon = customIcon ?: builtin
        ?: (if (a == null && appIcons) FolderIcons.appFallbackFor(name) else null)
        ?: Icons.Filled.Folder
    val model = custom?.model ?: prefs.folderModel
    val plain = model == FolderModel.Plain
    val glass = prefs.homeStyle == HomeStyle.Glass
    Box(modifier.size(size)) {
        if (glass) {
            val variant = path.hashCode() and 1
            GlassFolderIcon(size, variant)
            val hasOwnIcon = customIcon != null || builtin != null || (a == null && appIcons && FolderIcons.appFallbackFor(name) != null)
            if (hasOwnIcon) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (variant == 0) Color.White else Color(0xFF0A4F66),
                    modifier = Modifier.align(Alignment.Center).offset(y = size * 0.07f).size(size * 0.34f),
                )
            }
        } else {
            FolderFrame(model, tint, size) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * (if (plain) 0.92f else 0.56f)))
            }
        }
        if (a != null) {
            val badge = RoundedCornerShape(size * 0.12f)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(size * 0.05f, size * 0.05f)
                    .size(size * 0.44f)
                    .clip(badge)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(size * 0.03f),
            ) {
                Image(a, contentDescription = null, modifier = Modifier.fillMaxSize().clip(badge))
            }
        }
    }
}

@Composable
private fun FileGlyph(f: RemoteFile, backend: FileBackend, size: Dp) {
    val kind = kindOf(f)
    val tint = kind.tint
    val context = LocalContext.current
    val previews = LocalViewPrefs.current.previews
    val px = Thumbnails.bucket(with(LocalDensity.current) { (size.roundToPx() * 1.5f).toInt() })
    var thumb by remember(f.path, f.lastModified, previews, px) {
        mutableStateOf(if (previews) Thumbnails.cached(f, px)?.asImageBitmap() else null)
    }
    if (previews && thumb == null && Thumbnails.supports(f)) {
        LaunchedEffect(f.path, f.lastModified, px) {
            thumb = Thumbnails.load(context, backend, f, px)?.asImageBitmap()
        }
    }
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        val t = thumb
        if (t != null) {
            Image(t, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (kind == FileKind.Video) {
                Box(
                    Modifier
                        .size(size * 0.4f)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(size * 0.3f)) }
            }
        } else {
            Icon(kind.icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f))
        }
    }
}

@Composable
fun FileIcon(f: RemoteFile, backend: FileBackend, size: Dp, selected: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.3f)
    // Folders are not clipped, so the app badge on their corner shows in full.
    Box(modifier.size(size)) {
        if (f.isDirectory) {
            FolderGlyph(f.path, f.name, size)
        } else {
            Box(Modifier.fillMaxSize().clip(shape)) { FileGlyph(f, backend, size) }
        }
        AnimatedVisibility(selected, enter = scaleIn(), exit = scaleOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(size * 0.5f))
            }
        }
    }
}

/** The user's size multiplier for this item: folders and files can be sized separately. */
@Composable
fun iconScaleFor(f: RemoteFile): Float {
    val p = LocalViewPrefs.current
    return if (f.isDirectory) p.folderScale else p.fileScale
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    f: RemoteFile,
    backend: FileBackend,
    selected: Boolean,
    selecting: Boolean,
    subtitle: String = metaLine(f),
    iconSize: Dp = LocalViewPrefs.current.itemSize.rowIcon,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    val shownIcon = iconSize * iconScaleFor(f)
    val compact = shownIcon < 42.dp
    val gap = LocalViewPrefs.current.itemGap
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp * gap, vertical = (if (compact) 1.dp else 2.dp) * gap)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 10.dp, end = 2.dp, top = (if (compact) 4.dp else 8.dp) * gap, bottom = (if (compact) 4.dp else 8.dp) * gap)
            .alpha(if (f.isHidden) 0.6f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileIcon(f, backend, shownIcon, selected)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                f.name,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!selecting) {
            IconButton(onClick = onMore) {
                Icon(Icons.Filled.MoreVert, contentDescription = "گزینه‌ها", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    f: RemoteFile,
    backend: FileBackend,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val prefs = LocalViewPrefs.current
    val iconSize = prefs.itemSize.gridIcon * iconScaleFor(f)
    val scheme = MaterialTheme.colorScheme
    val card = when {
        !prefs.gridCards -> Color.Transparent
        else -> AccentColors.getOrNull(prefs.cardColor)?.let { lerp(scheme.surface, it, 0.16f) } ?: scheme.surfaceContainerLow
    }
    val bg = if (selected) scheme.primaryContainer.copy(alpha = 0.6f) else card
    Column(
        Modifier
            .padding(4.dp * prefs.itemGap)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp * prefs.itemGap)
            .alpha(if (f.isHidden) 0.6f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FileIcon(f, backend, iconSize, selected)
        Spacer(Modifier.height(8.dp * prefs.itemGap))
        Text(
            f.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (f.isDirectory) "${faNum(f.childCount.coerceAtLeast(0))} مورد" else formatSize(f.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
fun Breadcrumbs(path: String, rootPath: String, onNavigate: (String?) -> Unit) {
    val segments = remember(path) {
        val list = mutableListOf<Pair<String, String>>()
        if (path.startsWith(rootPath)) {
            list.add("حافظه داخلی" to rootPath)
            var acc = rootPath
            path.removePrefix(rootPath).split('/').filter { it.isNotEmpty() }.forEach {
                acc = "$acc/$it"
                list.add(it to acc)
            }
        } else {
            var acc = ""
            path.split('/').filter { it.isNotEmpty() }.forEach {
                acc = "$acc/$it"
                list.add(it to acc)
            }
        }
        list
    }
    val state = rememberLazyListState()
    LaunchedEffect(path) { if (segments.isNotEmpty()) state.animateScrollToItem(segments.lastIndex) }
    LazyRow(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
    ) {
        itemsIndexed(segments) { index, (label, target) ->
            val last = index == segments.lastIndex
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = { onNavigate(target) },
                    shape = CircleShape,
                    color = if (last) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (index == 0 && path.startsWith(rootPath)) {
                            Icon(Icons.Filled.PhoneAndroid, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (last) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                if (!last) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
fun StateMessage(
    icon: ImageVector,
    title: String,
    body: String?,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (action != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}
