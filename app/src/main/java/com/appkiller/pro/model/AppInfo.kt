package com.appkiller.pro.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val isRunning: Boolean = false,
    val sizeBytes: Long = 0L,
    val targetSdk: Int = 0,
    var isSelected: Boolean = false,
    var isWhitelisted: Boolean = false
) {
    companion object {
        fun formatSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            return "%.1f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[Math.min(digitGroups, units.size - 1)])
        }
    }

    val sizeFormatted: String
        get() = formatSize(sizeBytes)
}
