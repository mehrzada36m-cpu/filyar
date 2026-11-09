package com.filyar.app.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap

object FolderIcons {
    /** Icons the user can pick for a folder (key is what gets saved). */
    val choices: List<Pair<String, ImageVector>> = listOf(
        "folder" to Icons.Filled.Folder,
        "special" to Icons.Filled.FolderSpecial,
        "download" to Icons.Filled.Download,
        "music" to Icons.Filled.MusicNote,
        "image" to Icons.Filled.Image,
        "gallery" to Icons.Filled.PhotoLibrary,
        "camera" to Icons.Filled.CameraAlt,
        "movie" to Icons.Filled.Movie,
        "videos" to Icons.Filled.VideoLibrary,
        "videocam" to Icons.Filled.Videocam,
        "doc" to Icons.Filled.Description,
        "book" to Icons.AutoMirrored.Filled.MenuBook,
        "android" to Icons.Filled.Android,
        "apps" to Icons.Filled.Apps,
        "game" to Icons.Filled.SportsEsports,
        "gamepad" to Icons.Filled.Gamepad,
        "chat" to Icons.AutoMirrored.Filled.Chat,
        "send" to Icons.AutoMirrored.Filled.Send,
        "mic" to Icons.Filled.Mic,
        "headphones" to Icons.Filled.Headphones,
        "podcast" to Icons.Filled.Podcasts,
        "screenshot" to Icons.Filled.Screenshot,
        "wallpaper" to Icons.Filled.Wallpaper,
        "work" to Icons.Filled.Work,
        "school" to Icons.Filled.School,
        "favorite" to Icons.Filled.Favorite,
        "star" to Icons.Filled.Star,
        "pin" to Icons.Filled.PushPin,
        "home" to Icons.Filled.Home,
        "person" to Icons.Filled.Person,
        "groups" to Icons.Filled.Groups,
        "lock" to Icons.Filled.Lock,
        "security" to Icons.Filled.Security,
        "cloud" to Icons.Filled.Cloud,
        "backup" to Icons.Filled.Backup,
        "archive" to Icons.Filled.Archive,
        "storage" to Icons.Filled.Storage,
        "code" to Icons.Filled.Code,
        "build" to Icons.Filled.Build,
        "settings" to Icons.Filled.Settings,
        "font" to Icons.Filled.FontDownload,
        "palette" to Icons.Filled.Palette,
        "shopping" to Icons.Filled.ShoppingCart,
        "payments" to Icons.Filled.Payments,
        "receipt" to Icons.Filled.Receipt,
        "travel" to Icons.Filled.Flight,
        "map" to Icons.Filled.Map,
        "pets" to Icons.Filled.Pets,
        "attachment" to Icons.Filled.Attachment,
        "bluetooth" to Icons.Filled.Bluetooth,
        "cache" to Icons.Filled.Cached,
        "trash" to Icons.Filled.Delete,
    )

    private val byKeyMap = choices.toMap()

    fun byKey(key: String): ImageVector? = byKeyMap[key]

    /** Colours a folder can be painted with; index is what gets saved. */
    val palette: List<Color> = listOf(
        Color(0xFF0F766E), Color(0xFF0891B2), Color(0xFF2563EB), Color(0xFF7C3AED), Color(0xFFDB2777),
        Color(0xFFE11D48), Color(0xFFEA580C), Color(0xFFD97706), Color(0xFF16A34A), Color(0xFF64748B),
    )

    fun colorAt(index: Int): Color? = palette.getOrNull(index)

    private fun norm(name: String) = name.lowercase().filter { it.isLetterOrDigit() }

    private val builtin: Map<String, ImageVector> = buildMap {
        fun put(icon: ImageVector, vararg names: String) = names.forEach { put(it, icon) }
        put(Icons.Filled.Download, "download", "downloads", "دانلود", "دانلودها")
        put(Icons.Filled.MusicNote, "music", "musics", "audio", "audios", "sounds", "songs", "موسیقی", "آهنگ")
        put(Icons.Filled.RingVolume, "ringtones", "ringtone")
        put(Icons.Filled.Notifications, "notifications")
        put(Icons.Filled.Alarm, "alarms")
        put(Icons.Filled.Podcasts, "podcasts")
        put(Icons.Filled.Headphones, "audiobooks")
        put(Icons.Filled.Mic, "recordings", "recording", "recorder", "soundrecorder", "voicerecorder", "callrecordings", "voice", "voices")
        put(Icons.Filled.CameraAlt, "dcim", "camera")
        put(Icons.Filled.Image, "pictures", "picture", "images", "image", "photos", "photo", "تصاویر", "عکس")
        put(Icons.Filled.Screenshot, "screenshots", "screenshot", "screenrecordings", "screenrecorder")
        put(Icons.Filled.Movie, "movies", "movie", "videos", "video", "films", "فیلم")
        put(Icons.Filled.Description, "documents", "document", "docs", "اسناد")
        put(Icons.Filled.Android, "android", "apk", "apks")
        put(Icons.Filled.Apps, "data")
        put(Icons.Filled.Gamepad, "obb", "games", "game")
        put(Icons.Filled.PhotoLibrary, "media")
        put(Icons.Filled.Bluetooth, "bluetooth")
        put(Icons.Filled.Backup, "backup", "backups")
        put(Icons.Filled.FontDownload, "fonts", "font")
        put(Icons.AutoMirrored.Filled.MenuBook, "books", "ebooks", "book")
        put(Icons.Filled.Wallpaper, "wallpapers", "wallpaper")
        put(Icons.Filled.Cached, "thumbnails", "cache", "caches", "temp", "tmp")
        put(Icons.Filled.Delete, "trash", "recyclebin")
    }

