package com.example.sync

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.example.data.local.ActivityEventEntity
import com.example.data.local.AppDatabase
import com.example.models.ActivityEventType
import com.example.models.DeviceConfig
import com.example.models.SnapshotDiffResult
import com.example.models.SyncActivityEvent
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data class Syncing(val message: String) : SyncStatus
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

class ActivityFeedRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("phone_sync_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(context)
    private val eventDao = database.activityEventDao()
    private val snapshotDiffer = SnapshotDiffer(context)

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _deviceConfig = MutableStateFlow(loadDeviceConfig())
    val deviceConfig: StateFlow<DeviceConfig> = _deviceConfig.asStateFlow()

    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private val _lastDiffResult = MutableStateFlow<SnapshotDiffResult?>(null)
    val lastDiffResult: StateFlow<SnapshotDiffResult?> = _lastDiffResult.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private var firestoreListener: ListenerRegistration? = null

    init {
        initFirebase()
        startFirestoreListener()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firestore = FirebaseFirestore.getInstance()
                _isFirebaseConnected.value = true
                Log.d("ActivityFeedRepo", "Firebase Firestore initialized successfully")
            } else {
                _isFirebaseConnected.value = false
                Log.d("ActivityFeedRepo", "FirebaseApp not initialized. Local offline engine active.")
            }
        } catch (e: Exception) {
            _isFirebaseConnected.value = false
            Log.w("ActivityFeedRepo", "Firebase unavailable: ${e.message}. Using local storage engine.")
        }
    }

    private fun startFirestoreListener() {
        val fs = firestore ?: return
        try {
            firestoreListener?.remove()
            firestoreListener = fs.collection("activity_events")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("ActivityFeedRepo", "Firestore listen failed", error)
                        _syncStatus.value = SyncStatus.Error("Firestore sync error: ${error.localizedMessage}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        repositoryScope.launch {
                            val cloudEvents = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(SyncActivityEvent::class.java)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (cloudEvents.isNotEmpty()) {
                                eventDao.upsertAll(cloudEvents.map { ActivityEventEntity.fromDomain(it) })
                                _syncStatus.value = SyncStatus.Success(
                                    "Synced ${cloudEvents.size} events from cloud"
                                )
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("ActivityFeedRepo", "Could not start Firestore listener: ${e.message}")
        }
    }

    fun getAllEventsFlow(): Flow<List<SyncActivityEvent>> {
        return eventDao.getAllEventsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun runSnapshotDiff(isFirstRun: Boolean = false): SnapshotDiffResult = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.Syncing("Running SMS & call log diff...")
        val config = _deviceConfig.value
        val diffResult = snapshotDiffer.performDiff(
            deviceId = config.deviceId,
            deviceName = config.deviceName,
            isFirstRun = isFirstRun
        )
        _lastDiffResult.value = diffResult

        val allEventsToUpsert = (diffResult.newEvents + diffResult.deletedEvents)
        if (allEventsToUpsert.isNotEmpty()) {
            eventDao.upsertAll(allEventsToUpsert.map { ActivityEventEntity.fromDomain(it) })

            // Sync to Firestore if available
            firestore?.let { fs ->
                try {
                    val batch = fs.batch()
                    allEventsToUpsert.forEach { event ->
                        val docRef = fs.collection("activity_events").document(event.id)
                        batch.set(docRef, event, SetOptions.merge())
                    }
                    batch.commit().addOnSuccessListener {
                        Log.d("ActivityFeedRepo", "Successfully committed batch to Firestore")
                    }.addOnFailureListener { e ->
                        Log.w("ActivityFeedRepo", "Firestore batch commit failed", e)
                    }
                } catch (e: Exception) {
                    Log.w("ActivityFeedRepo", "Error writing to Firestore: ${e.message}")
                }
            }
        }

        // Update counts
        val now = System.currentTimeMillis()
        val updatedConfig = config.copy(
            lastSyncTimestamp = now,
            totalLocalSms = diffResult.totalSmsFound,
            totalLocalCalls = diffResult.totalCallsFound
        )
        saveDeviceConfig(updatedConfig)

        val totalChanges = diffResult.newEvents.size + diffResult.deletedEvents.size
        _syncStatus.value = SyncStatus.Success(
            if (totalChanges > 0) "Found $totalChanges new/modified event(s)" else "Diff completed. Everything in sync."
        )

        diffResult
    }

    suspend fun injectSimulatedEvent(event: SyncActivityEvent) = withContext(Dispatchers.IO) {
        eventDao.upsert(ActivityEventEntity.fromDomain(event))
        firestore?.let { fs ->
            try {
                fs.collection("activity_events").document(event.id).set(event)
            } catch (e: Exception) {
                Log.w("ActivityFeedRepo", "Failed to push simulated event to Firestore", e)
            }
        }
        _syncStatus.value = SyncStatus.Success("Added simulated ${event.getTypedEventType().title}")
    }

    suspend fun markEventAsDeleted(event: SyncActivityEvent) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val updated = event.copy(
            isDeleted = true,
            deletedTimestamp = now,
            eventType = if (event.getTypedEventType().isSms) ActivityEventType.SMS_DELETED.name else ActivityEventType.CALL_DELETED.name,
            deletionNotedFrom = "Deleted from device by user"
        )
        eventDao.upsert(ActivityEventEntity.fromDomain(updated))
        firestore?.let { fs ->
            try {
                fs.collection("activity_events").document(updated.id).set(updated)
            } catch (e: Exception) {
                Log.w("ActivityFeedRepo", "Failed to sync deletion to Firestore", e)
            }
        }
        _syncStatus.value = SyncStatus.Success("Marked event as deleted & synced")
    }

    suspend fun clearAllEvents() = withContext(Dispatchers.IO) {
        eventDao.clearAll()
        _syncStatus.value = SyncStatus.Success("Local event feed cleared")
    }

    fun updateDeviceName(newName: String) {
        val current = _deviceConfig.value
        val updated = current.copy(deviceName = newName.trim())
        saveDeviceConfig(updated)
    }

    fun setWatcherActive(active: Boolean) {
        val current = _deviceConfig.value
        val updated = current.copy(isWatcherActive = active)
        saveDeviceConfig(updated)
    }

    private fun loadDeviceConfig(): DeviceConfig {
        var id = prefs.getString("device_id", null)
        if (id.isNullOrBlank()) {
            id = "phone_${Build.MODEL.lowercase().replace("\\s+".toRegex(), "_")}_${UUID.randomUUID().toString().take(4)}"
            prefs.edit().putString("device_id", id).apply()
        }

        var name = prefs.getString("device_name", null)
        if (name.isNullOrBlank()) {
            name = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
            prefs.edit().putString("device_name", name).apply()
        }

        val watcherActive = prefs.getBoolean("watcher_active", false)
        val lastSync = prefs.getLong("last_sync", 0L)
        val smsCount = prefs.getInt("total_sms", 0)
        val callCount = prefs.getInt("total_calls", 0)

        return DeviceConfig(
            deviceId = id,
            deviceName = name,
            isWatcherActive = watcherActive,
            lastSyncTimestamp = lastSync,
            totalLocalSms = smsCount,
            totalLocalCalls = callCount
        )
    }

    private fun saveDeviceConfig(config: DeviceConfig) {
        prefs.edit()
            .putString("device_id", config.deviceId)
            .putString("device_name", config.deviceName)
            .putBoolean("watcher_active", config.isWatcherActive)
            .putLong("last_sync", config.lastSyncTimestamp)
            .putInt("total_sms", config.totalLocalSms)
            .putInt("total_calls", config.totalLocalCalls)
            .apply()
        _deviceConfig.value = config
    }

    fun cleanup() {
        firestoreListener?.remove()
    }
}
