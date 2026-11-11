package com.filyar.app.ui

import android.content.pm.ApplicationInfo
import android.provider.Settings
import androidx.compose.foundation.lazy.LazyListState
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Semaphore
import android.os.Bundle
import android.content.ContentResolver
import android.provider.MediaStore
import android.Manifest
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filyar.app.RemoteFile
import com.filyar.app.core.Duplicates
import com.filyar.app.core.FileBackend
import com.filyar.app.core.FolderStyle
import com.filyar.app.core.faNum
import com.filyar.app.core.formatSize
import com.filyar.app.core.FileOps
import com.filyar.app.core.Prefs
import com.filyar.app.core.Progress
import com.filyar.app.core.ShizukuManager
import com.filyar.app.core.ShizukuStatus
import com.filyar.app.core.SortBy
import com.filyar.app.core.ViewPrefs
import com.filyar.app.core.NaturalOrder
import com.filyar.app.core.joinPath
import com.filyar.app.core.parentPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

enum class ErrorKind { NeedsShizuku, NeedsPermission, Generic }

data class BrowserState(
    val path: String? = null,
    val items: List<RemoteFile> = emptyList(),
    val loading: Boolean = false,
    val errorKind: ErrorKind? = null,
    val errorText: String? = null,
    val selection: Set<String> = emptySet(),
    val searchQuery: String? = null,
    val searchResults: List<RemoteFile> = emptyList(),
    val searching: Boolean = false,
    /** Set when the folder was opened from a media list: only that kind of file is shown. */
    val mediaOnly: MediaKind? = null,
)

data class Clip(val items: List<RemoteFile>, val cut: Boolean)

data class OpProgress(val title: String, val current: String = "", val fraction: Float? = null)

sealed interface UiEvent {
    data class Edit(val file: RemoteFile) : UiEvent
    data class ViewImage(val file: RemoteFile) : UiEvent
    data class Play(val file: RemoteFile) : UiEvent

    /** An intent for another app; it is started from the Activity so it opens on top of this app. */
    data class Launch(val intent: Intent) : UiEvent
}

enum class CleanMode { Large, Duplicates, Apks, Leftovers, Recent }

/** The three kinds of media the home screen can list by folder. */
enum class MediaKind(val title: String, val extensions: Set<String>) {
    Images("تصاویر", setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif")),
    Videos("ویدیوها", setOf("mp4", "mkv", "webm", "3gp", "avi", "mov", "flv", "m4v", "mpg", "mpeg", "wmv", "ts")),
    Audio("موسیقی", setOf("mp3", "m4a", "aac", "ogg", "opus", "wav", "flac", "amr", "wma", "mid", "midi")),
}

/** Totals shown on the home screen's media and apps tiles. */
data class MediaStats(
    val loaded: Boolean = false,
    val images: Int = 0,
    val imageBytes: Long = 0L,
    val cameraImages: Int = 0,
    val videos: Int = 0,
    val videoBytes: Long = 0L,
    val cameraVideos: Int = 0,
    val tracks: Int = 0,
    val audioBytes: Long = 0L,
    val artists: Int = 0,
    val albums: Int = 0,
    val alarms: Int = 0,
    val ringtones: Int = 0,
    val notifications: Int = 0,
    val podcasts: Int = 0,
    val userApps: Int = 0,
    val systemApps: Int = 0,
)

/** The flat audio lists on the music screen; [flag] is the media database column that selects them. */
enum class AudioList(val title: String, val flag: String) {
    Tracks("همه‌ی آهنگ‌ها", MediaStore.Audio.Media.IS_MUSIC),
    Alarms("زنگ‌های هشدار", MediaStore.Audio.Media.IS_ALARM),
    Ringtones("زنگ تماس", MediaStore.Audio.Media.IS_RINGTONE),
    Notifications("اعلان‌ها", MediaStore.Audio.Media.IS_NOTIFICATION),
    Podcasts("پادکست‌ها", MediaStore.Audio.Media.IS_PODCAST),
}

data class ListGroup(val key: Long, val title: String, val count: Int)

/** A list screen on top of the media screen: either groups (artists, albums) or files. */
data class ListingState(
    val title: String,
    val files: List<RemoteFile> = emptyList(),
    val groups: List<ListGroup>? = null,
    val byArtist: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
)

data class AppEntry(
    val pkg: String,
    val label: String,
    val versionName: String,
    val isSystem: Boolean,
    val apkSize: Long,
    val sourceDir: String,
)

/** What an APK file says about itself, next to what is installed. */
data class ApkInfo(
    val label: String,
    val pkg: String,
    val versionName: String,
    val versionCode: Long,
    val installedName: String?,
    val installedCode: Long?,
)

/** One folder that holds media of one kind, with its newest file as a preview. */
data class MediaFolderInfo(val path: String, val count: Int, val size: Long, val newest: RemoteFile)

data class MediaState(
    val kind: MediaKind? = null,
    val scanning: Boolean = false,
    val folders: Map<MediaKind, List<MediaFolderInfo>> = emptyMap(),
    val scanned: Boolean = false,
    /** True once the whole file system was walked, so app folders and unindexed files are included. */
    val fullScanned: Boolean = false,
    val scannedAt: Long = 0L,
    val error: String? = null,
    val includeHidden: Boolean = false,
)

/** State of the "advanced delete" screens. */
data class CleanState(
    val mode: CleanMode? = null,
    val scanning: Boolean = false,
    val status: String = "",
    val error: String? = null,
    /** Every file at least [scannedMin] bytes, biggest first. */
    val large: List<RemoteFile> = emptyList(),
    val scannedMin: Long = -1,
    val groups: List<List<RemoteFile>> = emptyList(),
    val duplicatesScanned: Boolean = false,
    /** Installable app files (apk, apks, xapk, apkm) anywhere in storage, newest first. */
    val apks: List<RemoteFile> = emptyList(),
    val apksScanned: Boolean = false,
    /** Android/data and Android/obb folders whose app is no longer installed. */
    val leftovers: List<RemoteFile> = emptyList(),
    val leftoversScanned: Boolean = false,
    /** The newest files in storage, newest first. */
    val recent: List<RemoteFile> = emptyList(),
    val recentScanned: Boolean = false,
    val selection: Set<String> = emptySet(),
)

class BrowserViewModel(app: Application) : AndroidViewModel(app) {

    val rootPath: String = Environment.getExternalStorageDirectory().absolutePath
    private val prefs = Prefs(app)

    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    val viewPrefs: StateFlow<ViewPrefs> = prefs.view
    val quickAccess: StateFlow<List<String>> = prefs.quickAccess
    val folderStyles: StateFlow<Map<String, FolderStyle>> = prefs.folderStyles
    val folderLayouts: StateFlow<Map<String, Boolean>> = prefs.folderLayouts

    private val _media = MutableStateFlow(MediaState())
    val media: StateFlow<MediaState> = _media.asStateFlow()
    private var mediaJob: Job? = null

    private val _clean = MutableStateFlow(CleanState())
    val clean: StateFlow<CleanState> = _clean.asStateFlow()
    private var cleanJob: Job? = null

    /** Folder opened from the home screen; going back from it returns straight home. */
    private var homeAnchor: String? = null