    /** Icon chosen from the folder's name, e.g. Download → download icon. */
    fun builtinFor(name: String): ImageVector? = builtin[norm(name)]

    /** Fallback icons for well-known apps that are not installed. */
    private val appFallback: Map<String, ImageVector> = mapOf(
        "telegram" to Icons.AutoMirrored.Filled.Send,
        "telegramx" to Icons.AutoMirrored.Filled.Send,
        "whatsapp" to Icons.AutoMirrored.Filled.Chat,
        "whatsappbusiness" to Icons.AutoMirrored.Filled.Chat,
        "instagram" to Icons.Filled.PhotoCamera,
        "rubika" to Icons.AutoMirrored.Filled.Chat,
        "eitaa" to Icons.AutoMirrored.Filled.Chat,
        "bale" to Icons.AutoMirrored.Filled.Chat,
        "soroush" to Icons.AutoMirrored.Filled.Chat,
        "viber" to Icons.AutoMirrored.Filled.Chat,
    )

    fun appFallbackFor(name: String): ImageVector? = appFallback[norm(name)]

    internal fun normalized(name: String) = norm(name)
}

/** Finds the installed app that a folder belongs to and loads its icon. */
object AppIcons {
    private val known = mapOf(
        "telegram" to "org.telegram.messenger",
        "telegramx" to "org.thunderdog.challegram",
        "whatsapp" to "com.whatsapp",
        "whatsappbusiness" to "com.whatsapp.w4b",
        "instagram" to "com.instagram.android",
        "rubika" to "ir.resaneh1.iptv",
        "eitaa" to "ir.eitaa.messenger",
        "bale" to "ir.nasim",
        "soroush" to "mobi.mmdt.ottplus",
        "viber" to "com.viber.voip",
        "snapchat" to "com.snapchat.android",
        "tiktok" to "com.zhiliaoapp.musically",
        "spotify" to "com.spotify.music",
        "capcut" to "com.lemon.lvoverseas",
        "xender" to "cn.xender",
        "shareit" to "com.lenovo.anyshare.gps",
        "zarchiver" to "ru.zdevs.zarchiver",
        "mxplayer" to "com.mxtech.videoplayer.ad",
        "vlc" to "org.videolan.vlc",
        "facebook" to "com.facebook.katana",
        "messenger" to "com.facebook.orca",
        "skype" to "com.skype.raider",
        "imo" to "com.imo.android.imoim",
        "signal" to "org.thoughtcrime.securesms",
        "discord" to "com.discord",
        "pubgmobile" to "com.tencent.ig",
        "inshot" to "com.camerasideas.instashot",
        "kinemaster" to "com.nexstreaming.app.kinemasterfree",
    )

    private val packageRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")
    private const val NONE = "\u0000"
    private val lookups = ConcurrentHashMap<String, String>()
    @Volatile private var labels: Map<String, String>? = null
    private val icons = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private fun installed(pm: PackageManager, pkg: String) = try {
        pm.getApplicationInfo(pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun labelMap(context: Context): Map<String, String> {
        labels?.let { return it }
        val pm = context.packageManager
        val map = HashMap<String, String>()
        runCatching {
            for (app in pm.getInstalledApplications(0)) {
                val label = FolderIcons.normalized(app.loadLabel(pm).toString())
                if (label.length >= 3 && label !in map) map[label] = app.packageName
            }
        }
        labels = map
        return map
    }

    /** Package of the app a folder is named after, or null. Blocking: call off the main thread. */
    fun packageFor(context: Context, name: String): String? {
        lookups[name]?.let { return it.takeIf { v -> v != NONE } }
        val pm = context.packageManager
        val n = FolderIcons.normalized(name)
        val found = when {
            packageRegex.matches(name) && installed(pm, name) -> name
            known[n]?.let { installed(pm, it) } == true -> known[n]
            else -> labelMap(context)[n]
        }
        lookups[name] = found ?: NONE
        return found
    }

    fun cached(name: String, px: Int): Bitmap? {
        val pkg = lookups[name]?.takeIf { it != NONE } ?: return null
        return icons.get("$pkg:$px")
    }

    /** Icon of the app that owns a folder, or null. Blocking: call off the main thread. */
    fun folderIcon(context: Context, name: String, px: Int): Bitmap? {
        val pkg = packageFor(context, name) ?: return null
        icons.get("$pkg:$px")?.let { return it }
        val bmp = runCatching { context.packageManager.getApplicationIcon(pkg).toBitmap(px, px) }.getOrNull() ?: return null
        icons.put("$pkg:$px", bmp)
        return bmp
    }
}
