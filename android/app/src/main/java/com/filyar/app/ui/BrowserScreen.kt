package com.filyar.app.ui

import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.foundation.clickable
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filyar.app.RemoteFile
import com.filyar.app.core.ShizukuManager
import com.filyar.app.core.ShizukuStatus
import com.filyar.app.core.SortBy
import com.filyar.app.core.LongPressAction
import com.filyar.app.core.faNum
import com.filyar.app.ui.theme.Filyar

private sealed interface Dlg {
    data object CreateMenu : Dlg
    data object NewFolder : Dlg
    data object NewFile : Dlg
    data object Guide : Dlg
    data object SelectionMore : Dlg
    data class ItemMenu(val f: RemoteFile) : Dlg
    data class Rename(val f: RemoteFile) : Dlg
    data class Delete(val items: List<RemoteFile>) : Dlg
    data class Props(val f: RemoteFile) : Dlg
    data class Compress(val items: List<RemoteFile>) : Dlg
    data class Extract(val f: RemoteFile) : Dlg
    data class Restore(val f: RemoteFile) : Dlg
    data object AddQuick : Dlg
    data class QuickStyle(val path: String) : Dlg
    data class FolderStyleDlg(val f: RemoteFile) : Dlg
}

const val SHIZUKU_DOWNLOAD_URL = "https://shizuku.rikka.app/download/"

fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

