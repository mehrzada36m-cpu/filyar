package com.filyar.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.filyar.app.RemoteFile
import com.filyar.app.core.FileBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

object Thumbnails {
    private val cache = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val failed: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private val gate = Semaphore(4)

    fun cached(f: RemoteFile): Bitmap? = cache.get(key(f))

    private fun key(f: RemoteFile) = "${f.path}:${f.lastModified}:${f.size}"

    fun supports(f: RemoteFile): Boolean = when (kindOf(f)) {
        FileKind.Image -> f.extension != "svg"
        FileKind.Video, FileKind.Apk, FileKind.Audio -> true
        else -> false
    } && key(f) !in failed

    suspend fun load(context: Context, backend: FileBackend, f: RemoteFile, px: Int): Bitmap? {
        cache.get(key(f))?.let { return it }
        if (key(f) in failed) return null
        return gate.withPermit {
            withContext(Dispatchers.IO) {
                val bmp = runCatching {
                    when (kindOf(f)) {
                        FileKind.Image -> decodeSampled(backend, f.path, px)?.let { centerCrop(it, px) }
                        FileKind.Video -> videoFrame(backend, f.path, px)
                        FileKind.Audio -> audioCover(backend, f.path, px)
                        FileKind.Apk -> apkIcon(context, backend, f, px)
                        else -> null
                    }
                }.getOrNull()
                if (bmp != null) cache.put(key(f), bmp) else failed.add(key(f))
                bmp
            }
        }
    }

    fun decodeSampled(backend: FileBackend, path: String, target: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        backend.openInput(path).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return backend.openInput(path).use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun centerCrop(src: Bitmap, px: Int): Bitmap {
        if (src.width <= px * 2 && src.height <= px * 2) return src
        val out = ThumbnailUtils.extractThumbnail(src, px, px)
        if (out !== src) src.recycle()
        return out
    }

    private fun <T> withRetriever(backend: FileBackend, path: String, block: (MediaMetadataRetriever) -> T?): T? {
        val pfd = backend.openFd(path)
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(pfd.fileDescriptor)
            block(r)
        } finally {
            runCatching { r.release() }
            runCatching { pfd.close() }
        }
    }

    /** A frame from the video, read through the backend so it also works inside Android/data. */
    private fun videoFrame(backend: FileBackend, path: String, px: Int): Bitmap? = withRetriever(backend, path) { r ->
        val frame = r.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: r.frameAtTime
        frame?.let { centerCrop(it, px) }
    }

    private fun audioCover(backend: FileBackend, path: String, px: Int): Bitmap? = withRetriever(backend, path) { r ->
        val bytes = r.embeddedPicture ?: return@withRetriever null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= px && bounds.outHeight / (sample * 2) >= px) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?.let { centerCrop(it, px) }
    }

    private fun apkIcon(context: Context, backend: FileBackend, f: RemoteFile, px: Int): Bitmap? {
        val local = File(f.path)
        var temp: File? = null
        val path = if (local.canRead()) {
            f.path
        } else {
            // Android/data is invisible to the package manager, so read a private copy.
            if (f.size > 300L * 1024 * 1024) return null
            val t = File(context.cacheDir, "apkicon/${System.nanoTime()}.apk").apply { parentFile?.mkdirs() }
            backend.openInput(f.path).use { input -> t.outputStream().use { input.copyTo(it) } }
            temp = t
            t.absolutePath
        }
        try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(path, 0) ?: return null
            val app = info.applicationInfo ?: return null
            app.sourceDir = path
            app.publicSourceDir = path
            return app.loadIcon(pm).toBitmap(px, px)
        } finally {
            temp?.delete()
        }
    }
}