    /** Set when a folder was opened from a list screen (media, apk files): "back" returns there instead of home. */
    private var returnTo: (() -> Unit)? = null

    /** While set, the folder at [mediaFilterPath] lists only this kind of file. */
    private var mediaFilter: MediaKind? = null
    private var mediaFilterPath: String? = null

    private val _clip = MutableStateFlow<Clip?>(null)
    val clip: StateFlow<Clip?> = _clip.asStateFlow()

    private val _progress = MutableStateFlow<OpProgress?>(null)
    val progress: StateFlow<OpProgress?> = _progress.asStateFlow()

    private val _storage = MutableStateFlow<LongArray?>(null)
    val storage: StateFlow<LongArray?> = _storage.asStateFlow()

    private val _localAccess = MutableStateFlow(hasLocalAccess())
    val localAccess: StateFlow<Boolean> = _localAccess.asStateFlow()

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var rawItems: List<RemoteFile> = emptyList()
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var opJob: Job? = null
    private val scrollMemory = HashMap<String, Pair<Int, Int>>()

    /** Scroll position of the home screen; it lives here so it survives opening a folder or another screen. */
    val homeListState = LazyListState()

    private val cleanStates = HashMap<CleanMode, LazyListState>()
    private val mediaStates = HashMap<MediaKind, LazyListState>()
    fun cleanListState(mode: CleanMode): LazyListState = cleanStates.getOrPut(mode) { LazyListState() }
    fun mediaListState(kind: MediaKind): LazyListState = mediaStates.getOrPut(kind) { LazyListState() }

