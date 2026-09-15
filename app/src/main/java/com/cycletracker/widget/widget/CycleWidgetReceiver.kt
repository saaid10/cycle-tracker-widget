package com.cycletracker.widget.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.cycletracker.widget.work.StatusRefreshWorker

class CycleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CycleWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        StatusRefreshWorker.enqueuePeriodic(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        StatusRefreshWorker.enqueuePeriodic(context)
        StatusRefreshWorker.enqueueOneTime(context)
    }
}
