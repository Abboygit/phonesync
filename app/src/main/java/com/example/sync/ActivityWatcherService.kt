package com.example.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CallLog
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.PhoneSyncApp
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ActivityWatcherService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var smsObserver: ContentObserver? = null
    private var callObserver: ContentObserver? = null
    private var debounceJob: Job? = null
    private var periodicJob: Job? = null

    private val repository by lazy { (application as PhoneSyncApp).repository }

    companion object {
        const val CHANNEL_ID = "phone_sync_channel"
        const val NOTIFICATION_ID = 1001

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, ActivityWatcherService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ActivityWatcherService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startAsForeground()
        registerObservers()
        startPeriodicFallbackCheck()
        _isServiceRunning.value = true
        repository.setWatcherActive(true)
        Log.d("ActivityWatcherService", "Service created and observers registered")

        // Initial diff run
        serviceScope.launch {
            try {
                repository.runSnapshotDiff(isFirstRun = false)
            } catch (e: Exception) {
                Log.w("ActivityWatcherService", "Initial diff failed", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Activity Sync Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors real-time SMS and call log activity for cross-device sync"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startAsForeground() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Phone Sync Active")
            .setContentText("Monitoring SMS and Call log for cross-device sync")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun registerObservers() {
        val handler = Handler(Looper.getMainLooper())

        smsObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                Log.d("ActivityWatcherService", "SMS ContentObserver fired: $uri")
                triggerDebouncedDiff("SMS change observed")
            }
        }

        callObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                Log.d("ActivityWatcherService", "CallLog ContentObserver fired: $uri")
                triggerDebouncedDiff("Call log change observed")
            }
        }

        try {
            contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                smsObserver!!
            )
        } catch (e: SecurityException) {
            Log.w("ActivityWatcherService", "Missing READ_SMS permission for observer", e)
        }

        try {
            contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                callObserver!!
            )
        } catch (e: SecurityException) {
            Log.w("ActivityWatcherService", "Missing READ_CALL_LOG permission for observer", e)
        }
    }

    private fun triggerDebouncedDiff(reason: String) {
        debounceJob?.cancel()
        debounceJob = serviceScope.launch {
            delay(1500) // 1.5s debounce to consolidate multi-part SMS or rapid events
            try {
                Log.d("ActivityWatcherService", "Executing debounced diff. Reason: $reason")
                repository.runSnapshotDiff(isFirstRun = false)
            } catch (e: Exception) {
                Log.e("ActivityWatcherService", "Error during debounced diff", e)
            }
        }
    }

    private fun startPeriodicFallbackCheck() {
        periodicJob?.cancel()
        periodicJob = serviceScope.launch {
            while (isActive) {
                delay(15 * 60 * 1000L) // Every 15 minutes as fallback
                try {
                    Log.d("ActivityWatcherService", "Periodic fallback snapshot diff check running...")
                    repository.runSnapshotDiff(isFirstRun = false)
                } catch (e: Exception) {
                    Log.w("ActivityWatcherService", "Periodic check failed", e)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        smsObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                // Ignore
            }
        }
        callObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                // Ignore
            }
        }
        serviceJob.cancel()
        _isServiceRunning.value = false
        repository.setWatcherActive(false)
        Log.d("ActivityWatcherService", "Service destroyed and observers unregistered")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
