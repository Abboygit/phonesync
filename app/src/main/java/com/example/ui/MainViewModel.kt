package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PhoneSyncApp
import com.example.models.ActivityEventType
import com.example.models.DeviceConfig
import com.example.models.SnapshotDiffResult
import com.example.models.SyncActivityEvent
import com.example.models.SyncFilterType
import com.example.sync.ActivityWatcherService
import com.example.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as PhoneSyncApp).repository

    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus
    val deviceConfig: StateFlow<DeviceConfig> = repository.deviceConfig
    val isFirebaseConnected: StateFlow<Boolean> = repository.isFirebaseConnected
    val isServiceRunning: StateFlow<Boolean> = ActivityWatcherService.isServiceRunning

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDeviceFilter = MutableStateFlow<String?>(null) // null = all
    val selectedDeviceFilter: StateFlow<String?> = _selectedDeviceFilter.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow(SyncFilterType.ALL)
    val selectedTypeFilter: StateFlow<SyncFilterType> = _selectedTypeFilter.asStateFlow()

    private val rawEvents = repository.getAllEventsFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val availableDevices: StateFlow<List<Pair<String, String>>> = rawEvents.combine(deviceConfig) { events, config ->
        val map = mutableMapOf<String, String>()
        map[config.deviceId] = "${config.deviceName} (This Phone)"
        events.forEach { event ->
            if (event.deviceId.isNotBlank() && !map.containsKey(event.deviceId)) {
                map[event.deviceId] = event.deviceName.ifBlank { event.deviceId }
            }
        }
        map.toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf(deviceConfig.value.deviceId to "${deviceConfig.value.deviceName} (This Phone)")
    )

    val filteredEvents: StateFlow<List<SyncActivityEvent>> = combine(
        rawEvents,
        _searchQuery,
        _selectedDeviceFilter,
        _selectedTypeFilter
    ) { events, query, deviceFilter, typeFilter ->
        events.filter { event ->
            // Device filter
            val matchesDevice = deviceFilter == null || event.deviceId == deviceFilter

            // Type filter
            val typed = event.getTypedEventType()
            val matchesType = when (typeFilter) {
                SyncFilterType.ALL -> true
                SyncFilterType.SMS_ONLY -> typed.isSms && !typed.isDeletion
                SyncFilterType.CALLS_ONLY -> typed.isCall && !typed.isDeletion
                SyncFilterType.DELETIONS_ONLY -> typed.isDeletion || event.isDeleted
            }

            // Search query filter
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val q = query.trim().lowercase()
                event.address.lowercase().contains(q) ||
                        (event.contactName?.lowercase()?.contains(q) == true) ||
                        (event.bodySnippet?.lowercase()?.contains(q) == true) ||
                        event.deviceName.lowercase().contains(q) ||
                        typed.title.lowercase().contains(q)
            }

            matchesDevice && matchesType && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val counts: StateFlow<Map<SyncFilterType, Int>> = rawEvents.combine(_selectedDeviceFilter) { events, deviceFilter ->
        val scoped = if (deviceFilter == null) events else events.filter { it.deviceId == deviceFilter }
        mapOf(
            SyncFilterType.ALL to scoped.size,
            SyncFilterType.SMS_ONLY to scoped.count { it.getTypedEventType().isSms && !it.isDeleted },
            SyncFilterType.CALLS_ONLY to scoped.count { it.getTypedEventType().isCall && !it.isDeleted },
            SyncFilterType.DELETIONS_ONLY to scoped.count { it.isDeleted || it.getTypedEventType().isDeletion }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    init {
        // Run initial diff
        viewModelScope.launch {
            repository.runSnapshotDiff(isFirstRun = true)
        }
    }

    fun triggerDiff() {
        viewModelScope.launch {
            repository.runSnapshotDiff(isFirstRun = false)
        }
    }

    fun toggleService(context: Context) {
        if (isServiceRunning.value) {
            ActivityWatcherService.stop(context)
        } else {
            ActivityWatcherService.start(context)
        }
    }

    fun updateDeviceName(name: String) {
        repository.updateDeviceName(name)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDeviceFilter(deviceId: String?) {
        _selectedDeviceFilter.value = deviceId
    }

    fun setTypeFilter(type: SyncFilterType) {
        _selectedTypeFilter.value = type
    }

    fun markEventDeleted(event: SyncActivityEvent) {
        viewModelScope.launch {
            repository.markEventAsDeleted(event)
        }
    }

    fun clearAllEvents() {
        viewModelScope.launch {
            repository.clearAllEvents()
        }
    }

    fun injectSimulatedEvent(
        isSecondaryPhone: Boolean,
        type: ActivityEventType,
        address: String,
        contactName: String?,
        bodySnippet: String?,
        callDurationSeconds: Long
    ) {
        viewModelScope.launch {
            val currentDevice = deviceConfig.value
            val targetDeviceId = if (isSecondaryPhone) "phone_secondary_b7x2" else currentDevice.deviceId
            val targetDeviceName = if (isSecondaryPhone) "Galaxy S24 (Paired Phone)" else currentDevice.deviceName

            val id = "${targetDeviceId}_sim_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
            val event = SyncActivityEvent(
                id = id,
                originalRecordId = System.currentTimeMillis(),
                deviceId = targetDeviceId,
                deviceName = targetDeviceName,
                eventType = type.name,
                address = address.ifBlank { "+1 (555) 234-5678" },
                contactName = contactName?.ifBlank { null } ?: "Alex Morgan",
                bodySnippet = bodySnippet ?: if (type.isSms) "Hey! Are we still meeting for lunch at 12:30?" else null,
                callDurationSeconds = callDurationSeconds,
                timestamp = System.currentTimeMillis(),
                detectedTimestamp = System.currentTimeMillis(),
                isDeleted = type.isDeletion,
                deletedTimestamp = if (type.isDeletion) System.currentTimeMillis() else null,
                deletionNotedFrom = if (type.isDeletion) "Simulated deletion test" else null
            )
            repository.injectSimulatedEvent(event)
        }
    }
}
