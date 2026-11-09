package com.filyar.app

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.annotation.Keep
import com.filyar.app.core.FileCore
import kotlin.system.exitProcess

/**
 * Runs in a separate process started by Shizuku with shell (adb) or root
 * privileges, which can read and write Android/data and Android/obb.
 */
class FileService : IFileService.Stub {

    @Keep
    constructor() : super()

    @Keep
    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context) : super()

    // Only a few exception types survive a Binder call; convert everything else.
    private inline fun <T> safe(block: () -> T): T = try {
        block()
    } catch (e: IllegalStateException) {
        throw e
    } catch (e: SecurityException) {
        throw e
    } catch (e: Throwable) {
        throw IllegalStateException(e.message ?: e.javaClass.simpleName)
    }

    override fun destroy() {
        exitProcess(0)
    }

    override fun listRange(path: String, offset: Int, limit: Int): List<RemoteFile> =
        safe { FileCore.listRange(path, offset, limit) }

    override fun stat(path: String): RemoteFile? = safe { FileCore.stat(path) }

    override fun mkdirs(path: String): Boolean = safe { FileCore.mkdirs(path) }

    override fun createFile(path: String): Boolean = safe { FileCore.createFile(path) }

    override fun delete(path: String): Boolean = safe { FileCore.delete(path) }

    override fun rename(from: String, to: String): Boolean = safe { FileCore.rename(from, to) }

    override fun open(path: String, mode: String): ParcelFileDescriptor = safe { FileCore.open(path, mode) }

    override fun storage(path: String): LongArray = safe { FileCore.storage(path) }

    override fun search(root: String, query: String, limit: Int): List<RemoteFile> =
        safe { FileCore.search(root, query, limit) }

    override fun measure(path: String): LongArray = safe { FileCore.measure(path) }

    override fun setModified(path: String, time: Long): Boolean = safe { FileCore.setModified(path, time) }

    // A full-storage scan is expensive, so the first page runs it and later pages reuse the result.
    private var scanKey: String? = null
    private var scanCache: List<RemoteFile> = emptyList()

    @Synchronized
    override fun scanRange(root: String, minSize: Long, offset: Int, limit: Int): List<RemoteFile> = safe {
        val key = "$root|$minSize"
        if (offset == 0 || scanKey != key) {
            scanCache = FileCore.scan(root, minSize)
            scanKey = key
        }
        val from = offset.coerceIn(0, scanCache.size)
        val to = (offset + limit).coerceIn(from, scanCache.size)
        ArrayList(scanCache.subList(from, to))
    }
}
