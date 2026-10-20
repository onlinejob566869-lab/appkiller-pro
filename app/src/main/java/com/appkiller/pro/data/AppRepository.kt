package com.appkiller.pro.data

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.appkiller.pro.model.AppInfo
import com.appkiller.pro.util.WhitelistManager
import java.util.concurrent.TimeUnit

object AppRepository {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getInstalledApps(showSystem: Boolean = false): List<AppInfo> {
        val pm = appContext.packageManager
        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<AppInfo>()

        for (appInfo in allApps) {
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystem && !showSystem) continue

            val pkg = appInfo.packageName
            if (pkg == appContext.packageName) continue

            val appName = pm.getApplicationLabel(appInfo).toString()
            val icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null }
            val sizeBytes = try {
                java.io.File(appInfo.sourceDir).length()
            } catch (e: Exception) { 0L }

            result.add(
                AppInfo(
                    packageName = pkg,
                    appName = appName,
                    icon = icon,
                    isSystemApp = isSystem,
                    isEnabled = appInfo.enabled,
                    isRunning = isAppRunning(pkg),
                    sizeBytes = sizeBytes,
                    targetSdk = appInfo.targetSdkVersion,
                    isWhitelisted = WhitelistManager.isWhitelisted(appContext, pkg)
                )
            )
        }

        result.sortBy { it.appName.lowercase() }
        return result
    }

    fun getRunningApps(): List<AppInfo> {
        val showSystem = WhitelistManager.showSystemApps(appContext)
        return getInstalledApps(showSystem).filter { it.isRunning && !it.isWhitelisted }
    }

    fun isAppRunning(packageName: String): Boolean {
        return try {
            val usageStatsManager = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - TimeUnit.MINUTES.toMillis(1),
                now
            )
            stats?.any {
                it.packageName == packageName && it.lastTimeUsed > now - TimeUnit.MINUTES.toMillis(1)
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun getMemoryInfo(): MemoryInfo {
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return MemoryInfo(mi.totalMem, mi.availMem, mi.threshold, mi.lowMemory)
    }

    fun killApp(packageName: String): Boolean {
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return try {
            am.killBackgroundProcesses(packageName)
            true
        } catch (e: Exception) {
            false
        }
    }

    data class MemoryInfo(
        val totalMem: Long,
        val availMem: Long,
        val threshold: Long,
        val lowMemory: Boolean
    )
}
