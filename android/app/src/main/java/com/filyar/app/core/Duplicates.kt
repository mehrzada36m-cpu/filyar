package com.filyar.app.core

import com.filyar.app.RemoteFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.InputStream
import java.security.MessageDigest

/** Finds files with identical content: same size first, then a content fingerprint. */
object Duplicates {
    private const val CHUNK = 1024 * 1024

    suspend fun find(
        b: FileBackend,
        files: List<RemoteFile>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): List<List<RemoteFile>> {
        val candidates = files.filter { it.size > 0 }.groupBy { it.size }.values.filter { it.size > 1 }
        val total = candidates.sumOf { it.size }
        var done = 0
        val groups = ArrayList<List<RemoteFile>>()
        for (sameSize in candidates) {
            val byPrint = HashMap<String, MutableList<RemoteFile>>()
            for (f in sameSize) {
                currentCoroutineContext().ensureActive()
                val print = runCatching { fingerprint(b, f) }.getOrNull()
                if (print != null) byPrint.getOrPut(print) { ArrayList() }.add(f)
                done++
                onProgress(done, total)
            }
            byPrint.values.filter { it.size > 1 }.forEach { g -> groups.add(g.sortedBy { it.lastModified }) }
        }
        // Groups that free the most space come first.
        return groups.sortedByDescending { it[0].size * (it.size - 1) }
    }

    /** Small files are hashed fully; big ones by their first and last megabyte plus size. */
    fun fingerprint(b: FileBackend, f: RemoteFile): String {
        val md = MessageDigest.getInstance("MD5")
        b.openInput(f.path).use { input ->
            val buf = ByteArray(64 * 1024)
            if (f.size <= CHUNK * 2L) {
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                }
            } else {
                readInto(input, md, buf, CHUNK)
                skipFully(input, f.size - CHUNK * 2L)
                readInto(input, md, buf, CHUNK)
            }
        }
        md.update(f.size.toString().toByteArray())
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun readInto(input: InputStream, md: MessageDigest, buf: ByteArray, count: Int) {
        var left = count
        while (left > 0) {
            val n = input.read(buf, 0, minOf(buf.size, left))
            if (n < 0) break
            md.update(buf, 0, n)
            left -= n
        }
    }

    private fun skipFully(input: InputStream, count: Long) {
        var left = count
        while (left > 0) {
            val n = input.skip(left)
            if (n <= 0) {
                if (input.read() < 0) return
                left--
            } else {
                left -= n
            }
        }
    }
}
