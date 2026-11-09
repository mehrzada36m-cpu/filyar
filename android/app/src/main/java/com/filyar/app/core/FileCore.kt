package com.filyar.app.core

import android.os.ParcelFileDescriptor
import android.os.StatFs
import com.filyar.app.RemoteFile
import java.io.File

/**
 * Plain java.io file operations. Runs inside the app process (normal access)
 * and inside the Shizuku service process (shell/root access to Android/data).
 */
object FileCore {

    fun toRemote(f: File): RemoteFile {
        val dir = f.isDirectory
        return RemoteFile(
            path = f.absolutePath,
            name = f.name,
            isDirectory = dir,
            size = if (dir) 0L else f.length(),
            lastModified = f.lastModified(),
            childCount = if (dir) (f.list()?.size ?: -1) else 0,
            canRead = f.canRead(),
            canWrite = f.canWrite(),
        )
    }

    fun listRange(path: String, offset: Int, limit: Int): List<RemoteFile> {
        val dir = File(path)
        if (!dir.exists()) throw IllegalStateException("پوشه پیدا نشد")
        val names = dir.list() ?: throw IllegalStateException("دسترسی به این پوشه ممکن نیست")
        names.sort()
        if (offset >= names.size) return emptyList()
        val end = minOf(names.size, offset + limit)
        val out = ArrayList<RemoteFile>(end - offset)
        for (i in offset until end) out.add(toRemote(File(dir, names[i])))
        return out
    }

    fun listAll(path: String): List<RemoteFile> {
        val dir = File(path)
        if (!dir.exists()) throw IllegalStateException("پوشه پیدا نشد")
        val files = dir.listFiles() ?: throw IllegalStateException("دسترسی به این پوشه ممکن نیست")
        return files.map { toRemote(it) }
    }

    fun stat(path: String): RemoteFile? {
        val f = File(path)
        return if (f.exists()) toRemote(f) else null
    }

    fun mkdirs(path: String): Boolean {
        val f = File(path)
        return f.isDirectory || f.mkdirs()
    }

    fun createFile(path: String): Boolean {
        val f = File(path)
        f.parentFile?.mkdirs()
        return f.createNewFile()
    }

    fun delete(path: String): Boolean {
        val f = File(path)
        if (!f.exists()) return true
        return f.deleteRecursively()
    }

    fun rename(from: String, to: String): Boolean {
        val src = File(from)
        val dst = File(to)
        if (dst.exists()) return false
        return src.renameTo(dst)
    }

    fun open(path: String, mode: String): ParcelFileDescriptor {
        val f = File(path)
        if (mode.contains('w')) f.parentFile?.mkdirs()
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.parseMode(mode))
    }

    fun storage(path: String): LongArray {
        val s = StatFs(path)
        return longArrayOf(s.totalBytes, s.availableBytes)
    }

    fun search(root: String, query: String, limit: Int): List<RemoteFile> {
        val out = ArrayList<RemoteFile>()
        val q = query.trim()
        if (q.isEmpty()) return out
        File(root).walkTopDown().maxDepth(30).forEach { f ->
            if (out.size >= limit) return out
            if (f.absolutePath != root && f.name.contains(q, ignoreCase = true)) out.add(toRemote(f))
        }
        return out
    }

    /** Returns [bytes, files, folders]. */
    fun measure(path: String): LongArray {
        var bytes = 0L
        var files = 0L
        var dirs = 0L
        File(path).walkTopDown().forEach { f ->
            if (f.isDirectory) {
                if (f.absolutePath != path) dirs++
            } else {
                files++
                bytes += f.length()
            }
        }
        return longArrayOf(bytes, files, dirs)
    }

    fun setModified(path: String, time: Long): Boolean = File(path).setLastModified(time)

    /** All files of at least [minSize] bytes below [root], biggest first. */
    fun scan(root: String, minSize: Long, max: Int = 200_000): List<RemoteFile> {
        val out = ArrayList<RemoteFile>()
        for (f in File(root).walkTopDown().onFail { _, _ -> }) {
            if (out.size >= max) break
            if (f.isFile && f.length() >= minSize) out.add(toRemote(f))
        }
        out.sortByDescending { it.size }
        return out
    }
}
