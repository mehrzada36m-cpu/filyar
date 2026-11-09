package com.filyar.app

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RemoteFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val childCount: Int,
    val canRead: Boolean,
    val canWrite: Boolean,
) : Parcelable {
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "").lowercase()

    val parent: String
        get() = path.substringBeforeLast('/', "/").ifEmpty { "/" }

    val isHidden: Boolean
        get() = name.startsWith(".")
}
