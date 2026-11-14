package com.filyar.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.FolderOpen
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filyar.app.RemoteFile
import com.filyar.app.core.FileBackend
import com.filyar.app.core.faNum
import com.filyar.app.core.formatDate
import com.filyar.app.core.formatSize

private const val MB = 1024L * 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanScreen(vm: BrowserViewModel, snackbar: SnackbarHostState) {
    val c by vm.clean.collectAsStateWithLifecycle()
    // Keep the last mode while the screen fades out, so it does not go blank first.
    val lastMode = remember { arrayOfNulls<CleanMode>(1) }
    c.mode?.let { lastMode[0] = it }
    val mode = c.mode ?: lastMode[0] ?: return
    BackHandler(enabled = c.mode != null) { vm.closeClean() }

    val listState = vm.cleanListState(mode)
    var confirm by remember { mutableStateOf<List<RemoteFile>?>(null) }
    var infoDialog by remember { mutableStateOf<Pair<RemoteFile, ApkInfo?>?>(null) }
    var minBytes by remember { mutableLongStateOf(BrowserViewModel.DEFAULT_LARGE_MIN) }
    var custom by remember { mutableStateOf("") }
    val backend = vm.backend

    if (mode == CleanMode.Large) {
        // A smaller limit than the one already scanned needs a fresh scan.
        LaunchedEffect(minBytes, c.scanning, c.scannedMin, c.error) {
            if (!c.scanning && c.error == null) vm.ensureLargeScan(minBytes)
        }
    }

    val shown: List<RemoteFile> = if (mode == CleanMode.Large) c.large.filter { it.size >= minBytes } else emptyList()
    val selected = vm.cleanSelectedFiles()
    val selectedSize = selected.sumOf { it.size }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
        Column {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(onClick = vm::closeClean) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } },
                title = {
                    Column {
                        Text(
                            when (mode) {
                                CleanMode.Large -> "فایل‌های بزرگ"
                                CleanMode.Duplicates -> "فایل‌های تکراری"
                                CleanMode.Apks -> "فایل‌های برنامه‌ها"
                                CleanMode.Leftovers -> "پوشه‌های باقی‌مانده"
                                CleanMode.Recent -> "فایل‌های اخیر"
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        val sub = when {
                            c.scanning -> "در حال جستجو…"
                            mode == CleanMode.Large -> "${faNum(shown.size)} فایل • ${formatSize(shown.sumOf { it.size })}"
                            mode == CleanMode.Apks -> "${faNum(c.apks.size)} فایل • ${formatSize(c.apks.sumOf { it.size })}"
                            mode == CleanMode.Leftovers -> "${faNum(c.leftovers.size)} پوشه • ${formatSize(c.leftovers.sumOf { it.size })} قابل آزاد شدن"
                            mode == CleanMode.Recent -> "${faNum(c.recent.size)} فایل"
                            else -> "${faNum(c.groups.size)} گروه • ${formatSize(c.groups.sumOf { g -> g[0].size * (g.size - 1) })} قابل آزاد شدن"
                        }
                        Text(sub, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    val selectable = when (mode) {
                        CleanMode.Apks -> c.apks
                        CleanMode.Leftovers -> c.leftovers
                        else -> shown
                    }
                    if (mode != CleanMode.Duplicates && mode != CleanMode.Recent && selectable.isNotEmpty()) {
                        IconButton(onClick = { vm.cleanSelect(selectable.map { it.path }, true) }) { Icon(Icons.Filled.SelectAll, "انتخاب همه") }
                    }
                    IconButton(
                        onClick = {
                            when (mode) {
                                CleanMode.Large -> vm.scanLarge(minBytes)
                                CleanMode.Duplicates -> vm.scanDuplicates()
                                CleanMode.Apks -> vm.scanApks()
                                CleanMode.Leftovers -> vm.scanLeftovers()
                                CleanMode.Recent -> vm.scanRecent()
                            }
                        },
                        enabled = !c.scanning,
                    ) { Icon(Icons.Filled.Refresh, "اسکن دوباره") }
                },
            )
                    MiniPlayerBar(vm)
        }
    },
        bottomBar = {
            if (selected.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${faNum(selected.size)} فایل انتخاب شده", style = MaterialTheme.typography.titleSmall)
                            Text(formatSize(selectedSize), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton2("لغو انتخاب") { vm.cleanClearSelection() }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { confirm = selected },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) { Text("حذف", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (c.scanning) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    c.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }

            if (mode == CleanMode.Large) {
                SizeFilter(
                    minBytes = minBytes,
                    custom = custom,
                    onCustom = { text ->
                        custom = text.filter { it.isDigit() }.take(6)
                        custom.toLongOrNull()?.takeIf { it > 0 }?.let { minBytes = it * MB }
                    },
                    onChip = {
                        minBytes = it
                        custom = ""
                    },
                )
            } else if (mode == CleanMode.Apks) {
                AppsCapsule(selected = 0) { i ->
                    if (i != 0) {
                        vm.setAppsTab(i)
                        vm.openApps()
                        vm.closeClean()
                    }
                }
                Text(
                    "فایل‌های apk، apks، xapk و apkm در همه‌ی حافظه. برای دیدن آن‌ها داخل Android/data باید Shizuku روشن باشد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            } else if (mode == CleanMode.Leftovers) {
                Text(
                    "پوشه‌هایی در Android/data و Android/obb که برنامه‌شان دیگر نصب نیست. حذف آن‌ها قابل برگشت نیست.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            } else if (mode == CleanMode.Duplicates && c.groups.isNotEmpty()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = vm::cleanSelectExtraCopies) {
                        Icon(Icons.Filled.DoneAll, null)
                        Spacer(Modifier.width(6.dp))
                        Text("انتخاب نسخه‌های اضافه")
                    }
                }
                Text(
                    "قدیمی‌ترین نسخه‌ی هر گروه نگه داشته می‌شود. فایل‌های Android/ بررسی نمی‌شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                )
            }

            when {
                c.error != null && !c.scanning -> StateMessage(Icons.Filled.AutoDelete, "جستجو ممکن نشد", c.error)
                mode == CleanMode.Large -> if (shown.isEmpty() && !c.scanning) {
                    StateMessage(Icons.Filled.SdStorage, "فایل بزرگی پیدا نشد", "حد حجم را کمتر کنید.")
                } else {
                    LazyColumn(state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)) {
                        items(shown, key = { it.path }) { f ->
                            CleanRow(f, backend, vm.rootPath, f.path in c.selection, null) { vm.cleanToggle(f.path) }
                        }
                    }
                }
                mode == CleanMode.Apks -> if (c.apks.isEmpty() && !c.scanning) {
                    StateMessage(Icons.Filled.Android, "فایل برنامه‌ای پیدا نشد", "هیچ APK یا بسته‌ی نصبی در حافظه نیست.")
                } else {
                    LazyColumn(state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)) {
                        items(c.apks, key = { it.path }) { f ->
                            var info by remember(f.path, f.lastModified) { mutableStateOf<ApkInfo?>(null) }
                            LaunchedEffect(f.path, f.lastModified) { info = vm.apkInfo(f) }
                            val selecting = c.selection.isNotEmpty()
                            ApkRow(
                                f, backend, info,
                                checked = f.path in c.selection,
                                selectionMode = selecting,
                                onClick = { if (selecting) vm.cleanToggle(f.path) else vm.open(f) },
                                onLongClick = { vm.cleanToggle(f.path) },
                                onInfo = { infoDialog = f to info },
                                onInstall = if (f.extension == "apk") ({ vm.installApk(f) }) else null,
                                onFolder = { vm.showInFolder(f.path) },
                            )
                        }
                    }
                }
                mode == CleanMode.Leftovers -> if (c.leftovers.isEmpty() && !c.scanning) {
                    StateMessage(Icons.Filled.FolderOff, "پوشه‌ی باقی‌مانده‌ای نیست", "همه‌ی پوشه‌های Android/data و Android/obb مال برنامه‌های نصب‌شده‌اند.")
                } else {
                    LazyColumn(state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)) {
                        items(c.leftovers, key = { it.path }) { f ->
                            CleanRow(f, backend, vm.rootPath, f.path in c.selection, null, showLocation = true) { vm.cleanToggle(f.path) }
                        }
                    }
                }
                mode == CleanMode.Recent -> if (c.recent.isEmpty() && !c.scanning) {
                    StateMessage(Icons.Filled.History, "فایل اخیری پیدا نشد", "فایل‌هایی که تازه ساخته یا تغییر کرده‌اند اینجا نشان داده می‌شوند.")
                } else {
                    LazyColumn(state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)) {
                        items(c.recent, key = { it.path }) { f ->
                            RecentRow(f, backend, vm.rootPath, onOpen = { vm.open(f) }, onFolder = { vm.showInFolder(f.path) })
                        }
                    }
                }
                else -> if (c.groups.isEmpty() && !c.scanning) {
                    StateMessage(Icons.Filled.FileCopy, "فایل تکراری پیدا نشد", "فایل‌های هم‌اندازه و هم‌محتوا اینجا گروه‌بندی می‌شوند.")
                } else {
                    LazyColumn(state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)) {
                        c.groups.forEachIndexed { i, g ->
                            item(key = "h$i") {
                                Column {
                                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp))
                                    Text(
                                        "گروه ${faNum(i + 1)} • ${faNum(g.size)} نسخه • هر کدام ${formatSize(g[0].size)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                    )
                                }
                            }
                            items(g.size, key = { j -> "$i:${g[j].path}" }) { j ->
                                val f = g[j]
                                CleanRow(f, backend, vm.rootPath, f.path in c.selection, if (j == 0) "قدیمی‌ترین • پیشنهاد نگه‌داشتن" else null) {
                                    vm.cleanToggle(f.path)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    infoDialog?.let { (file, inf) ->
        ApkInfoDialog(file, inf, vm.rootPath) { infoDialog = null }
    }

    confirm?.let { items ->
        val allOfGroup = mode == CleanMode.Duplicates && c.groups.any { g -> g.all { it.path in c.selection } }
        ConfirmDialog(
            title = "حذف نهایی؟",
            body = buildString {
                append("${faNum(items.size)} فایل (${formatSize(items.sumOf { it.size })}) برای همیشه حذف می‌شود و قابل بازگشت نیست.")
                if (allOfGroup) append("\n\nدر بعضی گروه‌ها همه‌ی نسخه‌ها انتخاب شده‌اند و هیچ کپی‌ای باقی نمی‌ماند.")
            },
            confirm = "حذف",
            danger = true,
            onDismiss = { confirm = null },
        ) {
            confirm = null
            vm.cleanDelete(items)
        }
    }
}

@Composable
private fun TextButton2(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(label) }
}

@Composable
private fun SizeFilter(minBytes: Long, custom: String, onCustom: (String) -> Unit, onChip: (Long) -> Unit) {
    val options = listOf(10 * MB, 50 * MB, 100 * MB, 500 * MB, 1024 * MB)
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(
            "فایل‌های بزرگ‌تر از",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { o ->
                FilterChip(
                    selected = custom.isEmpty() && minBytes == o,
                    onClick = { onChip(o) },
                    label = { Text(formatSize(o)) },
                )
            }
        }
        OutlinedTextField(
            value = custom,
            onValueChange = onCustom,
            label = { Text("حجم دلخواه (مگابایت)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun CleanRow(
    f: RemoteFile,
    backend: FileBackend,
    root: String,
    checked: Boolean,
    tag: String?,
    showLocation: Boolean = false,
    onOpenFolder: (() -> Unit)? = null,
    onToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        FileIcon(f, backend, 46.dp, selected = false)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(f.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatSize(f.size)} • ${formatDate(f.lastModified)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showLocation) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "در: " + f.parent.removePrefix(root).trimStart('/').ifEmpty { "حافظه داخلی" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text(
                    f.parent.removePrefix(root).ifEmpty { "/" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (tag != null) Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        if (onOpenFolder != null) {
            IconButton(onClick = onOpenFolder) { Icon(Icons.Filled.FolderOpen, "نمایش در پوشه") }
        }
    }
}

/** How this APK compares with the copy of the app that is installed. */
@Composable
private fun apkStatus(info: ApkInfo): Pair<String, androidx.compose.ui.graphics.Color> {
    val scheme = MaterialTheme.colorScheme
    val installed = info.installedCode
    return when {
        installed == null -> "نصب نیست" to scheme.tertiary
        info.versionCode > installed -> "جدیدتر از نصب‌شده (${info.installedName ?: "؟"})" to scheme.primary
        info.versionCode == installed -> "همین نسخه نصب است" to scheme.onSurfaceVariant
        else -> "قدیمی‌تر از نصب‌شده (${info.installedName ?: "؟"})" to scheme.error
    }
}

@Composable
fun RecentRow(f: RemoteFile, backend: FileBackend, root: String, onOpen: () -> Unit, onFolder: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileIcon(f, backend, 46.dp, selected = false)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(f.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatDate(f.lastModified)} • ${formatSize(f.size)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                f.parent.removePrefix(root).trimStart('/').ifEmpty { "حافظه داخلی" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onFolder) { Icon(Icons.Filled.FolderOpen, "نمایش در پوشه") }
    }
}

/**
 * One installable app file. It shows only the app's name and version; everything else sits behind the
 * info button. The checkbox only appears once a long press has started a selection.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ApkRow(
    f: RemoteFile,
    backend: FileBackend,
    info: ApkInfo?,
    checked: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInfo: () -> Unit,
    onInstall: (() -> Unit)?,
    onFolder: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Checkbox(checked = checked, onCheckedChange = { onClick() })
        }
        Box {
            FileIcon(f, backend, 46.dp, selected = false)
            if (info?.installedCode != null) {
                Text(
                    "نصب شده",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
                    color = scheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(scheme.primary)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(info?.label ?: f.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (info != null) "نسخه ${info.versionName.ifEmpty { "؟" }}" else f.extension.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        // The three actions share one capsule so the row stays slim.
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(scheme.surfaceContainerHigh),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CapsuleAction(Icons.Filled.Info, "اطلاعات برنامه", onInfo)
            if (onInstall != null) CapsuleAction(Icons.Filled.SystemUpdate, "نصب", onInstall)
            CapsuleAction(Icons.Filled.FolderOpen, "نمایش در پوشه", onFolder)
        }
    }
}

@Composable
private fun CapsuleAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Icon(icon, description, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ApkInfoDialog(f: RemoteFile, info: ApkInfo?, root: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(info?.label ?: f.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoLine("نام فایل", f.name)
                if (info != null) {
                    InfoLine("نام بسته", info.pkg)
                    InfoLine("نسخه", "${info.versionName.ifEmpty { "؟" }} (${faNum(info.versionCode)})")
                    val (status, color) = apkStatus(info)
                    InfoLine("وضعیت", status, color)
                }
                InfoLine("حجم", formatSize(f.size))
                InfoLine("آخرین ویرایش", formatDate(f.lastModified))
                InfoLine("پوشه", f.parent.removePrefix(root).trimStart('/').ifEmpty { "حافظه داخلی" })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } },
    )
}

@Composable
private fun InfoLine(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
