package com.cycletracker.widget.widget.actions

import android.content.Context
import android.util.Log
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

private const val TAG = "CycleTrackerWidget"

val flowIntensityParam = ActionParameters.Key<String>("flowIntensity")

/** Tapping a flow button logs today with that intensity, keeping any symptoms already known locally. */
class LogFlowAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val flowIntensity = parameters[flowIntensityParam] ?: return
        val app = context.applicationContext as CycleTrackerApp

        val currentPrefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val existingSymptoms = currentPrefs[WidgetKeys.todaySymptoms] ?: emptySet()

        try {
            val status = app.apiClient.logFlow(
                flowIntensity = flowIntensity,
                symptoms = existingSymptoms.toList()
            )
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs.applyStatus(status)
                prefs[WidgetKeys.todaySymptoms] = existingSymptoms
            }
        } catch (e: ApiException.Unauthorized) {
            updateAppWidgetState(context, glanceId) { prefs -> prefs.markNeedsLogin() }
        } catch (e: ApiException) {
            Log.e(TAG, "LogFlowAction failed: ${e::class.simpleName}: ${e.message}")
            updateAppWidgetState(context, glanceId) { prefs -> prefs.markError(e.message ?: "Failed to log") }
        }

        CycleWidget().update(context, glanceId)
    }
}
