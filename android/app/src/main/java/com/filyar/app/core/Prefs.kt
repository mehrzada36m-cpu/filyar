package com.filyar.app.core

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SortBy { Name, Size, Date, Type }
enum class ItemSize { Compact, Standard, Large }
enum class ThemeMode { System, Light, Dark }
enum class LongPressAction { Icon, Select }

/** How a folder is drawn: rounded tile, circle, bare icon, or outlined tile. */
enum class FolderModel { Tile, Circle, Plain, Outline }

/** Home screen layout: big cards, or a classic sectioned list. */
enum class HomeStyle { Cards, Classic }

data class ViewPrefs(
    val sort: SortBy = SortBy.Name,
    val ascending: Boolean = true,
    val foldersFirst: Boolean = true,
    val showHidden: Boolean = false,
    val grid: Boolean = false,
    val itemSize: ItemSize = ItemSize.Standard,
    val previews: Boolean = true,
    val appFolderIcons: Boolean = true,
    val theme: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
    val folderLongPress: LongPressAction = LongPressAction.Icon,
    /** Index into the accent palette; -1 keeps the default teal. */
    val accent: Int = -1,
    val folderModel: FolderModel = FolderModel.Tile,
    val homeStyle: HomeStyle = HomeStyle.Cards,
    /** Extra scaling on top of [itemSize]: 1f is the preset's own size. */
    val fileScale: Float = 1f,
    val folderScale: Float = 1f,
    /** The rounded card behind each item in grid view, and its tint (index into the accent palette, -1 = neutral). */
    val gridCards: Boolean = true,
    val cardColor: Int = -1,
)

