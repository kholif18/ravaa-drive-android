package com.ravaa.drive.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val exp = minOf((ln(bytes.toDouble()) / ln(1024.0)).toInt(), units.size - 1)
    val value = bytes / 1024.0.pow(exp.toDouble())
    return if (exp == 0) "${bytes} B" else String.format(Locale.US, "%.1f %s", value, units[exp])
}

fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        // ISO 8601 dari API, mis. 2026-09-22T11:53:00.000Z
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val date: Date = parser.parse(iso.substring(0, 19)) ?: return ""
        val now = System.currentTimeMillis()
        val diff = now - date.time
        when {
            diff < 60_000 -> "baru saja"
            diff < 3_600_000 -> "${diff / 60_000} mnt lalu"
            diff < 86_400_000 -> "${diff / 3_600_000} jam lalu"
            else -> SimpleDateFormat("d MMM yyyy", Locale("in", "ID")).format(date)
        }
    } catch (e: Exception) {
        ""
    }
}
