package com.appkiller.pro.util

import android.content.Context
import androidx.preference.PreferenceManager

object KillCounter {
    private const val KEY_TOTAL_KILLS = "total_kills"
    private const val KEY_MEMORY_FREED = "memory_freed_bytes"
    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun getTotalKills(): Int = prefs.getInt(KEY_TOTAL_KILLS, 0)

    fun incrementKills(count: Int = 1) {
        prefs.edit().putInt(KEY_TOTAL_KILLS, getTotalKills() + count).apply()
    }

    fun getMemoryFreed(): Long = prefs.getLong(KEY_MEMORY_FREED, 0L)

    fun addMemoryFreed(bytes: Long) {
        prefs.edit().putLong(KEY_MEMORY_FREED, getMemoryFreed() + bytes).apply()
    }

    fun reset() {
        prefs.edit().putInt(KEY_TOTAL_KILLS, 0).putLong(KEY_MEMORY_FREED, 0L).apply()
    }
}
