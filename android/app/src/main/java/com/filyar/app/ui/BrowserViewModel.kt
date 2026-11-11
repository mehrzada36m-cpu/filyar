package com.filyar.app.ui

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
)

data class Clip(val items: List<RemoteFile>, val cut: Boolean)

data class OpProgress(val title: String, val current: String = "", val fraction: Float? = null)

sealed interface UiEvent {
    data class Edit(val file: RemoteFile) : UiEvent
    data class ViewImage(val file: RemoteFile) : UiEvent

    /** An intent for another app; it is started from the Activity so it opens on top of this app. */
    data class Launch(val intent: Intent) : UiEvent
}

enum class CleanMode { Large, Duplicates, Apks }

/** The three kinds of media the home screen can list by folder. */
enum class MediaKind(val title: String, val extensions: Set<String>) {
    Images("تصاویر", setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif")),
    Videos("ویدیوها", setOf("mp4", "mkv", "webm", "3gp", "avi", "mov", "flv", "m4v", "mpg", "mpeg", "wmv", "ts")),
    Audio("موسیقی", setOf("mp3", "m4a", "aac", "ogg", "opus", "wav", "flac", "amr", "wma", "mid", "midi")),
}

/** Every media file found in storage; the screen groups them by folder for the chosen [kind]. */
data class MediaState(
    val kind: MediaKind? = null,
    val scanning: Boolean = false,
    val files: List<RemoteFile> = emptyList(),
    val scanned: Boolean = false,
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
    private val scrollMemory = HashMap<String, Int>()

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
    fun scrollFor(path: String?): Int = scrollMemory[path ?: ""] ?: 0
    fun saveScroll(path: String?, index: Int) {
        scrollMemory[path ?: ""] = index
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
        _state.update {
            it.copy(path = path, selection = emptySet(), searchQuery = null, searchResults = emptyList(), items = emptyList())
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
        val visible = if (vp.showHidden) list else list.filterNot { it.isHidden }
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
    fun openClean(mode: CleanMode) {
        _clean.update { it.copy(mode = mode, selection = emptySet(), error = null) }
        when (mode) {
            CleanMode.Large -> if (_clean.value.scannedMin < 0) scanLarge(DEFAULT_LARGE_MIN)
            CleanMode.Duplicates -> if (!_clean.value.duplicatesScanned) scanDuplicates()
            CleanMode.Apks -> if (!_clean.value.apksScanned) scanApks()
        }
    }

    fun openMedia(kind: MediaKind) {
        _media.update { it.copy(kind = kind) }
        val m = _media.value
        // Reuse the last scan for a couple of minutes so reopening is instant.
        if (!m.scanning && (!m.scanned || System.currentTimeMillis() - m.scannedAt > 120_000)) scanMedia()
    }

    fun closeMedia() {
        _media.update { it.copy(kind = null) }
    }

    fun setMediaIncludeHidden(v: Boolean) {
        _media.update { it.copy(includeHidden = v) }
    }

    /** Leaves the media list and shows [path], so "back" returns home. */
    fun showMediaFolder(path: String) {
        val kind = _media.value.kind
        closeMedia()
        openFromHome(path)
        if (kind != null) returnTo = { openMedia(kind) }
    }

    fun scanMedia() {
        mediaJob?.cancel()
        mediaJob = viewModelScope.launch {
            _media.update { it.copy(scanning = true, error = null) }
            val b = backend
            val res = withContext(Dispatchers.IO) {
                runCatching { b.scan(rootPath, MEDIA_MIN).filter { it.extension in ALL_MEDIA_EXTENSIONS } }
            }
            res.onSuccess { list ->
                _media.update { it.copy(scanning = false, files = list, scanned = true, scannedAt = System.currentTimeMillis()) }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _media.update { it.copy(scanning = false, error = e.message ?: "جستجو ممکن نشد") }
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
        closeClean()
        openFromHome(parentPath(path))
        if (mode != null) returnTo = { openClean(mode) }
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
            else -> openExternal(f, chooser = false)
        }
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
