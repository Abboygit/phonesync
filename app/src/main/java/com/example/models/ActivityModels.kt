package com.example.models

enum class ActivityEventType(
    val title: String,
    val isCall: Boolean,
    val isSms: Boolean,
    val isDeletion: Boolean
) {
    SMS_INCOMING("Incoming SMS", isCall = false, isSms = true, isDeletion = false),
    SMS_OUTGOING("Outgoing SMS", isCall = false, isSms = true, isDeletion = false),
    SMS_DELETED("Deleted SMS", isCall = false, isSms = true, isDeletion = true),
    CALL_INCOMING("Incoming Call", isCall = true, isSms = false, isDeletion = false),
    CALL_OUTGOING("Outgoing Call", isCall = true, isSms = false, isDeletion = false),
    CALL_MISSED("Missed Call", isCall = true, isSms = false, isDeletion = false),
    CALL_REJECTED("Rejected Call", isCall = true, isSms = false, isDeletion = false),
    CALL_DELETED("Deleted Call", isCall = true, isSms = false, isDeletion = true);

    companion object {
        fun fromString(type: String?): ActivityEventType {
            return entries.firstOrNull { it.name.equals(type, ignoreCase = true) } ?: SMS_INCOMING
        }
    }
}

data class SyncActivityEvent(
    val id: String = "",
    val originalRecordId: Long = 0L,
    val deviceId: String = "",
    val deviceName: String = "",
    val eventType: String = ActivityEventType.SMS_INCOMING.name,
    val address: String = "",
    val contactName: String? = null,
    val bodySnippet: String? = null,
    val callDurationSeconds: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val detectedTimestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long? = null,
    val deletionNotedFrom: String? = null
) {
    // No-arg constructor for Firestore deserialization
    constructor() : this(
        id = "",
        originalRecordId = 0L,
        deviceId = "",
        deviceName = "",
        eventType = ActivityEventType.SMS_INCOMING.name,
        address = "",
        contactName = null,
        bodySnippet = null,
        callDurationSeconds = 0L,
        timestamp = 0L,
        detectedTimestamp = 0L,
        isDeleted = false,
        deletedTimestamp = null,
        deletionNotedFrom = null
    )

    fun getTypedEventType(): ActivityEventType {
        return ActivityEventType.fromString(eventType)
    }
}

data class SmsSnapshotRecord(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
    val contactName: String? = null
)

data class CallSnapshotRecord(
    val id: Long,
    val number: String,
    val date: Long,
    val duration: Long,
    val type: Int,
    val contactName: String? = null
)

data class SnapshotDiffResult(
    val newEvents: List<SyncActivityEvent>,
    val deletedEvents: List<SyncActivityEvent>,
    val totalSmsFound: Int,
    val totalCallsFound: Int,
    val executionTimeMs: Long
)

data class DeviceConfig(
    val deviceId: String,
    val deviceName: String,
    val isWatcherActive: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val totalLocalSms: Int = 0,
    val totalLocalCalls: Int = 0
)

enum class SyncFilterType {
    ALL,
    SMS_ONLY,
    CALLS_ONLY,
    DELETIONS_ONLY
}
