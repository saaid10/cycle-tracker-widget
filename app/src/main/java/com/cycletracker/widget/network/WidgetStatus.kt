package com.cycletracker.widget.network

import kotlinx.serialization.Serializable

/** Mirrors WidgetStatus in the Next.js app's src/lib/widget/status.ts. */
@Serializable
data class WidgetStatus(
    val displayName: String? = null,
    val today: String,
    val cycleDay: Int? = null,
    val loggedToday: Boolean,
    val todayFlowIntensity: String? = null,
    val predictedPeriodStart: String? = null,
    val predictedOvulationDate: String? = null,
    val fertileWindowStart: String? = null,
    val fertileWindowEnd: String? = null,
    val ovulationConfirmed: Boolean = false,
    val confidenceScore: Double = 0.0
)

@Serializable
data class LogRequest(
    val flowIntensity: String,
    val symptoms: List<String>? = null
)
