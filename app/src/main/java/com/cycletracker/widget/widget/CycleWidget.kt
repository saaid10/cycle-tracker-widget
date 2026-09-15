package com.cycletracker.widget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.cycletracker.widget.LoginActivity
import com.cycletracker.widget.widget.actions.LogFlowAction
import com.cycletracker.widget.widget.actions.LogSymptomAction
import com.cycletracker.widget.widget.actions.flowIntensityParam
import com.cycletracker.widget.widget.actions.symptomTagParam

class CycleWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<Preferences>()
        val needsLogin = prefs[WidgetKeys.needsLogin] ?: false

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp)
        ) {
            if (needsLogin) {
                Text(
                    text = "Sign in to see your cycle",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = "Sign in again",
                    modifier = GlanceModifier.clickable(actionStartActivity<LoginActivity>())
                )
            } else {
                StatusHeader(prefs)
                val lastError = prefs[WidgetKeys.lastError]?.takeIf { it.isNotBlank() }
                if (lastError != null) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Couldn't save: $lastError",
                        style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xFFB00020)))
                    )
                }
                Spacer(modifier = GlanceModifier.height(8.dp))
                FlowButtonsRow()
                Spacer(modifier = GlanceModifier.height(4.dp))
                SymptomButtonsRow()
            }
        }
    }

    @Composable
    private fun StatusHeader(prefs: Preferences) {
        val cycleDay = prefs[WidgetKeys.cycleDay]
        val predictedPeriodStart = prefs[WidgetKeys.predictedPeriodStart]?.takeIf { it.isNotBlank() }
        val predictedOvulationDate = prefs[WidgetKeys.predictedOvulationDate]?.takeIf { it.isNotBlank() }
        val ovulationConfirmed = prefs[WidgetKeys.ovulationConfirmed] ?: false
        val loggedToday = prefs[WidgetKeys.loggedToday] ?: false

        Text(
            text = if (cycleDay != null) "Cycle day $cycleDay" else "No cycle data yet",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
        )
        if (predictedPeriodStart != null) {
            Text(
                text = "Next period: $predictedPeriodStart",
                style = TextStyle(fontSize = 12.sp)
            )
        }
        if (predictedOvulationDate != null) {
            Text(
                text = if (ovulationConfirmed) {
                    "Ovulation: $predictedOvulationDate"
                } else {
                    "Ovulation (est.): $predictedOvulationDate"
                },
                style = TextStyle(
                    fontSize = 12.sp,
                    color = if (ovulationConfirmed) ColorProvider(Color.Black) else ColorProvider(Color.Gray)
                )
            )
        }
        Text(
            text = if (loggedToday) "Logged today" else "Not logged today",
            style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color.DarkGray))
        )
    }

    @Composable
    private fun FlowButtonsRow() {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            FlowButton("None", "none")
            FlowButton("Spot", "spotting")
            FlowButton("Light", "light")
            FlowButton("Med", "medium")
            FlowButton("Heavy", "heavy")
        }
    }

    @Composable
    private fun FlowButton(label: String, value: String) {
        Text(
            text = label,
            style = TextStyle(fontSize = 12.sp),
            modifier = GlanceModifier
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .background(Color(0xFFF3D9E0))
                .clickable(
                    actionRunCallback<LogFlowAction>(actionParametersOf(flowIntensityParam to value))
                )
        )
    }

    @Composable
    private fun SymptomButtonsRow() {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "Ovulation twinge",
                style = TextStyle(fontSize = 11.sp),
                modifier = GlanceModifier
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .background(Color(0xFFE0E8F3))
                    .clickable(
                        actionRunCallback<LogSymptomAction>(
                            actionParametersOf(symptomTagParam to "ovulation_pain")
                        )
                    )
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "Fertile mucus",
                style = TextStyle(fontSize = 11.sp),
                modifier = GlanceModifier
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .background(Color(0xFFE0E8F3))
                    .clickable(
                        actionRunCallback<LogSymptomAction>(
                            actionParametersOf(symptomTagParam to "fertile_mucus")
                        )
                    )
            )
        }
    }
}
