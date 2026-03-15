package com.schoolms.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SchoolMsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize other global libraries here if needed (e.g., Timber for logging, Crashlytics)
    }
}