    val backend: FileBackend get() = ShizukuManager.backend()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { File(app.cacheDir, "shared").deleteRecursively() }
        }
        viewModelScope.launch {
            ShizukuManager.status.collect {
                if (it == ShizukuStatus.Ready || it == ShizukuStatus.NotRunning || it == ShizukuStatus.NoPermission) {
                    reload()
                    loadStorage()
                }
            }
        }
    }

    // ---------- permissions ----------
    fun hasLocalAccess(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun onResume() {
        val before = _localAccess.value
        _localAccess.value = hasLocalAccess()
        ShizukuManager.refresh()
        if (before != _localAccess.value) reload()
        loadStorage()
    }

    // ---------- navigation ----------
    fun scrollFor(path: String?): Pair<Int, Int> = scrollMemory[path ?: ""] ?: (0 to 0)
    fun saveScroll(path: String?, index: Int, offset: Int) {
        scrollMemory[path ?: ""] = index to offset
    }

    /** Opens a folder from the home screen so that "back" returns to home, not to its parent. */
    fun openFromHome(path: String) {
        returnTo = null
        navigate(path)
        homeAnchor = path
    }

    fun navigate(path: String?) {
        val anchor = homeAnchor
        if (path == null || (anchor != null && path != anchor && !path.startsWith("$anchor/"))) {
            homeAnchor = null
            returnTo = null
        }
        if (path != mediaFilterPath) {
            mediaFilter = null
            mediaFilterPath = null
        }
        _state.update {
            it.copy(path = path, selection = emptySet(), searchQuery = null, searchResults = emptyList(), items = emptyList(), mediaOnly = mediaFilter)
        }
        reload()
    }

    fun goUp(): Boolean {
        val p = _state.value.path ?: return false
        if (p == homeAnchor || p == rootPath || !p.startsWith(rootPath)) {
            val back = if (p == homeAnchor) returnTo else null
            navigate(null)
            back?.invoke()
        } else {
            navigate(parentPath(p))
        }
        return true
    }

    fun reload() {
        val path = _state.value.path ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, errorKind = null, errorText = null) }
            val b = backend
            val result = withContext(Dispatchers.IO) { runCatching { b.list(path) } }
            result.onSuccess { list ->
                rawItems = list
                _state.update { it.copy(items = arrange(list), loading = false) }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                rawItems = emptyList()
                val kind = when {
                    isRestricted(path) && !b.isPrivileged -> ErrorKind.NeedsShizuku
                    !b.isPrivileged && !hasLocalAccess() -> ErrorKind.NeedsPermission
                    else -> ErrorKind.Generic
                }
                _state.update { it.copy(items = emptyList(), loading = false, errorKind = kind, errorText = e.message) }
            }
        }
    }

    fun isRestricted(path: String): Boolean {
        val d = "$rootPath/Android/data"
        val o = "$rootPath/Android/obb"
        return path == d || path.startsWith("$d/") || path == o || path.startsWith("$o/")
    }

    private fun loadStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            _storage.value = runCatching { backend.storage(rootPath) }.getOrNull()
                ?: runCatching { LocalStat.of(rootPath) }.getOrNull()
        }
    }

    // ---------- sorting & view ----------
    fun updateView(transform: (ViewPrefs) -> ViewPrefs) {
        prefs.updateView(transform)
        _state.update { it.copy(items = arrange(rawItems)) }
    }

    private fun arrange(list: List<RemoteFile>): List<RemoteFile> {
        val vp = prefs.view.value
        val shown = if (vp.showHidden) list else list.filterNot { it.isHidden }
        val onlyKind = mediaFilter
        val visible = if (onlyKind == null) shown else shown.filter { !it.isDirectory && it.extension in onlyKind.extensions }
        val byName = Comparator<RemoteFile> { a, b -> NaturalOrder.compare(a.name, b.name) }
        val primary: Comparator<RemoteFile> = when (vp.sort) {
            SortBy.Name -> byName
            SortBy.Size -> compareBy<RemoteFile> { if (it.isDirectory) it.childCount.toLong() else it.size }.then(byName)
            SortBy.Date -> compareBy<RemoteFile> { it.lastModified }.then(byName)
            SortBy.Type -> compareBy<RemoteFile> { it.extension }.then(byName)
        }
        val directed = if (vp.ascending) primary else primary.reversed()
        val full = if (vp.foldersFirst) compareBy<RemoteFile> { !it.isDirectory }.then(directed) else directed
        return visible.sortedWith(full)
    }

    // ---------- selection ----------
    fun toggleSelect(f: RemoteFile) = _state.update {
        val s = it.selection.toMutableSet()
        if (!s.remove(f.path)) s.add(f.path)
        it.copy(selection = s)
    }

    fun selectAll() = _state.update { it.copy(selection = it.items.map { f -> f.path }.toSet()) }
    fun clearSelection() = _state.update { it.copy(selection = emptySet()) }

    fun selectedItems(): List<RemoteFile> {
        val st = _state.value
        val pool = if (st.searchQuery != null) st.searchResults else st.items
        return pool.filter { it.path in st.selection }
    }

    // ---------- search ----------
    fun openSearch() = _state.update { it.copy(searchQuery = "", searchResults = emptyList(), selection = emptySet()) }

    fun closeSearch() {
        searchJob?.cancel()
        _state.update { it.copy(searchQuery = null, searchResults = emptyList(), searching = false, selection = emptySet()) }
    }

    fun search(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList(), searching = false) }
            return
        }
        val root = _state.value.path ?: rootPath
        searchJob = viewModelScope.launch {
            _state.update { it.copy(searching = true) }
            delay(350)
            val b = backend
            val res = withContext(Dispatchers.IO) { runCatching { b.search(root, query.trim(), 400) }.getOrDefault(emptyList()) }
            val vp = prefs.view.value
            _state.update {
                it.copy(searchResults = if (vp.showHidden) res else res.filterNot { f -> f.isHidden }, searching = false)
            }
        }
    }

    // ---------- clipboard ----------
    fun setClip(items: List<RemoteFile>, cut: Boolean) {
        if (items.isEmpty()) return
        _clip.value = Clip(items, cut)
        clearSelection()
        toast(if (cut) "${items.size} مورد برای انتقال آماده است" else "${items.size} مورد کپی شد")
    }

    fun clearClip() {
        _clip.value = null
    }

    fun paste() {
        val clip = _clip.value ?: return
        val dest = _state.value.path ?: return
        runOp(if (clip.cut) "در حال انتقال" else "در حال کپی") { b, report ->
            val total = if (clip.cut) 0L else FileOps.totalSize(b, clip.items)
            val p = Progress(total, report)
            for (item in clip.items) {
                if (clip.cut) {
                    FileOps.move(b, item, dest, p)?.let { prefs.movePath(item.path, it) }
                } else {
                    FileOps.copy(b, item, dest, p)
                }
            }
            if (clip.cut) _clip.value = null
            if (clip.cut) "انتقال انجام شد" else "کپی انجام شد"
        }
    }

    // ---------- file operations ----------
    fun delete(items: List<RemoteFile>) = runOp("در حال حذف") { b, report ->
        try {
            FileOps.delete(b, items, Progress(0, report))
        } finally {
            items.filter { it.isDirectory }.forEach { prefs.forgetPath(it.path) }
        }
        clearSelection()
        "${faNum(items.size)} مورد حذف شد"
    }

    /** Hides items by adding a dot to the start of their name (the Android convention), or shows them again. */
    fun setHidden(items: List<RemoteFile>, hide: Boolean) = runOp(if (hide) "مخفی کردن" else "آشکار کردن") { b, _ ->
        var count = 0
        for (f in items) {
            val newName = if (hide) ".${f.name}" else f.name.trimStart('.')
            if (newName == f.name || newName.isEmpty()) continue
            val target = joinPath(f.parent, newName)
            if (b.stat(target) != null) throw IOException("«$newName» از قبل وجود دارد")
            if (!b.rename(f.path, target)) throw IOException("تغییر نام ممکن نشد: ${f.name}")
            prefs.movePath(f.path, target)
            count++
        }
        clearSelection()
        when {
            count == 0 -> if (hide) "این موارد از قبل مخفی هستند" else "این موارد مخفی نیستند"
            hide && !prefs.view.value.showHidden ->
                "${faNum(count)} مورد مخفی شد. برای دیدن، «نمایش فایل‌های مخفی» را از منوی سه‌نقطه روشن کنید"
            hide -> "${faNum(count)} مورد مخفی شد"
            else -> "${faNum(count)} مورد آشکار شد"
        }
    }

    fun rename(f: RemoteFile, newName: String) = runOp("تغییر نام") { b, _ ->
        val name = newName.trim()
        if (name.isEmpty() || name.contains('/')) throw IOException("نام معتبر نیست")
        val target = joinPath(f.parent, name)
        if (!b.rename(f.path, target)) throw IOException("تغییر نام ممکن نشد. شاید این نام وجود دارد.")
        prefs.movePath(f.path, target)
        clearSelection()
        "نام تغییر کرد"
    }

    fun createFolder(name: String) = runOp("ساخت پوشه") { b, _ ->
        val dir = _state.value.path ?: throw IOException("یک پوشه را باز کنید")
        val n = name.trim()
        if (n.isEmpty() || n.contains('/')) throw IOException("نام معتبر نیست")
        if (b.stat(joinPath(dir, n)) != null) throw IOException("این نام از قبل وجود دارد")
        if (!b.mkdirs(joinPath(dir, n))) throw IOException("ساخت پوشه ممکن نشد")
        "پوشه ساخته شد"
    }

    fun createFile(name: String) = runOp("ساخت فایل") { b, _ ->
        val dir = _state.value.path ?: throw IOException("یک پوشه را باز کنید")
        val n = name.trim()
        if (n.isEmpty() || n.contains('/')) throw IOException("نام معتبر نیست")
        if (b.stat(joinPath(dir, n)) != null) throw IOException("این نام از قبل وجود دارد")
        if (!b.createFile(joinPath(dir, n))) throw IOException("ساخت فایل ممکن نشد")
        "فایل ساخته شد"
    }

    fun compress(items: List<RemoteFile>, zipName: String) = runOp("در حال فشرده‌سازی") { b, report ->
        val dir = items.firstOrNull()?.parent ?: throw IOException("چیزی انتخاب نشده")
        val base = zipName.trim().ifEmpty { "archive" }.removeSuffix(".zip")
        val name = FileOps.uniqueName(b, dir, "$base.zip")
        FileOps.compress(b, items, joinPath(dir, name), Progress(FileOps.totalSize(b, items), report))
        clearSelection()
        "فایل $name ساخته شد"
    }

    fun extract(zip: RemoteFile) = runOp("در حال استخراج") { b, report ->
        val out = FileOps.extract(b, zip, zip.parent, Progress(0, report))
        "استخراج شد در ${out.substringAfterLast('/')}"
    }

    // ---------- quick access & folder looks ----------
    fun addQuickAccess(path: String) {
        toast(if (prefs.addQuickAccess(path)) "به دسترسی سریع اضافه شد" else "این پوشه در دسترسی سریع هست")
    }

    fun removeQuickAccess(path: String) {
        prefs.removeQuickAccess(path)
        toast("از دسترسی سریع حذف شد")
    }

    fun toggleQuickAccess(path: String) {
        if (path in prefs.quickAccess.value) removeQuickAccess(path) else addQuickAccess(path)
    }

    fun resetQuickAccess() {
        prefs.resetQuickAccess()
        toast("دسترسی سریع به حالت اول برگشت")
    }

    fun setFolderStyle(path: String, style: FolderStyle?) = prefs.setFolderStyle(path, style)

    fun setFolderLayout(path: String, grid: Boolean?) = prefs.setFolderLayout(path, grid)

    fun clearFolderLayouts() {
        prefs.clearFolderLayouts()
        toast("چیدمان همه‌ی پوشه‌ها به حالت پیش‌فرض برگشت")
    }

    fun clearFolderStyles() {
        prefs.clearFolderStyles()
        toast("آیکون همه‌ی پوشه‌ها به حالت خودکار برگشت")
    }

    suspend fun listFolders(path: String): Result<List<RemoteFile>> = withContext(Dispatchers.IO) {
        runCatching {
            backend.list(path).filter { it.isDirectory && !it.isHidden }.sortedWith { a, b -> NaturalOrder.compare(a.name, b.name) }
        }
    }

    // ---------- advanced delete (large & duplicate files) ----------
    /** [keepScroll] is true when coming back from a folder, so the list stays where it was. */
    fun openClean(mode: CleanMode, keepScroll: Boolean = false) {
        if (!keepScroll) cleanStates.remove(mode)
        _clean.update { it.copy(mode = mode, selection = emptySet(), error = null) }
        when (mode) {
            CleanMode.Large -> if (_clean.value.scannedMin < 0) scanLarge(DEFAULT_LARGE_MIN)
            CleanMode.Duplicates -> if (!_clean.value.duplicatesScanned) scanDuplicates()
            CleanMode.Apks -> if (!_clean.value.apksScanned) scanApks()
            CleanMode.Leftovers -> if (!_clean.value.leftoversScanned) scanLeftovers()
            CleanMode.Recent -> if (!_clean.value.recentScanned) scanRecent()
        }
    }

    fun openMedia(kind: MediaKind, keepScroll: Boolean = false) {
        if (!keepScroll) mediaStates.remove(kind)
        _media.update { it.copy(kind = kind) }
        val m = _media.value
        // Reuse the last result for a couple of minutes so reopening is instant.
        if (!m.scanning && (!m.scanned || System.currentTimeMillis() - m.scannedAt > 120_000)) scanMedia()
    }

    fun closeMedia() {
        _media.update { it.copy(kind = null) }
    }

    fun setMediaIncludeHidden(v: Boolean) {
        _media.update { it.copy(includeHidden = v) }
        // The media database does not index app folders, so showing them needs a full scan.
        val m = _media.value
        if (v && !m.fullScanned && !m.scanning) scanMedia(full = true)
    }

    fun rescanMedia() = scanMedia(full = _media.value.includeHidden)

    /** Leaves the media list and shows [path] with only that kind of file, so "back" returns to the list. */
    fun showMediaFolder(path: String) {
        val kind = _media.value.kind
        closeMedia()
        mediaFilter = kind
        mediaFilterPath = path
        openFromHome(path)
        if (kind != null) returnTo = { openMedia(kind, keepScroll = true) }
    }

    fun clearMediaFilter() {
        mediaFilter = null
        mediaFilterPath = null
        _state.update { it.copy(items = arrange(rawItems), mediaOnly = null) }
    }

    /** Fast path: ask the system media database. Slow path: walk every file when it has nothing. */
    fun scanMedia(full: Boolean = false) {
        mediaJob?.cancel()
        mediaJob = viewModelScope.launch {
            _media.update { it.copy(scanning = true, error = null) }
            val b = backend
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val fast: Map<MediaKind, List<MediaFolderInfo>> = if (full) emptyMap() else queryMediaStore()
                    if (fast.values.any { it.isNotEmpty() }) {
                        fast to false
                    } else {
                        groupMedia(b.scan(rootPath, MEDIA_MIN).filter { it.extension in ALL_MEDIA_EXTENSIONS }) to true
                    }
                }
            }
            res.onSuccess { (folders, wasFull) ->
                _media.update {
                    it.copy(scanning = false, folders = folders, scanned = true, fullScanned = wasFull, scannedAt = System.currentTimeMillis())
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _media.update { it.copy(scanning = false, error = e.message ?: "جستجو ممکن نشد") }
            }
        }
    }

    private fun groupMedia(files: List<RemoteFile>): Map<MediaKind, List<MediaFolderInfo>> =
        MediaKind.values().associateWith { kind ->
            files.filter { it.extension in kind.extensions }
                .groupBy { it.parent }
                .map { (dir, list) ->
                    MediaFolderInfo(dir, list.size, list.sumOf { it.size }, list.maxByOrNull { it.lastModified } ?: list.first())
                }
        }

    private class MediaAgg {
        var count = 0
        var size = 0L
        var newestTime = -1L
        var newestPath = ""
        var newestSize = 0L
    }

    /** Folder totals straight from the media database, which answers in a fraction of a second. */
    private fun queryMediaStore(): Map<MediaKind, List<MediaFolderInfo>> {
        val resolver = getApplication<Application>().contentResolver
        val root = rootPath
        val sources = listOf(
            MediaKind.Images to MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaKind.Videos to MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaKind.Audio to MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        )
        return sources.associate { (kind, uri) ->
            val agg = HashMap<String, MediaAgg>()
            runCatching {
                val columns = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED)
                resolver.query(uri, columns, null, null, null)?.use { c ->
                    val iPath = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    val iSize = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val iDate = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    while (c.moveToNext()) {
                        val path = c.getString(iPath) ?: continue
                        if (!path.startsWith("$root/")) continue
                        val a = agg.getOrPut(path.substringBeforeLast('/')) { MediaAgg() }
                        val size = c.getLong(iSize)
                        val time = c.getLong(iDate) * 1000
                        a.count++
                        a.size += size
                        if (time >= a.newestTime) {
                            a.newestTime = time
                            a.newestPath = path
                            a.newestSize = size
                        }
                    }
                }
            }
            kind to agg.mapNotNull { (dir, a) ->
                // The database can still list folders that were deleted since the last media scan.
                if (!File(dir).exists()) return@mapNotNull null
                val name = a.newestPath.substringAfterLast('/')
                MediaFolderInfo(dir, a.count, a.size, RemoteFile(a.newestPath, name, false, a.newestSize, a.newestTime, 0, true, false))
            }
        }
    }

    fun closeClean() {
        cleanJob?.cancel()
        _clean.update { it.copy(mode = null, scanning = false, selection = emptySet()) }
    }

    /** Large files are scanned once; a smaller limit than the last scan triggers a new scan. */
    fun ensureLargeScan(minSize: Long) {
        val c = _clean.value
        if (c.scanning || (c.scannedMin in 0..minSize)) return
        scanLarge(minSize)
    }

    fun scanLarge(minSize: Long) {
        cleanJob?.cancel()
        cleanJob = viewModelScope.launch {
            _clean.update { it.copy(scanning = true, status = "در حال جستجوی فایل‌های بزرگ…", error = null, selection = emptySet()) }
            val b = backend
            val res = withContext(Dispatchers.IO) { runCatching { b.scan(rootPath, minSize) } }
            res.onSuccess { list ->
                _clean.update { it.copy(scanning = false, large = list, scannedMin = minSize) }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _clean.update { it.copy(scanning = false, error = e.message ?: "جستجو ممکن نشد") }
            }
        }
    }

    fun scanApks() {
        cleanJob?.cancel()
        cleanJob = viewModelScope.launch {
            _clean.update { it.copy(scanning = true, status = "در حال جستجوی فایل‌های برنامه‌ها…", error = null, selection = emptySet()) }
            val b = backend
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    b.scan(rootPath, APK_MIN)
                        .filter { it.extension in APK_EXTENSIONS }
                        .sortedByDescending { it.lastModified }
                }
            }
            res.onSuccess { list ->
                _clean.update { it.copy(scanning = false, apks = list, apksScanned = true) }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _clean.update { it.copy(scanning = false, error = e.message ?: "جستجو ممکن نشد") }
            }
        }
    }

    /** Leaves the clean screen and shows the folder that holds [path], so "back" returns home. */
    fun showInFolder(path: String) {
        val mode = _clean.value.mode
        val appsWereOpen = _appsOpen.value
        closeClean()
        _appsOpen.value = false
        openFromHome(parentPath(path))
        if (mode != null) returnTo = {
            openClean(mode, keepScroll = true)
            _appsOpen.value = appsWereOpen
        }
    }

    // ---------- folders left behind by uninstalled apps ----------
    fun scanLeftovers() {
        cleanJob?.cancel()
        cleanJob = viewModelScope.launch {
            _clean.update { it.copy(scanning = true, status = "در حال بررسی پوشه‌های برنامه‌ها…", error = null, selection = emptySet()) }
            val b = backend
            val app = getApplication<Application>()
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val pm = app.packageManager
                    val out = ArrayList<RemoteFile>()
                    var listed = 0
                    for (base in listOf("$rootPath/Android/data", "$rootPath/Android/obb")) {
                        for (d in b.list(base)) {
                            listed++
                            if (!d.isDirectory || !PACKAGE_NAME.matches(d.name)) continue
                            if (d.name == app.packageName || isInstalled(pm, d.name)) continue
                            val bytes = runCatching { b.measure(d.path)[0] }.getOrDefault(0L)
                            out.add(d.copy(size = bytes))
                        }
                    }
                    if (listed == 0 && !b.isPrivileged) throw IOException("برای دیدن Android/data باید Shizuku روشن باشد")
                    out.sortedByDescending { it.size }
                }
            }
            res.onSuccess { list ->
                _clean.update { it.copy(scanning = false, leftovers = list, leftoversScanned = true) }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _clean.update { it.copy(scanning = false, error = e.message ?: "جستجو ممکن نشد") }
            }
        }
    }

    private fun isInstalled(pm: PackageManager, pkg: String): Boolean =
        runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess

    // ---------- recent files ----------
    fun scanRecent() {
        cleanJob?.cancel()
        cleanJob = viewModelScope.launch {
            _clean.update { it.copy(scanning = true, status = "در حال یافتن فایل‌های اخیر…", error = null, selection = emptySet()) }
            val b = backend
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    queryRecentStore().ifEmpty {
                        b.scan(rootPath, 1024).filter { isUserPath(it.path) }.sortedByDescending { it.lastModified }.take(RECENT_COUNT)
                    }
                }
            }
            res.onSuccess { list ->
                _clean.update { it.copy(scanning = false, recent = list, recentScanned = true) }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _clean.update { it.copy(scanning = false, error = e.message ?: "جستجو ممکن نشد") }
            }
        }
    }

    /** App caches under Android/data and Android/obb and hidden folders are not "the user's files". */
    private fun isUserPath(path: String): Boolean {
        val rel = path.removePrefix(rootPath).trimStart('/')
        if (rel.startsWith("Android/data/") || rel.startsWith("Android/obb/")) return false
        return rel.substringBeforeLast('/', "").split('/').none { it.startsWith(".") }
    }

    /** The newest files according to the system media database: fast, but it only knows indexed files. */
    private fun queryRecentStore(): List<RemoteFile> {
        val resolver = getApplication<Application>().contentResolver
        val uri = MediaStore.Files.getContentUri("external")
        val args = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Files.FileColumns.DATA} LIKE ? AND ${MediaStore.Files.FileColumns.SIZE} > 0 AND ${MediaStore.Files.FileColumns.MIME_TYPE} IS NOT NULL",
            )
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("$rootPath/%"))
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
            putInt(ContentResolver.QUERY_ARG_LIMIT, 1000)
        }
        val out = ArrayList<RemoteFile>()
        runCatching {
            resolver.query(uri, arrayOf(MediaStore.Files.FileColumns.DATA), args, null)?.use { c ->
                val iPath = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                while (c.moveToNext() && out.size < RECENT_COUNT) {
                    val path = c.getString(iPath) ?: continue
                    if (!isUserPath(path)) continue
                    val f = File(path)
                    if (!f.isFile) continue // the database can still list files that were deleted
                    out.add(RemoteFile(path, f.name, false, f.length(), f.lastModified(), 0, true, false))
                }
            }
        }
        return out
    }

    // ---------- APK details ----------
    private val apkInfoCache = HashMap<String, ApkInfo?>()
    private val apkGate = Semaphore(2)

    suspend fun apkInfo(f: RemoteFile): ApkInfo? {
        if (f.extension != "apk") return null
        val key = "${f.path}:${f.lastModified}:${f.size}"
        synchronized(apkInfoCache) { if (apkInfoCache.containsKey(key)) return apkInfoCache[key] }
        val info = apkGate.withPermit { withContext(Dispatchers.IO) { runCatching { readApkInfo(f) }.getOrNull() } }
        synchronized(apkInfoCache) { apkInfoCache[key] = info }
        return info
    }

    private fun readApkInfo(f: RemoteFile): ApkInfo? {
        val app = getApplication<Application>()
        val pm = app.packageManager
        var temp: File? = null
        val path = if (File(f.path).canRead()) {
            f.path
        } else {
            // Android/data is hidden from the package manager, so read a private copy.
            if (f.size > 300L * 1024 * 1024) return null
            val t = File(app.cacheDir, "apkinfo/${System.nanoTime()}.apk").apply { parentFile?.mkdirs() }
            backend.openInput(f.path).use { input -> t.outputStream().use { input.copyTo(it) } }
            temp = t
            t.absolutePath
        }
        try {
            val pi = pm.getPackageArchiveInfo(path, 0) ?: return null
            val ai = pi.applicationInfo
            ai?.sourceDir = path
            ai?.publicSourceDir = path
            val label = ai?.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pi.packageName
            val code = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
            val installed = runCatching { pm.getPackageInfo(pi.packageName, 0) }.getOrNull()
            val installedCode = installed?.let { if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong() }
            return ApkInfo(label, pi.packageName, pi.versionName ?: "", code, installed?.versionName, installedCode)
        } finally {
            temp?.delete()
        }
    }

    // ---------- backup and restore of app data ----------
    fun backupAppData(dir: RemoteFile) = runOp("در حال پشتیبان‌گیری") { b, report ->
        val pkg = dir.name
        val parts = ArrayList<Pair<RemoteFile, String>>()
        for (sub in listOf("data", "obb")) {
            val src = b.stat("$rootPath/Android/$sub/$pkg")
            if (src != null && src.isDirectory) parts.add(src to "$sub/$pkg")
        }
        if (parts.isEmpty()) throw IOException("دیتایی برای این برنامه پیدا نشد")
        val backupDir = "$rootPath/Download/$BACKUP_DIR"
        b.mkdirs(backupDir)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val name = FileOps.uniqueName(b, backupDir, "$pkg-$stamp.filyar.zip")
        val zipPath = joinPath(backupDir, name)
        FileOps.compressAs(b, parts, zipPath, Progress(FileOps.totalSize(b, parts.map { it.first }), report))
        "پشتیبان ساخته شد: $name (${formatSize(b.stat(zipPath)?.size ?: 0L)})"
    }

    fun restoreAppData(zip: RemoteFile) = runOp("در حال بازگردانی") { b, report ->
        val n = FileOps.restore(b, zip, "$rootPath/Android", Progress(0, report))
        if (n == 0) throw IOException("فایلی برای بازگردانی در این پشتیبان نبود")
        "${faNum(n)} فایل بازگردانی شد"
    }

    /** Opens the folder that holds the backups, creating it the first time. */
    fun openBackups() {
        val dir = "$rootPath/Download/$BACKUP_DIR"
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { backend.mkdirs(dir) } }
            openFromHome(dir)
        }
    }

    // ---------- statistics for the home tiles ----------
    private val _stats = MutableStateFlow(MediaStats())
    val stats: StateFlow<MediaStats> = _stats.asStateFlow()
    private var statsAt = 0L

    fun refreshStats(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - statsAt < 30_000) return
        statsAt = now
        viewModelScope.launch {
            val s = withContext(Dispatchers.IO) { runCatching { computeStats() }.getOrNull() }
            if (s != null) _stats.value = s
        }
    }

    private fun isUserApp(ai: ApplicationInfo): Boolean =
        (ai.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

    private fun computeStats(): MediaStats {
        val app = getApplication<Application>()
        val resolver = app.contentResolver
        val root = rootPath
        var s = MediaStats(loaded = true)
        for ((isImage, uri) in listOf(true to MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false to MediaStore.Video.Media.EXTERNAL_CONTENT_URI)) {
            var count = 0
            var bytes = 0L
            var camera = 0
            runCatching {
                resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.SIZE), null, null, null)?.use { c ->
                    val iPath = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    val iSize = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    while (c.moveToNext()) {
                        val path = c.getString(iPath) ?: continue
                        if (!path.startsWith("$root/")) continue
                        count++
                        bytes += c.getLong(iSize)
                        if (path.contains("/DCIM/Camera/")) camera++
                    }
                }
            }
            s = if (isImage) s.copy(images = count, imageBytes = bytes, cameraImages = camera)
            else s.copy(videos = count, videoBytes = bytes, cameraVideos = camera)
        }
        val artists = HashSet<Long>()
        val albums = HashSet<Long>()
        var tracks = 0
        var audioBytes = 0L
        var alarms = 0
        var ringtones = 0
        var notifications = 0
        var podcasts = 0
        runCatching {
            val columns = arrayOf(
                MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.IS_ALARM, MediaStore.Audio.Media.IS_RINGTONE, MediaStore.Audio.Media.IS_NOTIFICATION,
                MediaStore.Audio.Media.IS_PODCAST, MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.ALBUM_ID,
            )
            resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns, null, null, null)?.use { c ->
                val iPath = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val iSize = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val iMusic = c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)
                val iAlarm = c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_ALARM)
                val iRing = c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_RINGTONE)
                val iNote = c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_NOTIFICATION)
                val iPod = c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_PODCAST)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                while (c.moveToNext()) {
                    val path = c.getString(iPath) ?: continue
                    if (!path.startsWith("$root/")) continue
                    if (c.getInt(iMusic) != 0) {
                        tracks++
                        audioBytes += c.getLong(iSize)
                        artists.add(c.getLong(iArtist))
                        albums.add(c.getLong(iAlbum))
                    }
                    if (c.getInt(iAlarm) != 0) alarms++
                    if (c.getInt(iRing) != 0) ringtones++
                    if (c.getInt(iNote) != 0) notifications++
                    if (c.getInt(iPod) != 0) podcasts++
                }
            }
        }
        s = s.copy(
            tracks = tracks, audioBytes = audioBytes, artists = artists.size, albums = albums.size,
            alarms = alarms, ringtones = ringtones, notifications = notifications, podcasts = podcasts,
        )
        val installed = runCatching { app.packageManager.getInstalledApplications(0) }.getOrDefault(emptyList())
        val user = installed.count { isUserApp(it) }
        return s.copy(userApps = user, systemApps = installed.size - user)
    }

    // ---------- lists on top of the media screen ----------
    private val _listing = MutableStateFlow<ListingState?>(null)
    val listing: StateFlow<ListingState?> = _listing.asStateFlow()
    private val listingStack = ArrayList<ListingState>()
    private var listingJob: Job? = null

    private fun pushListing(next: ListingState) {
        _listing.value?.let { listingStack.add(it) }
        _listing.value = next
    }

    fun closeListing() {
        listingJob?.cancel()
        _listing.value = if (listingStack.isEmpty()) null else listingStack.removeAt(listingStack.size - 1)
    }

    private fun audioUri(): Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    fun openAllMedia(kind: MediaKind) {
        val uri = when (kind) {
            MediaKind.Images -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaKind.Videos -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaKind.Audio -> audioUri()
        }
        loadListing(ListingState("همه‌ی ${kind.title}")) { queryFiles(uri, null, null, LIST_LIMIT) }
    }

    fun openAudioList(list: AudioList) {
        loadListing(ListingState(list.title)) { queryFiles(audioUri(), "${list.flag} != 0", null, LIST_LIMIT) }
    }

    fun openAudioGroups(byArtist: Boolean) {
        pushListing(ListingState(if (byArtist) "هنرمندان" else "آلبوم‌ها", groups = emptyList(), byArtist = byArtist))
        listingJob?.cancel()
        listingJob = viewModelScope.launch {
            val res = withContext(Dispatchers.IO) { runCatching { queryGroups(byArtist) } }
            res.onSuccess { groups -> _listing.update { it?.copy(groups = groups, loading = false) } }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    _listing.update { it?.copy(loading = false, error = e.message ?: "خواندن فهرست ممکن نشد") }
                }
        }
    }

    fun openGroup(g: ListGroup) {
        val byArtist = _listing.value?.byArtist ?: true
        val column = if (byArtist) MediaStore.Audio.Media.ARTIST_ID else MediaStore.Audio.Media.ALBUM_ID
        loadListing(ListingState(g.title)) {
            queryFiles(audioUri(), "$column = ? AND ${MediaStore.Audio.Media.IS_MUSIC} != 0", arrayOf(g.key.toString()), LIST_LIMIT)
        }
    }

    private fun loadListing(initial: ListingState, load: () -> List<RemoteFile>) {
        pushListing(initial)
        listingJob?.cancel()
        listingJob = viewModelScope.launch {
            val res = withContext(Dispatchers.IO) { runCatching(load) }
            res.onSuccess { files -> _listing.update { it?.copy(files = files, loading = false) } }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    _listing.update { it?.copy(loading = false, error = e.message ?: "خواندن فهرست ممکن نشد") }
                }
        }
    }

    private fun queryGroups(byArtist: Boolean): List<ListGroup> {
        val resolver = getApplication<Application>().contentResolver
        val idCol = if (byArtist) MediaStore.Audio.Media.ARTIST_ID else MediaStore.Audio.Media.ALBUM_ID
        val nameCol = if (byArtist) MediaStore.Audio.Media.ARTIST else MediaStore.Audio.Media.ALBUM
        val names = HashMap<Long, String>()
        val counts = HashMap<Long, Int>()
        resolver.query(audioUri(), arrayOf(idCol, nameCol), "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, null)?.use { c ->
            val iId = c.getColumnIndexOrThrow(idCol)
            val iName = c.getColumnIndexOrThrow(nameCol)
            while (c.moveToNext()) {
                val id = c.getLong(iId)
                counts[id] = (counts[id] ?: 0) + 1
                if (id !in names) names[id] = c.getString(iName)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "نامشخص"
            }
        }
        return counts.map { (id, n) -> ListGroup(id, names[id] ?: "نامشخص", n) }.sortedBy { it.title.lowercase() }
    }

    /** Files from the media database, newest first. Entries whose file is gone are skipped. */
    private fun queryFiles(uri: Uri, selection: String?, args: Array<String>?, limit: Int): List<RemoteFile> {
        val resolver = getApplication<Application>().contentResolver
        val bundle = Bundle().apply {
            if (selection != null) putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            if (args != null) putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        }
        val out = ArrayList<RemoteFile>()
        resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), bundle, null)?.use { c ->
            val iPath = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            while (c.moveToNext()) {
                val path = c.getString(iPath) ?: continue
                val f = File(path)
                if (!f.isFile) continue
                out.add(RemoteFile(path, f.name, false, f.length(), f.lastModified(), 0, true, false))
            }
        }
        return out
    }

    /** Shows a file's folder; "back" returns to the lists that were open. */
    fun showListingFolder(path: String) {
        val snap = _listing.value
        val snapStack = listingStack.toList()
        val mediaKind = _media.value.kind
        listingJob?.cancel()
        listingStack.clear()
        _listing.value = null
        _media.update { it.copy(kind = null) }
        openFromHome(parentPath(path))
        returnTo = {
            _media.update { it.copy(kind = mediaKind) }
            listingStack.clear()
            listingStack.addAll(snapStack)
            _listing.value = snap
        }
    }

    // ---------- installed apps ----------
    private val _appsOpen = MutableStateFlow(false)
    val appsOpen: StateFlow<Boolean> = _appsOpen.asStateFlow()
    private val _apps = MutableStateFlow<List<AppEntry>?>(null)
    val apps: StateFlow<List<AppEntry>?> = _apps.asStateFlow()
    private val _appsTab = MutableStateFlow(1)
    /** 1 = installed apps, 2 = system apps (0 is the APK-files screen). */
    val appsTab: StateFlow<Int> = _appsTab.asStateFlow()
    fun setAppsTab(tab: Int) { _appsTab.value = tab }

    fun openApps() {
        _appsOpen.value = true
        if (_apps.value == null) loadApps()
    }

    fun closeApps() {
        _appsOpen.value = false
    }

    fun loadApps() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { runCatching { readApps() }.getOrDefault(emptyList()) }
            _apps.value = list
        }
    }

    private fun readApps(): List<AppEntry> {
        val pm = getApplication<Application>().packageManager
        return pm.getInstalledPackages(0).mapNotNull { pi ->
            val ai = pi.applicationInfo ?: return@mapNotNull null
            AppEntry(pi.packageName, ai.loadLabel(pm).toString(), pi.versionName ?: "", !isUserApp(ai), File(ai.sourceDir).length(), ai.sourceDir)
        }.sortedBy { it.label.lowercase() }
    }

    fun appSettings(pkg: String) {
        eventChannel.trySend(UiEvent.Launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))))
    }

    fun launchApp(pkg: String) {
        val intent = getApplication<Application>().packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) eventChannel.trySend(UiEvent.Launch(intent)) else toast("این برنامه صفحه‌ی اجرا ندارد")
    }

    fun uninstallApp(pkg: String) {
        eventChannel.trySend(UiEvent.Launch(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))))
    }

    /** Copies the app's APK into Download/Filyar-APK. */
    fun backupApk(e: AppEntry) = runOp("در حال ذخیره‌ی APK") { b, _ ->
        val dir = "$rootPath/Download/Filyar-APK"
        b.mkdirs(dir)
        val safe = e.label.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val name = FileOps.uniqueName(b, dir, "$safe-${e.versionName.ifEmpty { "x" }}.apk")
        val dest = joinPath(dir, name)
        File(e.sourceDir).inputStream().use { input -> b.openOutput(dest).use { out -> input.copyTo(out) } }
        "ذخیره شد: $name (${formatSize(File(e.sourceDir).length())})"
    }

    fun scanDuplicates() {
        cleanJob?.cancel()
        cleanJob = viewModelScope.launch {
            _clean.update { it.copy(scanning = true, status = "در حال فهرست کردن فایل‌ها…", error = null, selection = emptySet()) }
            val b = backend
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    // App data folders are skipped: deleting files there can break apps.
                    val data = "$rootPath/Android/"
                    val files = b.scan(rootPath, DUPLICATE_MIN).filterNot { it.path.startsWith(data) }
                    Duplicates.find(b, files) { done, total ->
                        _clean.update { it.copy(status = "در حال مقایسه‌ی فایل‌ها: ${faNum(done)} از ${faNum(total)}") }
                    }
                }
            }
            res.onSuccess { groups ->
                _clean.update { it.copy(scanning = false, groups = groups, duplicatesScanned = true) }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _clean.update { it.copy(scanning = false, error = e.message ?: "جستجو ممکن نشد") }
            }
        }
    }

    fun cleanToggle(path: String) = _clean.update {
        val s = it.selection.toMutableSet()
        if (!s.remove(path)) s.add(path)
        it.copy(selection = s)
    }

    fun cleanSelect(paths: Collection<String>, selected: Boolean) = _clean.update {
        it.copy(selection = if (selected) it.selection + paths else it.selection - paths.toSet())
    }

    fun cleanClearSelection() = _clean.update { it.copy(selection = emptySet()) }

    /** In every duplicate group, keeps the oldest copy and selects the rest. */
    fun cleanSelectExtraCopies() = _clean.update { c ->
        c.copy(selection = c.groups.flatMap { g -> g.drop(1).map { it.path } }.toSet())
    }

    fun cleanSelectedFiles(): List<RemoteFile> {
        val c = _clean.value
        val pool = when (c.mode) {
            CleanMode.Duplicates -> c.groups.flatten()
            CleanMode.Apks -> c.apks
            CleanMode.Leftovers -> c.leftovers
            else -> c.large
        }
        return pool.filter { it.path in c.selection }.distinctBy { it.path }
    }

    fun cleanDelete(items: List<RemoteFile>) = runOp("در حال حذف") { b, report ->
        val p = Progress(0, report)
        val removed = HashSet<String>()
        var freed = 0L
        try {
            for (f in items) {
                p.file(f.name)
                if (b.delete(f.path)) {
                    removed.add(f.path)
                    freed += f.size
                }
            }
        } finally {
            _clean.update { c ->
                c.copy(
                    large = c.large.filterNot { it.path in removed },
                    apks = c.apks.filterNot { it.path in removed },
                    leftovers = c.leftovers.filterNot { it.path in removed },
                    groups = c.groups.map { g -> g.filterNot { it.path in removed } }.filter { it.size > 1 },
                    selection = c.selection - removed,
                )
            }
        }
        val failed = items.size - removed.size
        buildString {
            append("${faNum(removed.size)} فایل حذف شد • ${formatSize(freed)} آزاد شد")
            if (failed > 0) append(" • ${faNum(failed)} فایل حذف نشد")
        }
    }

    suspend fun measure(f: RemoteFile): LongArray? = withContext(Dispatchers.IO) {
        runCatching { if (f.isDirectory) backend.measure(f.path) else longArrayOf(f.size, 1, 0) }.getOrNull()
    }

    // ---------- open / share ----------
    fun open(f: RemoteFile) {
        if (f.isDirectory) {
            navigate(f.path)
            return
        }
        when {
            isEditable(f) -> eventChannel.trySend(UiEvent.Edit(f))
            kindOf(f) == FileKind.Image && f.extension != "svg" -> eventChannel.trySend(UiEvent.ViewImage(f))
            (kindOf(f) == FileKind.Video || kindOf(f) == FileKind.Audio) && f.extension != "ts" ->
                if (File(f.path).canRead() && !isRestricted(f.path)) eventChannel.trySend(UiEvent.Play(f)) else openExternal(f, chooser = false)
            f.extension == "apk" -> _apkPrompt.value = f
            f.extension == "zip" -> _zipPrompt.value = f
            else -> openExternal(f, chooser = false)
        }
    }

    // ---------- built-in installer and ZIP preview ----------
    private val _apkPrompt = MutableStateFlow<RemoteFile?>(null)
    val apkPrompt: StateFlow<RemoteFile?> = _apkPrompt.asStateFlow()
    private val _zipPrompt = MutableStateFlow<RemoteFile?>(null)
    val zipPrompt: StateFlow<RemoteFile?> = _zipPrompt.asStateFlow()

    fun dismissApkPrompt() { _apkPrompt.value = null }
    fun dismissZipPrompt() { _zipPrompt.value = null }

    /** Starts the system installer; asks for the "install unknown apps" switch first when it is off. */
    fun installApk(f: RemoteFile) {
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= 26 && !app.packageManager.canRequestPackageInstalls()) {
            toast("اجازه‌ی نصب برنامه را روشن کنید، بعد دوباره نصب را بزنید")
            eventChannel.trySend(UiEvent.Launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))))
            return
        }
        openExternal(f, chooser = false)
    }

    suspend fun zipEntries(f: RemoteFile): List<Pair<String, Long>> = withContext(Dispatchers.IO) {
        runCatching {
            val local = File(f.path)
            if (local.canRead() && !isRestricted(f.path)) {
                java.util.zip.ZipFile(local).use { z -> z.entries().asSequence().map { it.name to it.size }.toList() }
            } else {
                val out = ArrayList<Pair<String, Long>>()
                java.util.zip.ZipInputStream(backend.openInput(f.path).buffered()).use { zis ->
                    while (true) {
                        val e = zis.nextEntry ?: break
                        out.add(e.name to e.size)
                        zis.closeEntry()
                    }
                }
                out
            }
        }.getOrDefault(emptyList())
    }

    fun openAsText(f: RemoteFile) {
        eventChannel.trySend(UiEvent.Edit(f))
    }

    fun openExternal(f: RemoteFile, chooser: Boolean = true) = runOp("آماده‌سازی فایل") { _, report ->
        val uri = uriFor(f, report)
        val mime = mimeOf(f)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        eventChannel.trySend(UiEvent.Launch(if (chooser) Intent.createChooser(intent, "باز کردن با") else intent))
        null
    }

    fun share(items: List<RemoteFile>) = runOp("آماده‌سازی برای اشتراک") { _, report ->
        val files = items.filterNot { it.isDirectory }
        if (files.isEmpty()) throw IOException("پوشه‌ها را نمی‌توان مستقیم فرستاد؛ اول فشرده کنید")
        val uris = ArrayList(files.map { uriFor(it, report) })
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeOf(files[0])
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        eventChannel.trySend(UiEvent.Launch(Intent.createChooser(intent, "اشتراک‌گذاری")))
        clearSelection()
        null
    }

    private fun mimeOf(f: RemoteFile): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension) ?: when (kindOf(f)) {
            FileKind.Apk -> "application/vnd.android.package-archive"
            FileKind.Text, FileKind.Code -> "text/plain"
            else -> "*/*"
        }

    /** Other apps cannot read Android/data, so such files are copied to the cache first. */
    private suspend fun uriFor(f: RemoteFile, report: (String, Float?) -> Unit): Uri {
        val app = getApplication<Application>()
        val local = File(f.path)
        val file = if (local.canRead() && !isRestricted(f.path)) {
            local
        } else {
            val dir = File(app.cacheDir, "shared/${System.nanoTime()}").apply { mkdirs() }
            val out = File(dir, f.name)
            val p = Progress(f.size, report)
            p.file(f.name)
            backend.openInput(f.path).use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(256 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        p.add(n.toLong())
                    }
                }
            }
            out
        }
        return FileProvider.getUriForFile(app, "${app.packageName}.files", file)
    }

    // ---------- text ----------
    suspend fun readText(f: RemoteFile): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (f.size > MAX_EDIT_BYTES) throw IOException("فایل برای ویرایشگر بزرگ است (بیش از ۵ مگابایت)")
            backend.openInput(f.path).use { input ->
                val out = ByteArrayOutputStream()
                input.copyTo(out)
                out.toString(Charsets.UTF_8.name())
            }
        }
    }

    suspend fun writeText(f: RemoteFile, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            backend.openOutput(f.path).use { it.write(text.toByteArray(Charsets.UTF_8)) }
            Unit
        }
    }.also { r ->
        if (r.isSuccess) {
            toast("ذخیره شد")
            reload()
        } else {
            toast("ذخیره نشد: ${r.exceptionOrNull()?.message}")
        }
    }

    // ---------- helpers ----------
    fun toast(msg: String) {
        messageChannel.trySend(msg)
    }

    fun cancelOp() {
        opJob?.cancel()
    }

    private fun runOp(title: String, block: suspend (FileBackend, (String, Float?) -> Unit) -> String?) {
        if (opJob?.isActive == true) {
            toast("یک عملیات دیگر در حال انجام است")
            return
        }
        opJob = viewModelScope.launch(Dispatchers.IO) {
            _progress.value = OpProgress(title)
            try {
                val msg = block(backend) { current, fraction ->
                    _progress.value = OpProgress(title, current, fraction)
                }
                if (msg != null) toast(msg)
            } catch (e: CancellationException) {
                toast("لغو شد")
            } catch (e: Throwable) {
                toast(e.message ?: "خطا رخ داد")
            } finally {
                _progress.value = null
                reload()
                loadStorage()
            }
        }
    }

    companion object {
        const val MAX_EDIT_BYTES = 5L * 1024 * 1024
        const val MB = 1024L * 1024
        const val DEFAULT_LARGE_MIN = 50 * MB
        const val DUPLICATE_MIN = 10L * 1024
        const val APK_MIN = 50L * 1024
        const val MEDIA_MIN = 20L * 1024
        const val RECENT_COUNT = 200
        const val LIST_LIMIT = 3000
        const val BACKUP_DIR = "Filyar-Backup"
        val PACKAGE_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")
        val ALL_MEDIA_EXTENSIONS: Set<String> = MediaKind.values().flatMap { it.extensions }.toSet()
        val APK_EXTENSIONS = setOf("apk", "apks", "xapk", "apkm")
    }
}

private object LocalStat {
    fun of(path: String): LongArray {
        val s = android.os.StatFs(path)
        return longArrayOf(s.totalBytes, s.availableBytes)
    }
}
