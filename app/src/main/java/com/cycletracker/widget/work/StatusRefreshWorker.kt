package com.cycletracker.widget.work

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cycletracker.widget.CycleTrackerApp
import com.cycletracker.widget.network.ApiException
import com.cycletracker.widget.widget.CycleWidget
import com.cycletracker.widget.widget.applyStatus
import com.cycletracker.widget.widget.markNeedsLogin
import java.util.concurrent.TimeUnit

/** Periodically (and once right after login) refreshes every placed widget instance's status. */
class StatusRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CycleTrackerApp
        val widget = CycleWidget()
        val glanceIds = GlanceAppWidgetManager(applicationContext).getGlanceIds(CycleWidget::class.java)
        if (glanceIds.isEmpty()) return Result.success()

        return try {
            val status = app.apiClient.getStatus()
            for (glanceId in glanceIds) {
                updateAppWidgetState(applicationContext, glanceId) { prefs -> prefs.applyStatus(status) }
                widget.update(applicationContext, glanceId)
            }
            Result.success()
        } catch (e: ApiException.Unauthorized) {
            for (glanceId in glanceIds) {
                updateAppWidgetState(applicationContext, glanceId) { prefs -> prefs.markNeedsLogin() }
                widget.update(applicationContext, glanceId)
            }
            Result.success()
        } catch (e: ApiException) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "cycle-tracker-status-refresh-periodic"
        private const val ONE_TIME_WORK_NAME = "cycle-tracker-status-refresh-once"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<StatusRefreshWorker>(30, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueOneTime(context: Context) {
            val request = OneTimeWorkRequestBuilder<StatusRefreshWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
