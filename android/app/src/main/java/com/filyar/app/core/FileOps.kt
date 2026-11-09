package com.filyar.app.core

import com.filyar.app.RemoteFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun joinPath(dir: String, name: String): String = if (dir.endsWith("/")) dir + name else "$dir/$name"

fun parentPath(path: String): String = path.substringBeforeLast('/', "/").ifEmpty { "/" }

/** Reports progress of a long operation, throttled for the UI. */
class Progress(
    private val total: Long,
    private val onUpdate: (current: String, fraction: Float?) -> Unit,
) {
    private var done = 0L
    private var current = ""
    private var lastPush = 0L

    fun file(name: String) {
        current = name
        push(force = true)
    }

    fun add(bytes: Long) {
        done += bytes
        push(force = false)
    }

    private fun push(force: Boolean) {
        val now = System.nanoTime()
        if (!force && now - lastPush < 80_000_000L) return
        lastPush = now
        onUpdate(current, if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else null)
    }
}

object FileOps {

    fun uniqueName(b: FileBackend, dir: String, name: String): String {
        if (b.stat(joinPath(dir, name)) == null) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var i = 1
        while (true) {
            val candidate = "$base ($i)$ext"
            if (b.stat(joinPath(dir, candidate)) == null) return candidate
            i++
        }
    }

    fun totalSize(b: FileBackend, items: List<RemoteFile>): Long =
        items.sumOf { if (it.isDirectory) runCatching { b.measure(it.path)[0] }.getOrDefault(0L) else it.size }

    private fun checkNotInside(src: RemoteFile, destDir: String) {
        if (src.isDirectory && (destDir == src.path || destDir.startsWith(src.path + "/"))) {
            throw IOException("نمی‌توان پوشه را داخل خودش قرار داد")
        }
    }

    suspend fun copy(b: FileBackend, src: RemoteFile, destDir: String, p: Progress) {
        checkNotInside(src, destDir)
        val target = joinPath(destDir, uniqueName(b, destDir, src.name))
        copyInto(b, src, target, p)
    }

    /** Moves [src] into [destDir] and returns its new path (null when it was already there). */
    suspend fun move(b: FileBackend, src: RemoteFile, destDir: String, p: Progress): String? {
        if (src.parent == destDir) return null
        checkNotInside(src, destDir)
        val target = joinPath(destDir, uniqueName(b, destDir, src.name))
        if (b.rename(src.path, target)) return target
        copyInto(b, src, target, p)
        if (!b.delete(src.path)) throw IOException("حذف فایل مبدا ممکن نشد: ${src.name}")
        return target
    }

    private suspend fun copyInto(b: FileBackend, src: RemoteFile, target: String, p: Progress) {
        currentCoroutineContext().ensureActive()
        if (src.isDirectory) {
            if (!b.mkdirs(target)) throw IOException("ساخت پوشه ممکن نشد: ${src.name}")
            for (child in b.list(src.path)) copyInto(b, child, joinPath(target, child.name), p)
        } else {
            p.file(src.name)
            b.openInput(src.path).use { input ->
                b.openOutput(target).use { output -> pipe(input, output, p) }
            }
            runCatching { b.setModified(target, src.lastModified) }
        }
    }

    private suspend fun pipe(input: InputStream, output: OutputStream, p: Progress) {
        val buf = ByteArray(256 * 1024)
        while (true) {
            currentCoroutineContext().ensureActive()
            val n = input.read(buf)
            if (n < 0) break
            output.write(buf, 0, n)
            p.add(n.toLong())
        }
        output.flush()
    }

    suspend fun delete(b: FileBackend, items: List<RemoteFile>, p: Progress) {
        for (item in items) {
            currentCoroutineContext().ensureActive()
            p.file(item.name)
            if (!b.delete(item.path)) throw IOException("حذف ممکن نشد: ${item.name}")
        }
    }

    suspend fun compress(b: FileBackend, items: List<RemoteFile>, zipPath: String, p: Progress) {
        ZipOutputStream(BufferedOutputStream(b.openOutput(zipPath))).use { zos ->
            for (item in items) addEntry(b, zos, item, item.name, p)
        }
    }

    private suspend fun addEntry(b: FileBackend, zos: ZipOutputStream, f: RemoteFile, entry: String, p: Progress) {
        currentCoroutineContext().ensureActive()
        if (f.isDirectory) {
            zos.putNextEntry(ZipEntry("$entry/").apply { time = f.lastModified })
            zos.closeEntry()
            for (child in b.list(f.path)) addEntry(b, zos, child, "$entry/${child.name}", p)
        } else {
            p.file(f.name)
            zos.putNextEntry(ZipEntry(entry).apply { time = f.lastModified })
            b.openInput(f.path).use { input -> pipe(input, zos, p) }
            zos.closeEntry()
        }
    }

    suspend fun extract(b: FileBackend, zip: RemoteFile, destDir: String, p: Progress): String {
        val folderName = uniqueName(b, destDir, zip.name.substringBeforeLast('.').ifEmpty { "extracted" })
        val root = joinPath(destDir, folderName)
        if (!b.mkdirs(root)) throw IOException("ساخت پوشه ممکن نشد")
        ZipInputStream(BufferedInputStream(b.openInput(zip.path))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                currentCoroutineContext().ensureActive()
                val parts = entry.name.replace('\\', '/').split('/').filter { it.isNotEmpty() && it != "." }
                // Skip entries that try to escape the target folder.
                if (parts.isNotEmpty() && parts.none { it == ".." }) {
                    val out = joinPath(root, parts.joinToString("/"))
                    if (entry.isDirectory) {
                        b.mkdirs(out)
                    } else {
                        b.mkdirs(parentPath(out))
                        p.file(parts.last())
                        b.openOutput(out).use { output -> pipe(zis, output, p) }
                        if (entry.time > 0) runCatching { b.setModified(out, entry.time) }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return root
    }
}
