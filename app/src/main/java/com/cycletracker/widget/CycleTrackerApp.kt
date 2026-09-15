package com.cycletracker.widget

import android.app.Application
import com.cycletracker.widget.auth.AuthRepository
import com.cycletracker.widget.network.WidgetApiClient

class CycleTrackerApp : Application() {
    val authRepository: AuthRepository by lazy { AuthRepository(this) }
    val apiClient: WidgetApiClient by lazy { WidgetApiClient(this) }
}
