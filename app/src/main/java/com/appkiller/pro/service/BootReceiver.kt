package com.appkiller.pro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            if (com.appkiller.pro.util.WhitelistManager.isAutoKillEnabled(context)) {
                AutoKillService.startService(context)
            }
        }
    }
}
