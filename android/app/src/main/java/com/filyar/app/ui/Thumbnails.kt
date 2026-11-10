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
    // About a sixth of the app's heap, so a folder of photos stays in memory when you leave and return.
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 6).toInt().coerceIn(32 shl 20, 160 shl 20)) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val diskWrites = java.util.concurrent.atomic.AtomicInteger()
    @Volatile private var diskDir: File? = null
    private val failed: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private val gate = Semaphore(4)

    /** Thumbnail sizes are rounded to steps of 32 px so small layout changes reuse the same bitmap. */
    fun bucket(px: Int): Int = (((px + 31) / 32) * 32).coerceIn(96, 384)

    fun cached(f: RemoteFile, px: Int): Bitmap? = cache.get(key(f, px))

    private fun baseKey(f: RemoteFile) = "${f.path}:${f.lastModified}:${f.size}"

    private fun key(f: RemoteFile, px: Int) = "${baseKey(f)}@$px"

    fun supports(f: RemoteFile): Boolean = when (kindOf(f)) {
        FileKind.Image -> f.extension != "svg"
        FileKind.Video, FileKind.Apk, FileKind.Audio -> true
        else -> false
    } && baseKey(f) !in failed

    suspend fun load(context: Context, backend: FileBackend, f: RemoteFile, pxWanted: Int): Bitmap? {
        val px = bucket(pxWanted)
        cache.get(key(f, px))?.let { return it }
        if (baseKey(f) in failed) return null
        // A saved copy from an earlier visit is much faster than decoding the photo or video again.
        withContext(Dispatchers.IO) { readDisk(context, f, px) }?.let {
            cache.put(key(f, px), it)
            return it
        }
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
                if (bmp != null) {
                    cache.put(key(f, px), bmp)
                    writeDisk(context, f, px, bmp)
                } else {
                    failed.add(baseKey(f))
                }
                bmp
            }
        }
    }

    private fun diskFile(context: Context, f: RemoteFile, px: Int): File {
        val dir = diskDir ?: File(context.cacheDir, "thumbs").also {
            it.mkdirs()
            diskDir = it
        }
        val hash = java.security.MessageDigest.getInstance("MD5")
            .digest(key(f, px).toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(dir, hash)
    }

    private fun readDisk(context: Context, f: RemoteFile, px: Int): Bitmap? = runCatching {
        val file = diskFile(context, f, px)
        if (!file.exists()) return@runCatching null
        file.setLastModified(System.currentTimeMillis())
        BitmapFactory.decodeFile(file.path)
    }.getOrNull()

    private fun writeDisk(context: Context, f: RemoteFile, px: Int, bmp: Bitmap) {
        runCatching {
            val file = diskFile(context, f, px)
            file.outputStream().use {
                // Icons keep their transparency; photos and frames are smaller as JPEG.
                if (kindOf(f) == FileKind.Apk) bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                else bmp.compress(Bitmap.CompressFormat.JPEG, 85, it)
            }
            if (diskWrites.incrementAndGet() % 60 == 0) trimDisk(file.parentFile)
        }
    }

    /** Keeps the saved thumbnails under about 100 MB by deleting the least recently used. */
    private fun trimDisk(dir: File?) {
        val files = dir?.listFiles() ?: return
        var total = files.sumOf { it.length() }
        if (total <= 150L * 1024 * 1024) return
        for (old in files.sortedBy { it.lastModified() }) {
            if (total <= 100L * 1024 * 1024) break
            total -= old.length()
            old.delete()
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
        if (src.width == px && src.height == px) return src
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
