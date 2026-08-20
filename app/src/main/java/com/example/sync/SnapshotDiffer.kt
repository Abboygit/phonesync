package com.example.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.local.CallSnapshotEntity
import com.example.data.local.SmsSnapshotEntity
import com.example.models.ActivityEventType
import com.example.models.CallSnapshotRecord
import com.example.models.SmsSnapshotRecord
import com.example.models.SnapshotDiffResult
import com.example.models.SyncActivityEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class SnapshotDiffer(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val snapshotDao = database.snapshotDao()

    suspend fun performDiff(
        deviceId: String,
        deviceName: String,
        isFirstRun: Boolean = false
    ): SnapshotDiffResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val newEvents = mutableListOf<SyncActivityEvent>()
        val deletedEvents = mutableListOf<SyncActivityEvent>()

        val hasSmsPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val hasCallPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        // 1. Process SMS
        val freshSmsList = if (hasSmsPerm) queryCurrentSms() else emptyList()
        val previousSmsEntities = snapshotDao.getAllSmsSnapshots()
        val previousSmsMap = previousSmsEntities.associateBy { it.id }

        if (previousSmsEntities.isEmpty() && isFirstRun && freshSmsList.isNotEmpty()) {
            // Baseline initial ingestion: take recent messages to populate feed
            val baseline = freshSmsList.take(30)
            baseline.forEach { sms ->
                val type = if (sms.type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                    ActivityEventType.SMS_OUTGOING
                } else {
                    ActivityEventType.SMS_INCOMING
                }
                newEvents.add(
                    SyncActivityEvent(
                        id = "${deviceId}_sms_${sms.id}",
                        originalRecordId = sms.id,
                        deviceId = deviceId,
                        deviceName = deviceName,
                        eventType = type.name,
                        address = sms.address,
                        contactName = sms.contactName,
                        bodySnippet = sms.body,
                        callDurationSeconds = 0L,
                        timestamp = sms.date,
                        detectedTimestamp = startTime,
                        isDeleted = false
                    )
                )
            }
        } else if (previousSmsEntities.isNotEmpty()) {
            // Find NEW SMS (in fresh but not in previous)
            freshSmsList.forEach { sms ->
                if (!previousSmsMap.containsKey(sms.id)) {
                    val type = if (sms.type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                        ActivityEventType.SMS_OUTGOING
                    } else {
                        ActivityEventType.SMS_INCOMING
                    }
                    newEvents.add(
                        SyncActivityEvent(
                            id = "${deviceId}_sms_${sms.id}",
                            originalRecordId = sms.id,
                            deviceId = deviceId,
                            deviceName = deviceName,
                            eventType = type.name,
                            address = sms.address,
                            contactName = sms.contactName,
                            bodySnippet = sms.body,
                            callDurationSeconds = 0L,
                            timestamp = sms.date,
                            detectedTimestamp = startTime,
                            isDeleted = false
                        )
                    )
                }
            }

            // Find DELETED SMS (in previous snapshot but missing from fresh table)
            if (hasSmsPerm) {
                val freshSmsIds = freshSmsList.map { it.id }.toSet()
                previousSmsEntities.forEach { oldSms ->
                    if (!freshSmsIds.contains(oldSms.id)) {
                        deletedEvents.add(
                            SyncActivityEvent(
                                id = "${deviceId}_sms_del_${oldSms.id}",
                                originalRecordId = oldSms.id,
                                deviceId = deviceId,
                                deviceName = deviceName,
                                eventType = ActivityEventType.SMS_DELETED.name,
                                address = oldSms.address,
                                contactName = oldSms.contactName,
                                bodySnippet = oldSms.body,
                                callDurationSeconds = 0L,
                                timestamp = oldSms.date,
                                detectedTimestamp = startTime,
                                isDeleted = true,
                                deletedTimestamp = startTime,
                                deletionNotedFrom = "Missing in SMS snapshot diff"
                            )
                        )
                    }
                }
            }
        }

        // Save fresh SMS snapshot to local Room DB
        if (hasSmsPerm) {
            val entitiesToSave = freshSmsList.map {
                SmsSnapshotEntity(
                    id = it.id,
                    address = it.address,
                    body = it.body,
                    date = it.date,
                    type = it.type,
                    contactName = it.contactName
                )
            }
            snapshotDao.replaceSmsSnapshots(entitiesToSave)
        }

        // 2. Process Calls
        val freshCallList = if (hasCallPerm) queryCurrentCalls() else emptyList()
        val previousCallEntities = snapshotDao.getAllCallSnapshots()
        val previousCallMap = previousCallEntities.associateBy { it.id }

        if (previousCallEntities.isEmpty() && isFirstRun && freshCallList.isNotEmpty()) {
            val baseline = freshCallList.take(30)
            baseline.forEach { call ->
                val type = mapCallType(call.type)
                newEvents.add(
                    SyncActivityEvent(
                        id = "${deviceId}_call_${call.id}",
                        originalRecordId = call.id,
                        deviceId = deviceId,
                        deviceName = deviceName,
                        eventType = type.name,
                        address = call.number,
                        contactName = call.contactName,
                        bodySnippet = null,
                        callDurationSeconds = call.duration,
                        timestamp = call.date,
                        detectedTimestamp = startTime,
                        isDeleted = false
                    )
                )
            }
        } else if (previousCallEntities.isNotEmpty()) {
            // Find NEW Calls
            freshCallList.forEach { call ->
                if (!previousCallMap.containsKey(call.id)) {
                    val type = mapCallType(call.type)
                    newEvents.add(
                        SyncActivityEvent(
                            id = "${deviceId}_call_${call.id}",
                            originalRecordId = call.id,
                            deviceId = deviceId,
                            deviceName = deviceName,
                            eventType = type.name,
                            address = call.number,
                            contactName = call.contactName,
                            bodySnippet = null,
                            callDurationSeconds = call.duration,
                            timestamp = call.date,
                            detectedTimestamp = startTime,
                            isDeleted = false
                        )
                    )
                }
            }

            // Find DELETED Calls
            if (hasCallPerm) {
                val freshCallIds = freshCallList.map { it.id }.toSet()
                previousCallEntities.forEach { oldCall ->
                    if (!freshCallIds.contains(oldCall.id)) {
                        deletedEvents.add(
                            SyncActivityEvent(
                                id = "${deviceId}_call_del_${oldCall.id}",
                                originalRecordId = oldCall.id,
                                deviceId = deviceId,
                                deviceName = deviceName,
                                eventType = ActivityEventType.CALL_DELETED.name,
                                address = oldCall.number,
                                contactName = oldCall.contactName,
                                bodySnippet = null,
                                callDurationSeconds = oldCall.duration,
                                timestamp = oldCall.date,
                                detectedTimestamp = startTime,
                                isDeleted = true,
                                deletedTimestamp = startTime,
                                deletionNotedFrom = "Missing in Call Log snapshot diff"
                            )
                        )
                    }
                }
            }
        }

        // Save fresh Call snapshot
        if (hasCallPerm) {
            val entitiesToSave = freshCallList.map {
                CallSnapshotEntity(
                    id = it.id,
                    number = it.number,
                    date = it.date,
                    duration = it.duration,
                    type = it.type,
                    contactName = it.contactName
                )
            }
            snapshotDao.replaceCallSnapshots(entitiesToSave)
        }

        val duration = System.currentTimeMillis() - startTime
        SnapshotDiffResult(
            newEvents = newEvents,
            deletedEvents = deletedEvents,
            totalSmsFound = freshSmsList.size,
            totalCallsFound = freshCallList.size,
            executionTimeMs = duration
        )
    }

    private fun queryCurrentSms(): List<SmsSnapshotRecord> {
        val result = mutableListOf<SmsSnapshotRecord>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val cursor: Cursor? = try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )
        } catch (e: Exception) {
            null
        }

        cursor?.use {
            val idCol = it.getColumnIndex(Telephony.Sms._ID)
            val addrCol = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyCol = it.getColumnIndex(Telephony.Sms.BODY)
            val dateCol = it.getColumnIndex(Telephony.Sms.DATE)
            val typeCol = it.getColumnIndex(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val id = if (idCol >= 0) it.getLong(idCol) else 0L
                val address = if (addrCol >= 0) it.getString(addrCol) ?: "" else ""
                val body = if (bodyCol >= 0) it.getString(bodyCol) ?: "" else ""
                val date = if (dateCol >= 0) it.getLong(dateCol) else System.currentTimeMillis()
                val type = if (typeCol >= 0) it.getInt(typeCol) else Telephony.Sms.MESSAGE_TYPE_INBOX
                val contactName = resolveContactName(address)

                result.add(
                    SmsSnapshotRecord(
                        id = id,
                        address = address,
                        body = body,
                        date = date,
                        type = type,
                        contactName = contactName
                    )
                )
            }
        }
        return result
    }

    private fun queryCurrentCalls(): List<CallSnapshotRecord> {
        val result = mutableListOf<CallSnapshotRecord>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE,
            CallLog.Calls.CACHED_NAME
        )
        val cursor: Cursor? = try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )
        } catch (e: Exception) {
            null
        }

        cursor?.use {
            val idCol = it.getColumnIndex(CallLog.Calls._ID)
            val numCol = it.getColumnIndex(CallLog.Calls.NUMBER)
            val dateCol = it.getColumnIndex(CallLog.Calls.DATE)
            val durCol = it.getColumnIndex(CallLog.Calls.DURATION)
            val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
            val nameCol = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

            while (it.moveToNext()) {
                val id = if (idCol >= 0) it.getLong(idCol) else 0L
                val number = if (numCol >= 0) it.getString(numCol) ?: "" else ""
                val date = if (dateCol >= 0) it.getLong(dateCol) else System.currentTimeMillis()
                val duration = if (durCol >= 0) it.getLong(durCol) else 0L
                val type = if (typeCol >= 0) it.getInt(typeCol) else CallLog.Calls.INCOMING_TYPE
                val cachedName = if (nameCol >= 0) it.getString(nameCol) else null
                val contactName = cachedName ?: resolveContactName(number)

                result.add(
                    CallSnapshotRecord(
                        id = id,
                        number = number,
                        date = date,
                        duration = duration,
                        type = type,
                        contactName = contactName
                    )
                )
            }
        }
        return result
    }

    private fun resolveContactName(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        val hasContactPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasContactPerm) return null

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val col = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (col >= 0) cursor.getString(col) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun mapCallType(type: Int): ActivityEventType {
        return when (type) {
            CallLog.Calls.INCOMING_TYPE -> ActivityEventType.CALL_INCOMING
            CallLog.Calls.OUTGOING_TYPE -> ActivityEventType.CALL_OUTGOING
            CallLog.Calls.MISSED_TYPE -> ActivityEventType.CALL_MISSED
            CallLog.Calls.REJECTED_TYPE -> ActivityEventType.CALL_REJECTED
            else -> ActivityEventType.CALL_INCOMING
        }
    }
}
