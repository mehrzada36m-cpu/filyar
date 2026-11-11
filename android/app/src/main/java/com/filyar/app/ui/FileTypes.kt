package com.filyar.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.filyar.app.RemoteFile
import com.filyar.app.ui.theme.Filyar

enum class FileKind { Folder, Image, Video, Audio, Archive, Apk, Pdf, Document, Text, Code, Other }

private val imageExt = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "ico")
private val videoExt = setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "m4v", "flv", "ts")
private val audioExt = setOf("mp3", "wav", "ogg", "m4a", "aac", "flac", "opus", "amr", "mid")
private val archiveExt = setOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "obb", "jar")
private val docExt = setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf", "epub")
private val textExt = setOf("txt", "md", "log", "csv", "srt", "ass", "vtt", "nfo", "nomedia")
private val codeExt = setOf(
    "json", "xml", "yml", "yaml", "ini", "cfg", "conf", "properties", "prop", "toml", "html", "htm",
    "css", "js", "ts", "kt", "java", "py", "sh", "c", "cpp", "h", "gradle", "sql", "lua", "smali", "php",
    "rb", "go", "rs", "dart", "swift", "bat", "env", "plist",
)

fun kindOf(f: RemoteFile): FileKind {
    if (f.isDirectory) return FileKind.Folder
    return when (val e = f.extension) {
        in imageExt -> FileKind.Image
        in videoExt -> FileKind.Video
        in audioExt -> FileKind.Audio
        "apk", "apks", "xapk" -> FileKind.Apk
        in archiveExt -> FileKind.Archive
        "pdf" -> FileKind.Pdf
        in docExt -> FileKind.Document
        in textExt -> FileKind.Text
        in codeExt -> FileKind.Code
        else -> if (e.isEmpty() && f.size < 64 * 1024) FileKind.Text else FileKind.Other
    }
}

/** Files the built-in editor should open directly. */
fun isEditable(f: RemoteFile): Boolean = kindOf(f).let { it == FileKind.Text || it == FileKind.Code }

fun isZip(f: RemoteFile): Boolean = !f.isDirectory && f.extension in setOf("zip", "jar", "apks", "xapk")

val FileKind.icon: ImageVector
    get() = when (this) {
        FileKind.Folder -> Icons.Filled.Folder
        FileKind.Image -> Icons.Filled.Image
        FileKind.Video -> Icons.Filled.Movie
        FileKind.Audio -> Icons.Filled.Audiotrack
        FileKind.Archive -> Icons.Filled.FolderZip
        FileKind.Apk -> Icons.Filled.Android
        FileKind.Pdf -> Icons.Filled.PictureAsPdf
        FileKind.Document -> Icons.Filled.Description
        FileKind.Text -> Icons.AutoMirrored.Filled.TextSnippet
        FileKind.Code -> Icons.Filled.Code
        FileKind.Other -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

val FileKind.tint: Color
    @Composable get() {
        val t = Filyar.tokens
        return when (this) {
            FileKind.Folder -> t.folder
            FileKind.Image -> t.image
            FileKind.Video -> t.video
            FileKind.Audio -> t.audio
            FileKind.Archive -> t.archive
            FileKind.Apk -> t.apk
            FileKind.Pdf -> t.image
            FileKind.Document -> t.document
            FileKind.Text -> t.text
            FileKind.Code -> t.code
            FileKind.Other -> t.other
        }
    }

val FileKind.label: String
    get() = when (this) {
        FileKind.Folder -> "پوشه"
        FileKind.Image -> "تصویر"
        FileKind.Video -> "ویدیو"
        FileKind.Audio -> "صوت"
        FileKind.Archive -> "فایل فشرده"
        FileKind.Apk -> "برنامه اندروید"
        FileKind.Pdf -> "PDF"
        FileKind.Document -> "سند"
        FileKind.Text -> "متن"
        FileKind.Code -> "کد"
        FileKind.Other -> "فایل"
    }

/** A backup made by "backup app data". */
fun isRestorable(f: RemoteFile): Boolean = !f.isDirectory && f.name.endsWith(".filyar.zip")

/** A package folder directly inside Android/data or Android/obb. */
fun isAppDataDir(f: RemoteFile, root: String): Boolean =
    f.isDirectory && '.' in f.name && (f.parent == "$root/Android/data" || f.parent == "$root/Android/obb")
