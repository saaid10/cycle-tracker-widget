package com.cycletracker.widget.widget

import androidx.datastore.preferences.core.MutablePreferences
import com.cycletracker.widget.network.WidgetStatus

/** Writes a fresh WidgetStatus into Glance preferences state, clearing any prior error/login flag. */
fun MutablePreferences.applyStatus(status: WidgetStatus) {
    this[WidgetKeys.displayName] = status.displayName ?: ""
    status.cycleDay?.let { this[WidgetKeys.cycleDay] = it } ?: this.remove(WidgetKeys.cycleDay)
    this[WidgetKeys.loggedToday] = status.loggedToday
    this[WidgetKeys.todayFlowIntensity] = status.todayFlowIntensity ?: ""
    this[WidgetKeys.predictedPeriodStart] = status.predictedPeriodStart ?: ""
    this[WidgetKeys.predictedOvulationDate] = status.predictedOvulationDate ?: ""
    this[WidgetKeys.fertileWindowStart] = status.fertileWindowStart ?: ""
    this[WidgetKeys.fertileWindowEnd] = status.fertileWindowEnd ?: ""
    this[WidgetKeys.ovulationConfirmed] = status.ovulationConfirmed
    this[WidgetKeys.confidenceScore] = status.confidenceScore
    this[WidgetKeys.needsLogin] = false
    this[WidgetKeys.lastError] = ""
    this[WidgetKeys.lastUpdatedEpochMillis] = System.currentTimeMillis()
}

fun MutablePreferences.markNeedsLogin() {
    this[WidgetKeys.needsLogin] = true
}

fun MutablePreferences.markError(message: String) {
    this[WidgetKeys.lastError] = message
}
