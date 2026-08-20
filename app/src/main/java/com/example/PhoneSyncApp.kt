package com.example

import android.app.Application
import com.example.sync.ActivityFeedRepository

class PhoneSyncApp : Application() {
    lateinit var repository: ActivityFeedRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = ActivityFeedRepository(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        repository.cleanup()
    }
}
