package com.appkiller.pro

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import android.app.Application

class AppKillerApp : Application() {

    companion object {
        private var instance: AppKillerApp? = null

        fun getInstance(): AppKillerApp = instance!!

        fun isAccessibilityEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            )
            if (enabled == 1) {
                val services = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false
                val serviceName = "${context.packageName}/.service.AppKillerAccessibilityService"
                return services.contains(serviceName)
            }
            return false
        }

        fun isPremium(context: Context): Boolean = true

        fun hasUsageStatsPermission(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    "android:get_usage_stats",
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    "android:get_usage_stats",
                    Process.myUid(),
                    context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        KillCounter.init(this)
        AppRepository.init(this)
    }
}
