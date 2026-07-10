package io.github.aadi1607.habittracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.aadi1607.habittracker.HabitApplication
import io.github.aadi1607.habittracker.MainActivity
import io.github.aadi1607.habittracker.R
import io.github.aadi1607.habittracker.data.db.Habit
import java.time.LocalDate

private val WidgetBackground = Color(0xFF1C1F2B)
private val WidgetText = Color(0xFFE6E8F0)
private val WidgetTextMuted = Color(0xFF9BA0B4)
private val WidgetAccent = Color(0xFF8AB4FF)

private data class WidgetRow(
    val habit: Habit,
    val count: Int,
) {
    val done: Boolean get() = count >= habit.dailyTarget
}

/** Home-screen widget: today's habits with progress; tapping a row logs +1. */
class HabitWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as HabitApplication
        val rows = app.container.repository.getProgressOn(LocalDate.now())
            .map { WidgetRow(it.habit, it.count) }

        provideContent {
            WidgetContent(rows)
        }
    }
}

@Composable
private fun WidgetContent(rows: List<WidgetRow>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetBackground)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        val context = androidx.glance.LocalContext.current
        val doneCount = rows.count { it.done }
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = context.getString(R.string.widget_title),
                style = TextStyle(
                    color = ColorProvider(WidgetText),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (rows.isNotEmpty()) {
                Text(
                    text = context.getString(R.string.progress_done, doneCount, rows.size),
                    style = TextStyle(color = ColorProvider(WidgetTextMuted), fontSize = 13.sp),
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        when {
            rows.isEmpty() -> Text(
                text = context.getString(R.string.widget_empty),
                style = TextStyle(color = ColorProvider(WidgetTextMuted), fontSize = 14.sp),
            )
            rows.all { it.done } -> Text(
                text = context.getString(R.string.widget_all_done),
                style = TextStyle(color = ColorProvider(WidgetAccent), fontSize = 14.sp),
            )
            else -> LazyColumn {
                items(rows, itemId = { it.habit.id }) { row ->
                    WidgetHabitRow(row)
                }
            }
        }
    }
}

@Composable
private fun WidgetHabitRow(row: WidgetRow) {
    val rowModifier = if (row.done) {
        GlanceModifier.fillMaxWidth().padding(vertical = 6.dp)
    } else {
        GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(
                actionRunCallback<LogHabitAction>(
                    actionParametersOf(LogHabitAction.HABIT_ID to row.habit.id)
                )
            )
    }
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = row.habit.emoji,
            style = TextStyle(fontSize = 16.sp),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = row.habit.name,
            style = TextStyle(
                color = ColorProvider(if (row.done) WidgetTextMuted else WidgetText),
                fontSize = 14.sp,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = if (row.done) {
                "✓"
            } else {
                "${row.count}/${row.habit.dailyTarget}"
            },
            style = TextStyle(
                color = ColorProvider(if (row.done) WidgetAccent else WidgetText),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/** Widget row tap: log one completion and refresh the widget. */
class LogHabitAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val habitId = parameters[HABIT_ID] ?: return
        val app = context.applicationContext as HabitApplication
        app.container.repository.increment(habitId, LocalDate.now())
        HabitWidget().updateAll(context)
    }

    companion object {
        val HABIT_ID = ActionParameters.Key<Long>("habit_id")
    }
}

class HabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitWidget()
}
