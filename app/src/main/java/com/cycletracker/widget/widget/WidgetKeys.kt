package com.cycletracker.widget.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/** Glance widget state keys -- mirrors network.WidgetStatus plus local-only UI state. */
object WidgetKeys {
    val displayName = stringPreferencesKey("displayName")
    val cycleDay = intPreferencesKey("cycleDay")
    val loggedToday = booleanPreferencesKey("loggedToday")
    val todayFlowIntensity = stringPreferencesKey("todayFlowIntensity")
    val todaySymptoms = stringSetPreferencesKey("todaySymptoms")
    val predictedPeriodStart = stringPreferencesKey("predictedPeriodStart")
    val predictedOvulationDate = stringPreferencesKey("predictedOvulationDate")
    val fertileWindowStart = stringPreferencesKey("fertileWindowStart")
    val fertileWindowEnd = stringPreferencesKey("fertileWindowEnd")
    val ovulationConfirmed = booleanPreferencesKey("ovulationConfirmed")
    val confidenceScore = doublePreferencesKey("confidenceScore")

    val needsLogin = booleanPreferencesKey("needsLogin")
    val lastError = stringPreferencesKey("lastError")
    val lastUpdatedEpochMillis = longPreferencesKey("lastUpdatedEpochMillis")
}
