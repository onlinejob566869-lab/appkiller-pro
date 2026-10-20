package com.appkiller.pro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.appkiller.pro.AppKillerApp
import com.appkiller.pro.R
import com.appkiller.pro.util.KillCounter
import com.appkiller.pro.util.WhitelistManager

class AutoKillService : Service() {

    companion object {
        private const val CHANNEL_ID = "auto_kill_channel"
        private const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, AutoKillService::class.java)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, AutoKillService::class.java))
        }
    }

    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                performAutoKill()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    private fun performAutoKill() {
        if (!AppKillerApp.isAccessibilityEnabled(this)) return
        if (!WhitelistManager.isAutoKillEnabled(this)) return

        val runningApps = com.appkiller.pro.data.AppRepository.getRunningApps()
        val packages = runningApps.map { it.packageName }
        val killed = AppKillerAccessibilityService.killPackages(packages)
        KillCounter.incrementKills(killed)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Auto-Kill Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors screen state to auto-kill apps"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AppKiller Pro")
            .setContentText("Auto-Kill is active — ${KillCounter.getTotalKills()} kills total")
            .setSmallIcon(R.drawable.ic_kill)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(screenReceiver) } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