fun openShizukuApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(ShizukuManager.SHIZUKU_PACKAGE)
    if (intent != null) context.startActivity(intent) else openUrl(context, SHIZUKU_DOWNLOAD_URL)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(vm: BrowserViewModel, snackbar: SnackbarHostState, onOpenSettings: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val vp by vm.viewPrefs.collectAsStateWithLifecycle()
    val clip by vm.clip.collectAsStateWithLifecycle()
    val shizuku by ShizukuManager.status.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val localAccess by vm.localAccess.collectAsStateWithLifecycle()
    val quickAccess by vm.quickAccess.collectAsStateWithLifecycle()
    val folderStyles by vm.folderStyles.collectAsStateWithLifecycle()
    val folderLayouts by vm.folderLayouts.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val homeBg by vm.homeBg.collectAsStateWithLifecycle()
    LaunchedEffect(st.path == null) { if (st.path == null) vm.refreshStats() }
    val curPath = st.path
    val gridHere = curPath?.let { folderLayouts[it] } ?: vp.grid
    val layoutOverridden = curPath != null && folderLayouts.containsKey(curPath)
    val context = LocalContext.current
    val backend = vm.backend

    var dlg by remember { mutableStateOf<Dlg?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.onResume() }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            val media = arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
            if (media.any { context.checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                permLauncher.launch(media)
            }
        }
    }
    val grantLocal: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
                )
            } catch (_: Exception) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            permLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }
    val shizukuAction: () -> Unit = {
        when (shizuku) {
            ShizukuStatus.NoPermission -> ShizukuManager.requestPermission()
            ShizukuStatus.NotRunning -> openShizukuApp(context)
            ShizukuStatus.NotInstalled -> openUrl(context, SHIZUKU_DOWNLOAD_URL)
            else -> ShizukuManager.refresh()
        }
    }

    val selecting = st.selection.isNotEmpty()
    val searching = st.searchQuery != null

    BackHandler(enabled = selecting || searching || st.path != null) {
        when {
            selecting -> vm.clearSelection()
            searching -> vm.closeSearch()
            else -> vm.goUp()
        }
    }

    val onItemClick: (RemoteFile) -> Unit = { f -> if (selecting) vm.toggleSelect(f) else vm.open(f) }
    val onItemLong: (RemoteFile) -> Unit = { f ->
        if (f.isDirectory && !selecting && !searching && vp.folderLongPress == LongPressAction.Icon) {
            dlg = Dlg.FolderStyleDlg(f)
        } else {
            vm.toggleSelect(f)
        }
    }

    val showBg = homeBg != null && st.path == null && !searching
    val bgModifier = if (showBg && homeBg != null) {
        Modifier
            .clipToBounds()
            .paint(BitmapPainter(homeBg!!), contentScale = ContentScale.Crop)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
    } else {
        Modifier
    }
    Scaffold(
        modifier = bgModifier,
        containerColor = if (showBg) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            when {
                selecting -> SelectionTopBar(st.selection.size, onClose = vm::clearSelection, onSelectAll = vm::selectAll)
                searching -> SearchTopBar(st.searchQuery.orEmpty(), st.path, vm.rootPath, onQuery = vm::search, onClose = vm::closeSearch)
                else -> MainTopBar(
                    title = st.path?.let { if (it == vm.rootPath) "حافظه داخلی" else it.substringAfterLast('/') } ?: "فایل‌یار",
                    subtitle = if (st.path == null) statusText(shizuku) else countText(st.items),
                    atHome = st.path == null,
                    shizukuReady = shizuku == ShizukuStatus.Ready,
                    grid = gridHere,
                    layoutOverridden = layoutOverridden,
                    defaultGrid = vp.grid,
                    sort = vp.sort,
                    ascending = vp.ascending,
                    foldersFirst = vp.foldersFirst,
                    showHidden = vp.showHidden,
                    onBack = { vm.goUp() },
                    onSearch = vm::openSearch,
                    onToggleGrid = { curPath?.let { vm.setFolderLayout(it, !gridHere) } },
                    onToggleGridAll = { vm.updateView { it.copy(grid = !it.grid) } },
                    onResetLayout = { curPath?.let { vm.setFolderLayout(it, null) } },
                    onSort = { s -> vm.updateView { if (it.sort == s) it.copy(ascending = !it.ascending) else it.copy(sort = s, ascending = s == SortBy.Name || s == SortBy.Type) } },
                    onFoldersFirst = { vm.updateView { it.copy(foldersFirst = !it.foldersFirst) } },
                    onToggleHidden = { vm.updateView { it.copy(showHidden = !it.showHidden) } },
                    onRefresh = { vm.reload() },
                    onGuide = { dlg = Dlg.Guide },
                    onSettings = onOpenSettings,
                    onShizuku = shizukuAction,
                )
            }
        },
        bottomBar = {
            when {
                selecting -> SelectionBottomBar(
                    onCopy = { vm.setClip(vm.selectedItems(), cut = false) },
                    onCut = { vm.setClip(vm.selectedItems(), cut = true) },
                    onShare = { vm.share(vm.selectedItems()) },
                    onDelete = { dlg = Dlg.Delete(vm.selectedItems()) },
                    onMore = { dlg = Dlg.SelectionMore },
                )
                clip != null && st.path != null && !searching -> PasteBar(
                    count = clip!!.items.size,
                    cut = clip!!.cut,
                    onPaste = vm::paste,
                    onCancel = vm::clearClip,
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(!selecting && !searching && st.path != null && clip == null, enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(
                    onClick = { dlg = Dlg.CreateMenu },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                ) { Icon(Icons.Filled.Add, "ساختن") }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize(),
        ) {
            val path = st.path
            when {
                searching -> SearchResults(st, vm, onItemClick, onItemLong) { dlg = Dlg.ItemMenu(it) }
                path == null -> HomeDashboard(
                    rootPath = vm.rootPath,
                    shizuku = shizuku,
                    localAccess = localAccess,
                    storage = storage,
                    quickAccess = quickAccess,
                    onOpen = { vm.openFromHome(it) },
                    onShizukuAction = shizukuAction,
                    onShowGuide = { dlg = Dlg.Guide },
                    onGrantLocal = grantLocal,
                    onRemoveQuick = { vm.removeQuickAccess(it) },
                    onAddQuick = { dlg = Dlg.AddQuick },
                    onCustomizeQuick = { dlg = Dlg.QuickStyle(it) },
                    onOpenCleaner = { vm.openClean(it) },
                    style = vp.homeStyle,
                    onOpenMedia = { vm.openMedia(it) },
                    onOpenBackups = { vm.openBackups() },
                    listState = vm.homeListState,
                    stats = stats,
                    onOpenApps = { vm.openApps() },
                )
                else -> {
                    Breadcrumbs(path, vm.rootPath) { vm.navigate(it) }
                    st.mediaOnly?.let { k ->
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                            FilterChip(
                                selected = true,
                                onClick = vm::clearMediaFilter,
                                label = { Text("فقط ${k.title}") },
                                trailingIcon = { Icon(Icons.Filled.Close, "نمایش همه‌ی فایل‌ها", Modifier.size(16.dp)) },
                            )
                        }
                    }
                    FolderContent(
                        st = st,
                        grid = gridHere,
                        vm = vm,
                        shizuku = shizuku,
                        onShizukuAction = shizukuAction,
                        onGuide = { dlg = Dlg.Guide },
                        onGrantLocal = grantLocal,
                        onClick = onItemClick,
                        onLong = onItemLong,
                        onMore = { dlg = Dlg.ItemMenu(it) },
                    )
                }
            }
        }
    }

    // ---------- dialogs & sheets ----------
    when (val d = dlg) {
        null -> Unit
        Dlg.CreateMenu -> ActionSheet(
            header = { SectionTitle("ساختن در این پوشه") },
            actions = listOf(
                SheetAction(Icons.Filled.CreateNewFolder, "پوشه‌ی جدید") { dlg = Dlg.NewFolder },
                SheetAction(Icons.AutoMirrored.Filled.NoteAdd, "فایل جدید") { dlg = Dlg.NewFile },
            ),
            onDismiss = { dlg = null },
        )
        Dlg.NewFolder -> NameDialog("پوشه‌ی جدید", "", "ساختن", onDismiss = { dlg = null }) {
            dlg = null
            vm.createFolder(it)
        }
        Dlg.NewFile -> NameDialog("فایل جدید", "new.txt", "ساختن", selectBaseName = true, onDismiss = { dlg = null }) {
            dlg = null
            vm.createFile(it)
        }
        Dlg.Guide -> ShizukuGuideDialog(onDismiss = { dlg = null }) {
            dlg = null
            openShizukuApp(context)
        }
        Dlg.SelectionMore -> {
            val items = vm.selectedItems()
            val single = items.singleOrNull()
            ActionSheet(
                header = { SectionTitle("${faNum(items.size)} مورد انتخاب شده") },
                actions = buildList {
                    if (single != null) add(SheetAction(Icons.Filled.DriveFileRenameOutline, "تغییر نام") { dlg = Dlg.Rename(single) })
                    add(SheetAction(Icons.Filled.FolderZip, "فشرده‌سازی (ZIP)") { dlg = Dlg.Compress(items) })
                    if (single != null) add(SheetAction(Icons.Filled.Info, "مشخصات") { dlg = Dlg.Props(single) })
                    if (single != null && single.isDirectory) {
                        add(SheetAction(Icons.Filled.BookmarkBorder, "افزودن / حذف از دسترسی سریع") { vm.toggleQuickAccess(single.path) })
                    }
                    add(SheetAction(Icons.Filled.SelectAll, "انتخاب همه") { vm.selectAll() })
                },
                onDismiss = { if (dlg == Dlg.SelectionMore) dlg = null },
            )
        }
        is Dlg.ItemMenu -> {
            val f = d.f
            val bookmarked = f.path in quickAccess
            ActionSheet(
                header = { FileSheetHeader(f, backend) },
                actions = buildList {
                    if (f.isDirectory) {
                        add(SheetAction(Icons.Filled.FolderOpen, "باز کردن") { vm.navigate(f.path) })
                    } else {
                        add(SheetAction(Icons.AutoMirrored.Filled.OpenInNew, "باز کردن با…") { vm.openExternal(f) })
                        add(SheetAction(Icons.AutoMirrored.Filled.TextSnippet, "باز کردن در ویرایشگر متن") { vm.openAsText(f) })
                        add(SheetAction(Icons.Filled.Share, "اشتراک‌گذاری") { vm.share(listOf(f)) })
                    }
                    if (isZip(f)) add(SheetAction(Icons.Filled.Unarchive, "استخراج در همین‌جا") { dlg = Dlg.Extract(f) })
                    if (isRestorable(f)) add(SheetAction(Icons.Filled.Restore, "بازگردانی دیتای برنامه") { dlg = Dlg.Restore(f) })
                    if (isAppDataDir(f, vm.rootPath)) add(SheetAction(Icons.Filled.Backup, "پشتیبان‌گیری از دیتای برنامه") { vm.backupAppData(f) })
                    add(SheetAction(Icons.Filled.ContentCopy, "کپی") { vm.setClip(listOf(f), cut = false) })
                    add(SheetAction(Icons.Filled.ContentCut, "انتقال") { vm.setClip(listOf(f), cut = true) })
                    add(SheetAction(Icons.Filled.DriveFileRenameOutline, "تغییر نام") { dlg = Dlg.Rename(f) })
                    add(SheetAction(Icons.Filled.FolderZip, "فشرده‌سازی (ZIP)") { dlg = Dlg.Compress(listOf(f)) })
                    if (f.isDirectory) {
                        add(
                            SheetAction(
                                if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                if (bookmarked) "حذف از دسترسی سریع" else "افزودن به دسترسی سریع",
                            ) { vm.toggleQuickAccess(f.path) }
                        )
                        add(SheetAction(Icons.Filled.Palette, "تغییر آیکون پوشه") { dlg = Dlg.FolderStyleDlg(f) })
                    }
                    add(SheetAction(Icons.Filled.CheckCircle, "انتخاب") { vm.toggleSelect(f) })
                    add(SheetAction(Icons.Filled.Info, "مشخصات") { dlg = Dlg.Props(f) })
                    add(SheetAction(Icons.Filled.Delete, "حذف", danger = true) { dlg = Dlg.Delete(listOf(f)) })
                },
                onDismiss = { if (dlg == d) dlg = null },
            )
        }
        is Dlg.Rename -> NameDialog("تغییر نام", d.f.name, "ذخیره", selectBaseName = !d.f.isDirectory, onDismiss = { dlg = null }) {
            dlg = null
            vm.rename(d.f, it)
        }
        is Dlg.Delete -> ConfirmDialog(
            title = "حذف شود؟",
            body = if (d.items.size == 1) "«${d.items[0].name}» برای همیشه حذف می‌شود." else "${faNum(d.items.size)} مورد برای همیشه حذف می‌شوند.",
            confirm = "حذف",
            danger = true,
            onDismiss = { dlg = null },
        ) {
            dlg = null
            vm.delete(d.items)
        }
        is Dlg.Props -> PropertiesDialog(d.f, vm::measure) { dlg = null }
        is Dlg.Compress -> NameDialog(
            "فشرده‌سازی",
            (d.items.singleOrNull()?.name?.substringBeforeLast('.')?.ifEmpty { null } ?: "archive") + ".zip",
            "فشرده کن",
            selectBaseName = true,
            onDismiss = { dlg = null },
        ) {
            dlg = null
            vm.compress(d.items, it)
        }
        is Dlg.Extract -> ConfirmDialog(
            title = "استخراج",
            body = "محتوای «${d.f.name}» در یک پوشه‌ی جدید کنار همین فایل استخراج شود؟",
            confirm = "استخراج",
            danger = false,
            onDismiss = { dlg = null },
        ) {
            dlg = null
            vm.extract(d.f)
        }
        is Dlg.Restore -> ConfirmDialog(
            title = "بازگردانی دیتا",
            body = "دیتای «${d.f.name}» به Android/data و Android/obb برگردانده می‌شود و فایل‌های هم‌نام جایگزین می‌شوند. برنامه باید قبلاً نصب شده باشد.",
            confirm = "بازگردانی",
            danger = true,
            onDismiss = { dlg = null },
        ) {
            dlg = null
            vm.restoreAppData(d.f)
        }
        Dlg.AddQuick -> FolderPickerDialog(
            startPath = vm.rootPath,
            rootPath = vm.rootPath,
            list = vm::listFolders,
            onDismiss = { dlg = null },
        ) {
            dlg = null
            vm.addQuickAccess(it)
        }
        is Dlg.QuickStyle -> FolderStyleDialog(
            name = d.path.substringAfterLast('/'),
            current = folderStyles[d.path],
            onDismiss = { dlg = null },
            onSave = {
                dlg = null
                vm.setFolderStyle(d.path, it)
            },
        )
        is Dlg.FolderStyleDlg -> FolderStyleDialog(
            name = d.f.name,
            current = folderStyles[d.f.path],
            onDismiss = { dlg = null },
            onSave = {
                dlg = null
                vm.setFolderStyle(d.f.path, it)
            },
        )
    }
}

