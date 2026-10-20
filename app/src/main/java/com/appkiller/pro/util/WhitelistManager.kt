package com.appkiller.pro.util

import android.content.Context
import androidx.preference.PreferenceManager

object WhitelistManager {
    private const val KEY_WHITELIST = "whitelist_packages"
    private const val KEY_AUTO_KILL = "auto_kill_enabled"
    private const val KEY_SHOW_SYSTEM = "show_system_apps"
    private const val KEY_SORT_BY = "sort_by"

    private fun prefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

    fun getWhitelist(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_WHITELIST, emptySet()) ?: emptySet()

    fun addToWhitelist(context: Context, packageName: String) {
        val current = getWhitelist(context).toMutableSet()
        current.add(packageName)
        prefs(context).edit().putStringSet(KEY_WHITELIST, current).apply()
    }

    fun removeFromWhitelist(context: Context, packageName: String) {
        val current = getWhitelist(context).toMutableSet()
        current.remove(packageName)
        prefs(context).edit().putStringSet(KEY_WHITELIST, current).apply()
    }

    fun isWhitelisted(context: Context, packageName: String): Boolean =
        getWhitelist(context).contains(packageName)

    fun isAutoKillEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_KILL, false)

    fun setAutoKillEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_KILL, enabled).apply()
    }

    fun showSystemApps(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_SYSTEM, false)

    fun setShowSystemApps(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_SYSTEM, show).apply()
    }

    fun getSortBy(context: Context): String =
        prefs(context).getString(KEY_SORT_BY, "name") ?: "name"

    fun setSortBy(context: Context, sortBy: String) {
        prefs(context).edit().putString(KEY_SORT_BY, sortBy).apply()
    }
}
