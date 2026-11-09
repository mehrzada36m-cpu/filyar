package com.filyar.app.core

import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import java.util.Date
import java.util.Locale

private val faLocale: Locale = Locale.forLanguageTag("fa-IR")
private val jalali = ULocale("fa_IR@calendar=persian")

fun faNum(n: Long): String = String.format(faLocale, "%,d", n)
fun faNum(n: Int): String = faNum(n.toLong())

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "${faNum(bytes)} بایت"
    val units = arrayOf("کیلوبایت", "مگابایت", "گیگابایت", "ترابایت")
    var v = bytes / 1024.0
    var i = 0
    while (v >= 1024 && i < units.size - 1) {
        v /= 1024
        i++
    }
    val pattern = if (v >= 100) "%.0f %s" else "%.1f %s"
    return String.format(faLocale, pattern, v, units[i])
}

fun formatDate(ms: Long): String = SimpleDateFormat("yyyy/MM/dd  HH:mm", jalali).format(Date(ms))

fun formatDateShort(ms: Long): String = SimpleDateFormat("d MMMM yyyy", jalali).format(Date(ms))

/** Natural order: "file2" before "file10", case-insensitive. */
object NaturalOrder : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                val si = i
                val sj = j
                while (i < a.length && a[i].isDigit()) i++
                while (j < b.length && b[j].isDigit()) j++
                val na = a.substring(si, i).trimStart('0')
                val nb = b.substring(sj, j).trimStart('0')
                if (na.length != nb.length) return na.length - nb.length
                val c = na.compareTo(nb)
                if (c != 0) return c
            } else {
                val c = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (c != 0) return c
                i++
                j++
            }
        }
        return (a.length - i) - (b.length - j)
    }
}