/** A user-chosen look for one folder: icon key and colour index (-1 = default colour). */
data class FolderStyle(val icon: String, val color: Int, val model: FolderModel? = null)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("filyar", Context.MODE_PRIVATE)
    val rootPath: String = Environment.getExternalStorageDirectory().absolutePath

    private inline fun <reified T : Enum<T>> enumPref(key: String, def: T): T =
        runCatching { enumValueOf<T>(sp.getString(key, def.name)!!) }.getOrDefault(def)

    private val _view = MutableStateFlow(
        ViewPrefs(
            sort = enumPref("sort", SortBy.Name),
            ascending = sp.getBoolean("asc", true),
            foldersFirst = sp.getBoolean("foldersFirst", true),
            showHidden = sp.getBoolean("hidden", false),
            grid = sp.getBoolean("grid", false),
            itemSize = enumPref("itemSize", ItemSize.Standard),
            previews = sp.getBoolean("previews", true),
            appFolderIcons = sp.getBoolean("appFolderIcons", true),
            theme = enumPref("theme", ThemeMode.System),
            dynamicColor = sp.getBoolean("dynamicColor", false),
            folderLongPress = enumPref("folderLongPress", LongPressAction.Icon),
            accent = sp.getInt("accent", -1),
            folderModel = enumPref("folderModel", FolderModel.Tile),
            homeStyle = enumPref("homeStyle", HomeStyle.Cards),
            fileScale = sp.getFloat("fileScale", 1f).coerceIn(0.6f, 1.8f),
            folderScale = sp.getFloat("folderScale", 1f).coerceIn(0.6f, 1.8f),
            gridCards = sp.getBoolean("gridCards", true),
            cardColor = sp.getInt("cardColor", -1),
        )
    )
    val view: StateFlow<ViewPrefs> = _view.asStateFlow()

    private val _quick = MutableStateFlow(loadQuickAccess())
    val quickAccess: StateFlow<List<String>> = _quick.asStateFlow()

    private val _styles = MutableStateFlow(loadStyles())
    val folderStyles: StateFlow<Map<String, FolderStyle>> = _styles.asStateFlow()

    private val _layouts = MutableStateFlow(loadLayouts())

    /** Folders whose list/grid choice differs from the global default (true = grid). */
    val folderLayouts: StateFlow<Map<String, Boolean>> = _layouts.asStateFlow()

    fun updateView(transform: (ViewPrefs) -> ViewPrefs) {
        val v = transform(_view.value)
        _view.value = v
        sp.edit()
            .putString("sort", v.sort.name)
            .putBoolean("asc", v.ascending)
            .putBoolean("foldersFirst", v.foldersFirst)
            .putBoolean("hidden", v.showHidden)
            .putBoolean("grid", v.grid)
            .putString("itemSize", v.itemSize.name)
            .putBoolean("previews", v.previews)
            .putBoolean("appFolderIcons", v.appFolderIcons)
            .putString("theme", v.theme.name)
            .putBoolean("dynamicColor", v.dynamicColor)
            .putString("folderLongPress", v.folderLongPress.name)
            .putInt("accent", v.accent)
            .putString("folderModel", v.folderModel.name)
            .putString("homeStyle", v.homeStyle.name)
            .putFloat("fileScale", v.fileScale)
            .putFloat("folderScale", v.folderScale)
            .putBoolean("gridCards", v.gridCards)
            .putInt("cardColor", v.cardColor)
            .apply()
    }

    // ---------- quick access ----------
    fun defaultQuickAccess(): List<String> = DEFAULT_QUICK.map { "$rootPath/$it" }

    private fun loadQuickAccess(): List<String> {
        val saved = sp.getString("quick", null)
        if (saved != null) return saved.split('\n').filter { it.isNotBlank() }
        // First run of this version: start from defaults and keep any old bookmarks.
        val old = sp.getStringSet("bookmarks", emptySet())!!.sorted()
        val list = (defaultQuickAccess() + old).distinct()
        sp.edit().putString("quick", list.joinToString("\n")).remove("bookmarks").apply()
        return list
    }

    private fun saveQuick(list: List<String>) {
        _quick.value = list
        sp.edit().putString("quick", list.joinToString("\n")).apply()
    }

    fun addQuickAccess(path: String): Boolean {
        if (path in _quick.value) return false
        saveQuick(_quick.value + path)
        return true
    }

    fun removeQuickAccess(path: String) = saveQuick(_quick.value - path)

    fun resetQuickAccess() = saveQuick(defaultQuickAccess())

    // ---------- folder styles ----------
    private fun loadStyles(): Map<String, FolderStyle> =
        sp.getStringSet("folderStyles", emptySet())!!.mapNotNull { line ->
            val parts = line.split(SEP)
            if (parts.size !in 3..4) {
                null
            } else {
                val model = parts.getOrNull(3)?.let { n -> FolderModel.values().firstOrNull { it.name == n } }
                parts[0] to FolderStyle(parts[1], parts[2].toIntOrNull() ?: -1, model)
            }
        }.toMap()

    private fun saveStyles(map: Map<String, FolderStyle>) {
        _styles.value = map
        sp.edit().putStringSet("folderStyles", map.map { (p, s) -> "$p$SEP${s.icon}$SEP${s.color}$SEP${s.model?.name ?: ""}" }.toSet()).apply()
    }

    fun setFolderStyle(path: String, style: FolderStyle?) {
        val m = _styles.value.toMutableMap()
        if (style == null) m.remove(path) else m[path] = style
        saveStyles(m)
    }

    fun clearFolderStyles() = saveStyles(emptyMap())

    // ---------- per-folder layout ----------
    private fun loadLayouts(): Map<String, Boolean> =
        sp.getStringSet("folderLayouts", emptySet())!!.mapNotNull { line ->
            val parts = line.split(SEP)
            if (parts.size != 2) null else parts[0] to (parts[1] == "1")
        }.toMap()

    private fun saveLayouts(map: Map<String, Boolean>) {
        _layouts.value = map
        sp.edit().putStringSet("folderLayouts", map.map { (p, g) -> "$p$SEP${if (g) "1" else "0"}" }.toSet()).apply()
    }

    /** [grid] null removes the override so the folder follows the default again. */
    fun setFolderLayout(path: String, grid: Boolean?) {
        val m = _layouts.value.toMutableMap()
        if (grid == null) m.remove(path) else m[path] = grid
        saveLayouts(m)
    }

    fun clearFolderLayouts() = saveLayouts(emptyMap())

    /** Keeps quick access entries and custom icons attached to a renamed or moved folder. */
    fun movePath(from: String, to: String) {
        fun map(p: String) = when {
            p == from -> to
            p.startsWith("$from/") -> to + p.removePrefix(from)
            else -> p
        }
        if (_quick.value.any { it == from || it.startsWith("$from/") }) saveQuick(_quick.value.map(::map).distinct())
        if (_styles.value.keys.any { it == from || it.startsWith("$from/") }) {
            saveStyles(_styles.value.mapKeys { (k, _) -> map(k) })
        }
        if (_layouts.value.keys.any { it == from || it.startsWith("$from/") }) {
            saveLayouts(_layouts.value.mapKeys { (k, _) -> map(k) })
        }
    }

    /** Drops quick access entries and custom icons of a deleted folder. */
    fun forgetPath(path: String) {
        fun inside(p: String) = p == path || p.startsWith("$path/")
        if (_quick.value.any(::inside)) saveQuick(_quick.value.filterNot(::inside))
        if (_styles.value.keys.any(::inside)) saveStyles(_styles.value.filterKeys { !inside(it) })
    }

    companion object {
        private const val SEP = "\u0001"
        val DEFAULT_QUICK = listOf("Download", "DCIM", "Pictures", "Documents", "Music", "Movies", "Android/obb")
    }
}
