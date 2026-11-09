package com.filyar.app.core

import android.os.ParcelFileDescriptor
import com.filyar.app.IFileService
import com.filyar.app.RemoteFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

interface FileBackend {
    /** True when running through Shizuku (can see Android/data). */
    val isPrivileged: Boolean
    fun list(path: String): List<RemoteFile>
    fun stat(path: String): RemoteFile?
    fun mkdirs(path: String): Boolean
    fun createFile(path: String): Boolean
    fun delete(path: String): Boolean
    fun rename(from: String, to: String): Boolean
    fun openInput(path: String): InputStream
    fun openOutput(path: String): OutputStream
    fun storage(path: String): LongArray
    fun search(root: String, query: String, limit: Int = 500): List<RemoteFile>
    fun measure(path: String): LongArray
    fun setModified(path: String, time: Long): Boolean
    /** Seekable read-only descriptor (used for video frames and audio covers). */
    fun openFd(path: String): ParcelFileDescriptor
    /** Every file under [root] that is at least [minSize] bytes, biggest first. */
    fun scan(root: String, minSize: Long): List<RemoteFile>
}

object LocalBackend : FileBackend {
    override val isPrivileged = false
    override fun list(path: String) = FileCore.listAll(path)
    override fun stat(path: String) = FileCore.stat(path)
    override fun mkdirs(path: String) = FileCore.mkdirs(path)
    override fun createFile(path: String) = FileCore.createFile(path)
    override fun delete(path: String) = FileCore.delete(path)
    override fun rename(from: String, to: String) = FileCore.rename(from, to)
    override fun openInput(path: String): InputStream = FileInputStream(File(path))
    override fun openOutput(path: String): OutputStream {
        val f = File(path)
        f.parentFile?.mkdirs()
        return FileOutputStream(f)
    }
    override fun storage(path: String) = FileCore.storage(path)
    override fun search(root: String, query: String, limit: Int) = FileCore.search(root, query, limit)
    override fun measure(path: String) = FileCore.measure(path)
    override fun setModified(path: String, time: Long) = FileCore.setModified(path, time)
    override fun openFd(path: String): ParcelFileDescriptor =
        ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
    override fun scan(root: String, minSize: Long) = FileCore.scan(root, minSize)
}

class ShizukuBackend(private val service: IFileService) : FileBackend {
    override val isPrivileged = true

    override fun list(path: String): List<RemoteFile> {
        // Page through results so large folders never exceed the Binder size limit.
        val out = ArrayList<RemoteFile>()
        var offset = 0
        while (true) {
            val chunk = service.listRange(path, offset, PAGE) ?: break
            out.addAll(chunk)
            if (chunk.size < PAGE) break
            offset += chunk.size
        }
        return out
    }

    override fun stat(path: String): RemoteFile? = service.stat(path)
    override fun mkdirs(path: String) = service.mkdirs(path)
    override fun createFile(path: String) = service.createFile(path)
    override fun delete(path: String) = service.delete(path)
    override fun rename(from: String, to: String) = service.rename(from, to)

    override fun openInput(path: String): InputStream {
        val pfd = service.open(path, "r") ?: throw IOException("باز کردن فایل ممکن نشد")
        return ParcelFileDescriptor.AutoCloseInputStream(pfd)
    }

    override fun openOutput(path: String): OutputStream {
        val pfd = service.open(path, "w") ?: throw IOException("نوشتن فایل ممکن نشد")
        return ParcelFileDescriptor.AutoCloseOutputStream(pfd)
    }

    override fun storage(path: String): LongArray = service.storage(path)
    override fun search(root: String, query: String, limit: Int): List<RemoteFile> =
        service.search(root, query, limit) ?: emptyList()
    override fun measure(path: String): LongArray = service.measure(path)
    override fun setModified(path: String, time: Long) = service.setModified(path, time)
    override fun openFd(path: String): ParcelFileDescriptor =
        service.open(path, "r") ?: throw IOException("باز کردن فایل ممکن نشد")

    override fun scan(root: String, minSize: Long): List<RemoteFile> {
        val out = ArrayList<RemoteFile>()
        var offset = 0
        while (true) {
            val chunk = service.scanRange(root, minSize, offset, PAGE) ?: break
            out.addAll(chunk)
            if (chunk.size < PAGE) break
            offset += chunk.size
        }
        return out
    }

    private companion object {
        const val PAGE = 200
    }
}