private fun statusText(s: ShizukuStatus) = when (s) {
    ShizukuStatus.Ready -> "Shizuku فعال"
    ShizukuStatus.Connecting -> "Shizuku در حال اتصال…"
    else -> "Shizuku غیرفعال"
}

private fun countText(items: List<RemoteFile>): String {
    val dirs = items.count { it.isDirectory }
    val files = items.size - dirs
    return "${faNum(dirs)} پوشه • ${faNum(files)} فایل"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    title: String,
    subtitle: String,
    atHome: Boolean,
    shizukuReady: Boolean,
    grid: Boolean,
    layoutOverridden: Boolean,
    defaultGrid: Boolean,
    sort: SortBy,
    ascending: Boolean,
    foldersFirst: Boolean,
    showHidden: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleGridAll: () -> Unit,
    onResetLayout: () -> Unit,
    onSort: (SortBy) -> Unit,
    onFoldersFirst: () -> Unit,
    onToggleHidden: () -> Unit,
    onRefresh: () -> Unit,
    onGuide: () -> Unit,
    onSettings: () -> Unit,
    onShizuku: () -> Unit,
) {
    var moreOpen by remember { mutableStateOf(false) }
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = if (atHome) Color.Transparent else MaterialTheme.colorScheme.background),
        navigationIcon = {
            if (!atHome) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") }
            }
        },
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (atHome) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (shizukuReady) Filyar.tokens.success else Filyar.tokens.warning),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(enabled = atHome && !shizukuReady, onClick = onShizuku),
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, "جستجو") }
            Box {
                IconButton(onClick = { moreOpen = true }) { Icon(Icons.Filled.MoreVert, "بیشتر") }
                DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                    if (!atHome) {
                        DropdownMenuItem(
                            text = { Text(if (grid) "فهرستی کردن این پوشه" else "شبکه‌ای کردن این پوشه") },
                            leadingIcon = { Icon(if (grid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView, null) },
                            onClick = onToggleGrid,
                        )
                        if (layoutOverridden) {
                            DropdownMenuItem(
                                text = { Text("چیدمان این پوشه مثل پیش‌فرض") },
                                leadingIcon = { Icon(Icons.Filled.Restore, null) },
                                onClick = {
                                    moreOpen = false
                                    onResetLayout()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (defaultGrid) "پیش‌فرض همه‌ی پوشه‌ها: فهرستی" else "پیش‌فرض همه‌ی پوشه‌ها: شبکه‌ای") },
                            leadingIcon = { Icon(Icons.Filled.GridView, null) },
                            onClick = onToggleGridAll,
                        )
                        HorizontalDivider()
                        Text(
                            "مرتب‌سازی بر اساس",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        listOf(SortBy.Name to "نام", SortBy.Date to "تاریخ", SortBy.Size to "حجم", SortBy.Type to "نوع").forEach { (s, label) ->
                            DropdownMenuItem(
                                text = { Text(if (sort == s) "$label  ${if (ascending) "↑" else "↓"}" else label) },
                                leadingIcon = { RadioButton(selected = sort == s, onClick = null) },
                                onClick = { onSort(s) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("اول پوشه‌ها") },
                            leadingIcon = { if (foldersFirst) Icon(Icons.Filled.Check, null) else Spacer(Modifier.size(24.dp)) },
                            onClick = onFoldersFirst,
                        )
                        HorizontalDivider()
                    }
                    DropdownMenuItem(
                        text = { Text(if (showHidden) "پنهان کردن فایل‌های مخفی" else "نمایش فایل‌های مخفی") },
                        leadingIcon = { Icon(if (showHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null) },
                        onClick = {
                            moreOpen = false
                            onToggleHidden()
                        },
                    )
                    if (!atHome) {
                        DropdownMenuItem(
                            text = { Text("تازه‌سازی") },
                            leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                            onClick = {
                                moreOpen = false
                                onRefresh()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("راهنمای Shizuku") },
                        leadingIcon = { Icon(Icons.Filled.Bolt, null) },
                        onClick = {
                            moreOpen = false
                            onGuide()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("تنظیمات") },
                        leadingIcon = { Icon(Icons.Filled.Settings, null) },
                        onClick = {
                            moreOpen = false
                            onSettings()
                        },
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onClose: () -> Unit, onSelectAll: () -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "لغو انتخاب") } },
        title = { Text("${faNum(count)} انتخاب شده", style = MaterialTheme.typography.titleMedium) },
        actions = { IconButton(onClick = onSelectAll) { Icon(Icons.Filled.SelectAll, "انتخاب همه") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(query: String, path: String?, rootPath: String, onQuery: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val where = path?.let { if (it == rootPath) "حافظه داخلی" else it.substringAfterLast('/') } ?: "حافظه داخلی"
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بستن جستجو") } },
        title = {
            TextField(
                value = query,
                onValueChange = onQuery,
                placeholder = { Text("جستجو در $where", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onQuery(query) }),
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Close, "پاک کردن") }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .focusRequester(focus),
            )
        },
    )
}

@Composable
private fun SelectionBottomBar(onCopy: () -> Unit, onCut: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit, onMore: () -> Unit) {
    BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        BarAction(Icons.Filled.ContentCopy, "کپی", Modifier.weight(1f), onCopy)
        BarAction(Icons.Filled.ContentCut, "انتقال", Modifier.weight(1f), onCut)
        BarAction(Icons.Filled.Share, "ارسال", Modifier.weight(1f), onShare)
        BarAction(Icons.Filled.Delete, "حذف", Modifier.weight(1f), onDelete)
        BarAction(Icons.Filled.MoreVert, "بیشتر", Modifier.weight(1f), onMore)
    }
}

@Composable
private fun BarAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PasteBar(count: Int, cut: Boolean, onPaste: () -> Unit, onCancel: () -> Unit) {
    AnimatedVisibility(true, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp),
        ) {
            Row(Modifier.padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (cut) Icons.Filled.ContentCut else Icons.Filled.ContentCopy, null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${faNum(count)} مورد", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (cut) "به پوشه‌ی مقصد بروید و منتقل کنید" else "به پوشه‌ی مقصد بروید و بچسبانید",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                TextButton(onClick = onCancel) { Text("لغو", color = MaterialTheme.colorScheme.inverseOnSurface) }
                Button(onClick = onPaste) {
                    Icon(Icons.Filled.ContentPaste, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (cut) "انتقال اینجا" else "چسباندن", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderContent(
    st: BrowserState,
    grid: Boolean,
    vm: BrowserViewModel,
    shizuku: ShizukuStatus,
    onShizukuAction: () -> Unit,
    onGuide: () -> Unit,
    onGrantLocal: () -> Unit,
    onClick: (RemoteFile) -> Unit,
    onLong: (RemoteFile) -> Unit,
    onMore: (RemoteFile) -> Unit,
) {
    val backend = vm.backend
    val selecting = st.selection.isNotEmpty()
    PullToRefreshBox(
        isRefreshing = st.loading && st.items.isNotEmpty(),
        onRefresh = { vm.reload() },
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            st.loading && st.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            st.errorKind == ErrorKind.NeedsShizuku -> StateMessage(
                icon = Icons.Filled.Bolt,
                title = "این پوشه به Shizuku نیاز دارد",
                body = "اندروید دسترسی مستقیم به Android/data و Android/obb را بسته است. با روشن کردن Shizuku این محدودیت برداشته می‌شود.",
                action = when (shizuku) {
                    ShizukuStatus.NoPermission -> "اجازه دادن به فایل‌یار"
                    ShizukuStatus.NotInstalled -> "دریافت Shizuku"
                    ShizukuStatus.NotRunning -> "باز کردن Shizuku"
                    else -> "راهنما"
                },
                onAction = if (shizuku == ShizukuStatus.Connecting || shizuku == ShizukuStatus.Ready) onGuide else onShizukuAction,
            )
            st.errorKind == ErrorKind.NeedsPermission -> StateMessage(
                icon = Icons.Filled.Lock,
                title = "اجازه‌ی دسترسی لازم است",
                body = "برای دیدن فایل‌ها، دسترسی به همه‌ی فایل‌ها را بدهید یا Shizuku را روشن کنید.",
                action = "اجازه دادن",
                onAction = onGrantLocal,
            )
            st.errorKind != null -> StateMessage(
                icon = Icons.Filled.ErrorOutline,
                title = "این پوشه باز نشد",
                body = st.errorText,
                action = "تلاش دوباره",
                onAction = { vm.reload() },
            )
            st.items.isEmpty() -> StateMessage(Icons.Filled.FolderOpen, "این پوشه خالی است", "با دکمه‌ی + پوشه یا فایل بسازید.")
            grid -> {
                val gridState = remember(st.path) { vm.scrollFor(st.path).let { (i, o) -> LazyGridState(i, o) } }
                DisposableEffect(st.path) {
                    onDispose { vm.saveScroll(st.path, gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) }
                }
                LazyVerticalGrid(
                    columns = LocalViewPrefs.current.let { p ->
                        val scale = maxOf(1f, p.fileScale, p.folderScale)
                        val icon = p.itemSize.gridIcon * maxOf(p.fileScale, p.folderScale)
                        if (p.gridColumns > 0) {
                            GridCells.Fixed(p.gridColumns)
                        } else {
                            // A smaller gap narrows the cells, so more items fit in a row.
                            GridCells.Adaptive(maxOf(icon + 12.dp, p.itemSize.gridCell * scale * (0.5f + 0.5f * p.itemGap)))
                        }
                    },
                    state = gridState,
                    contentPadding = LocalViewPrefs.current.itemGap.let { g -> PaddingValues(start = 8.dp * g, end = 8.dp * g, top = 4.dp * g, bottom = 120.dp) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(st.items, key = { it.path }) { f ->
                        FileGridItem(f, backend, f.path in st.selection, onClick = { onClick(f) }, onLongClick = { onLong(f) })
                    }
                }
            }
            else -> {
                val listState = remember(st.path) { vm.scrollFor(st.path).let { (i, o) -> LazyListState(i, o) } }
                DisposableEffect(st.path) {
                    onDispose { vm.saveScroll(st.path, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
                }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(st.items, key = { it.path }) { f ->
                        FileRow(
                            f = f,
                            backend = backend,
                            selected = f.path in st.selection,
                            selecting = selecting,
                            onClick = { onClick(f) },
                            onLongClick = { onLong(f) },
                            onMore = { onMore(f) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    st: BrowserState,
    vm: BrowserViewModel,
    onClick: (RemoteFile) -> Unit,
    onLong: (RemoteFile) -> Unit,
    onMore: (RemoteFile) -> Unit,
) {
    val backend = vm.backend
    Column(Modifier.fillMaxSize()) {
        if (st.searching) LinearProgressIndicator(Modifier.fillMaxWidth())
        when {
            st.searchQuery.isNullOrBlank() -> StateMessage(Icons.Filled.Search, "نام فایل یا پوشه را بنویسید", "جستجو در همه‌ی زیرپوشه‌ها انجام می‌شود.")
            !st.searching && st.searchResults.isEmpty() -> StateMessage(Icons.Filled.SearchOff, "چیزی پیدا نشد", null)
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                item {
                    Text(
                        "${faNum(st.searchResults.size)} نتیجه",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
                items(st.searchResults, key = { it.path }) { f ->
                    FileRow(
                        f = f,
                        backend = backend,
                        selected = f.path in st.selection,
                        selecting = st.selection.isNotEmpty(),
                        subtitle = f.parent.removePrefix(vm.rootPath).ifEmpty { "/" },
                        onClick = { onClick(f) },
                        onLongClick = { onLong(f) },
                        onMore = { onMore(f) },
                    )
                }
            }
        }
    }
}
