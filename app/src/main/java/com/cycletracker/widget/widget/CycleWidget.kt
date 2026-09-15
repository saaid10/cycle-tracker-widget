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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.cycletracker.widget.LoginActivity
import com.cycletracker.widget.widget.actions.LogFlowAction
import com.cycletracker.widget.widget.actions.LogSymptomAction
import com.cycletracker.widget.widget.actions.flowIntensityParam
import com.cycletracker.widget.widget.actions.symptomTagParam

// Matches the web app's rose identity (theme-color #d85c85) so the widget
// doesn't feel like a separate product. The accent is reserved for exactly
// one meaning -- "this is what's logged" -- everything else stays neutral.
private val BackgroundColor = Color(0xFFFFF7F9)
private val AccentColor = Color(0xFFD8527E)
private val TextPrimary = Color(0xFF3F2E33)
private val TextMuted = Color(0xFF8C7680)
private val FlowIdleColor = Color(0xFFF5DCE4)
private val SymptomIdleColor = Color(0xFFE4E7F5)
private val ErrorColor = Color(0xFFB00020)

// Short labels for the narrow buttons (must survive the smallest supported
// widget width without wrapping); full words for the "Logged: ..." line,
// which has a whole row's width to itself.
private val FLOW_BUTTON_LABELS = mapOf(
    "none" to "None",
    "spotting" to "Spot",
    "light" to "Light",
    "medium" to "Med",
    "heavy" to "Heavy",
)
private val FLOW_FULL_LABELS = mapOf(
    "none" to "None",
    "spotting" to "Spotting",
    "light" to "Light",
    "medium" to "Medium",
    "heavy" to "Heavy",
)

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
                .background(BackgroundColor)
                .padding(14.dp)
        ) {
            if (needsLogin) {
                Text(
                    text = "Sign in to see your cycle",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ColorProvider(TextPrimary))
                )
                Spacer(modifier = GlanceModifier.height(10.dp))
                Box(
                    modifier = GlanceModifier
                        .background(AccentColor)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable(actionStartActivity<LoginActivity>())
                ) {
                    Text(
                        text = "Sign in again",
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ColorProvider(Color.White))
                    )
                }
            } else {
                StatusHeader(prefs)
                val lastError = prefs[WidgetKeys.lastError]?.takeIf { it.isNotBlank() }
                if (lastError != null) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Couldn't save: $lastError",
                        style = TextStyle(fontSize = 11.sp, color = ColorProvider(ErrorColor))
                    )
                }
                Spacer(modifier = GlanceModifier.height(10.dp))
                FlowButtonsRow(prefs)
                Spacer(modifier = GlanceModifier.height(8.dp))
                SymptomButtonsRow(prefs)
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
        val todayFlowLabel = prefs[WidgetKeys.todayFlowIntensity]?.let { FLOW_FULL_LABELS[it] }

        Text(
            text = if (cycleDay != null) "Cycle day $cycleDay" else "No cycle data yet",
            style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ColorProvider(TextPrimary))
        )
        if (predictedPeriodStart != null) {
            Text(
                text = "Next period: $predictedPeriodStart",
                style = TextStyle(fontSize = 12.sp, color = ColorProvider(TextMuted))
            )
        }
        if (predictedOvulationDate != null) {
            Text(
                text = if (ovulationConfirmed) {
                    "Ovulation: $predictedOvulationDate"
                } else {
                    "Ovulation (est.): $predictedOvulationDate"
                },
                style = TextStyle(fontSize = 12.sp, color = ColorProvider(TextMuted))
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = if (loggedToday && todayFlowLabel != null) "✓ Logged: $todayFlowLabel" else "Not logged today",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = if (loggedToday) FontWeight.Medium else FontWeight.Normal,
                color = ColorProvider(if (loggedToday) AccentColor else TextMuted)
            )
        )
    }

    @Composable
    private fun FlowButtonsRow(prefs: Preferences) {
        val selectedFlow = prefs[WidgetKeys.todayFlowIntensity]?.takeIf {
            (prefs[WidgetKeys.loggedToday] ?: false) && it.isNotBlank()
        }
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            val entries = FLOW_BUTTON_LABELS.entries.toList()
            entries.forEachIndexed { index, (flowValue, flowLabel) ->
                FlowButton(flowLabel, flowValue, isSelected = flowValue == selectedFlow)
                if (index != entries.lastIndex) {
                    Spacer(modifier = GlanceModifier.width(5.dp))
                }
            }
        }
    }

    @Composable
    private fun FlowButton(label: String, value: String, isSelected: Boolean) {
        Box(
            modifier = GlanceModifier
                .width(44.dp)
                .padding(vertical = 14.dp)
                .background(if (isSelected) AccentColor else FlowIdleColor)
                .clickable(
                    actionRunCallback<LogFlowAction>(actionParametersOf(flowIntensityParam to value))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    color = ColorProvider(if (isSelected) Color.White else TextPrimary)
                )
            )
        }
    }

    @Composable
    private fun SymptomButtonsRow(prefs: Preferences) {
        val loggedSymptoms = prefs[WidgetKeys.todaySymptoms] ?: emptySet()
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            SymptomChip(
                "Ovulation twinge",
                "ovulation_pain",
                isSelected = "ovulation_pain" in loggedSymptoms
            )
            Spacer(modifier = GlanceModifier.width(10.dp))
            SymptomChip(
                "Fertile mucus",
                "fertile_mucus",
                isSelected = "fertile_mucus" in loggedSymptoms
            )
        }
    }

    @Composable
    private fun SymptomChip(label: String, tag: String, isSelected: Boolean) {
        Box(
            modifier = GlanceModifier
                .width(118.dp)
                .padding(vertical = 12.dp, horizontal = 6.dp)
                .background(if (isSelected) AccentColor else SymptomIdleColor)
                .clickable(
                    actionRunCallback<LogSymptomAction>(actionParametersOf(symptomTagParam to tag))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    color = ColorProvider(if (isSelected) Color.White else TextPrimary)
                )
            )
        }
    }
}
