package com.cycletracker.widget.widget.actions

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.cycletracker.widget.CycleTrackerApp
import com.cycletracker.widget.network.ApiException
import com.cycletracker.widget.widget.CycleWidget
import com.cycletracker.widget.widget.WidgetKeys
import com.cycletracker.widget.widget.applyStatus
import com.cycletracker.widget.widget.markError
import com.cycletracker.widget.widget.markNeedsLogin

val symptomTagParam = ActionParameters.Key<String>("symptomTag")

/**
 * Tapping an ovulation-signal chip (ovulation_pain / fertile_mucus) adds it
 * to today's already-known symptoms and re-sends today's already-known flow
 * intensity (the API requires flowIntensity on every write; "none" if
 * nothing's been logged today yet).
 */
class LogSymptomAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val symptomTag = parameters[symptomTagParam] ?: return
        val app = context.applicationContext as CycleTrackerApp

        val currentPrefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val existingSymptoms = currentPrefs[WidgetKeys.todaySymptoms] ?: emptySet()
        val existingFlow = currentPrefs[WidgetKeys.todayFlowIntensity]?.takeIf { it.isNotBlank() } ?: "none"
        val mergedSymptoms = existingSymptoms + symptomTag

        try {
            val status = app.apiClient.logFlow(
                flowIntensity = existingFlow,
                symptoms = mergedSymptoms.toList()
            )
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs.applyStatus(status)
                prefs[WidgetKeys.todaySymptoms] = mergedSymptoms
            }
        } catch (e: ApiException.Unauthorized) {
            updateAppWidgetState(context, glanceId) { prefs -> prefs.markNeedsLogin() }
        } catch (e: ApiException) {
            updateAppWidgetState(context, glanceId) { prefs -> prefs.markError(e.message ?: "Failed to log") }
        }

        CycleWidget().update(context, glanceId)
    }
}
