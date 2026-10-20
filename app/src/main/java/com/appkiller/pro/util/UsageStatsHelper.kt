package com.appkiller.pro.util

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.concurrent.TimeUnit

object UsageStatsHelper {

    data class AppUsageInfo(
        val packageName: String,
        val appName: String,
        val totalTimeMs: Long,
        val lastTimeUsed: Long
    )

    fun getUsageStats(context: Context, hoursBack: Int = 24): List<AppUsageInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - TimeUnit.HOURS.toMillis(hoursBack.toLong()),
            now
        ) ?: return emptyList()

        val pm = context.packageManager
        return stats.mapNotNull { s ->
            try {
                val ai = pm.getApplicationInfo(s.packageName, 0)
                val appName = pm.getApplicationLabel(ai).toString()
                AppUsageInfo(s.packageName, appName, s.totalTimeInForeground, s.lastTimeUsed)
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.totalTimeMs }
    }

    fun getRunningAppCount(context: Context): Int = getUsageStats(context).size

    private fun formatDuration(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return when {
            hours > 0 -> "$hours h $minutes min"
            minutes > 0 -> "$minutes min $seconds sec"
            else -> "$seconds sec"
        }
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} min ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} h ago"
            else -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
        }
    }
}
