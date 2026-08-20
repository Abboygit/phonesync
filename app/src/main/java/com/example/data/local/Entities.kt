package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.example.models.SyncActivityEvent
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "activity_events")
data class ActivityEventEntity(
    @PrimaryKey
    val id: String,
    val originalRecordId: Long,
    val deviceId: String,
    val deviceName: String,
    val eventType: String,
    val address: String,
    val contactName: String?,
    val bodySnippet: String?,
    val callDurationSeconds: Long,
    val timestamp: Long,
    val detectedTimestamp: Long,
    val isDeleted: Boolean,
    val deletedTimestamp: Long?,
    val deletionNotedFrom: String?
) {
    fun toDomain(): SyncActivityEvent = SyncActivityEvent(
        id = id,
        originalRecordId = originalRecordId,
        deviceId = deviceId,
        deviceName = deviceName,
        eventType = eventType,
        address = address,
        contactName = contactName,
        bodySnippet = bodySnippet,
        callDurationSeconds = callDurationSeconds,
        timestamp = timestamp,
        detectedTimestamp = detectedTimestamp,
        isDeleted = isDeleted,
        deletedTimestamp = deletedTimestamp,
        deletionNotedFrom = deletionNotedFrom
    )

    companion object {
        fun fromDomain(event: SyncActivityEvent): ActivityEventEntity = ActivityEventEntity(
            id = event.id,
            originalRecordId = event.originalRecordId,
            deviceId = event.deviceId,
            deviceName = event.deviceName,
            eventType = event.eventType,
            address = event.address,
            contactName = event.contactName,
            bodySnippet = event.bodySnippet,
            callDurationSeconds = event.callDurationSeconds,
            timestamp = event.timestamp,
            detectedTimestamp = event.detectedTimestamp,
            isDeleted = event.isDeleted,
            deletedTimestamp = event.deletedTimestamp,
            deletionNotedFrom = event.deletionNotedFrom
        )
    }
}

@Entity(tableName = "sms_snapshot")
data class SmsSnapshotEntity(
    @PrimaryKey
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
    val contactName: String?
)

@Entity(tableName = "call_snapshot")
data class CallSnapshotEntity(
    @PrimaryKey
    val id: Long,
    val number: String,
    val date: Long,
    val duration: Long,
    val type: Int,
    val contactName: String?
)

@Dao
interface ActivityEventDao {
    @Query("SELECT * FROM activity_events ORDER BY timestamp DESC")
    fun getAllEventsFlow(): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM activity_events ORDER BY timestamp DESC")
    suspend fun getAllEventsList(): List<ActivityEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<ActivityEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: ActivityEventEntity)

    @Query("UPDATE activity_events SET isDeleted = 1, deletedTimestamp = :deletedTime, deletionNotedFrom = :notedFrom WHERE originalRecordId = :recordId AND deviceId = :deviceId")
    suspend fun markDeleted(recordId: Long, deviceId: String, deletedTime: Long, notedFrom: String)

    @Query("DELETE FROM activity_events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM activity_events")
    suspend fun clearAll()
}

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM sms_snapshot")
    suspend fun getAllSmsSnapshots(): List<SmsSnapshotEntity>

    @Query("DELETE FROM sms_snapshot")
    suspend fun clearSmsSnapshots()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsSnapshots(snapshots: List<SmsSnapshotEntity>)

    @Transaction
    suspend fun replaceSmsSnapshots(snapshots: List<SmsSnapshotEntity>) {
        clearSmsSnapshots()
        if (snapshots.isNotEmpty()) {
            insertSmsSnapshots(snapshots)
        }
    }

    @Query("SELECT * FROM call_snapshot")
    suspend fun getAllCallSnapshots(): List<CallSnapshotEntity>

    @Query("DELETE FROM call_snapshot")
    suspend fun clearCallSnapshots()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallSnapshots(snapshots: List<CallSnapshotEntity>)

    @Transaction
    suspend fun replaceCallSnapshots(snapshots: List<CallSnapshotEntity>) {
        clearCallSnapshots()
        if (snapshots.isNotEmpty()) {
            insertCallSnapshots(snapshots)
        }
    }

    @Query("SELECT COUNT(*) FROM sms_snapshot")
    suspend fun getSmsCount(): Int

    @Query("SELECT COUNT(*) FROM call_snapshot")
    suspend fun getCallCount(): Int
}
