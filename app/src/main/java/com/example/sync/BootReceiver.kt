package com.example.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = context.getSharedPreferences("phone_sync_prefs", Context.MODE_PRIVATE)
            val wasActive = prefs.getBoolean("watcher_active", true)
            if (wasActive) {
                Log.d("BootReceiver", "Auto-starting ActivityWatcherService on boot")
                try {
                    ActivityWatcherService.start(context)
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Could not start service on boot", e)
                }
            }
        }
    }
}
