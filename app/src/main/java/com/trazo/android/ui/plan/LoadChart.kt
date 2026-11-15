package com.trazo.android.ui.plan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trazo.android.R
import com.trazo.android.data.model.PlanLoadView
import com.trazo.android.ui.theme.TrazoPalette

/**
 * Weekly volume of the whole plan: horizontal axis = plan weeks, vertical axis = sets. One line for the planned volume (only
 * where its block has been generated) and one, in another colour, for what was completed (only for the weeks already reached).
 * Deload weeks sit on a grey band.
 */
@Composable
fun LoadChart(load: PlanLoadView, modifier: Modifier = Modifier) {
    val planned = TrazoPalette.PrimaryButton
    val actual = TrazoPalette.Ok
    val grid = TrazoPalette.Muted.copy(alpha = 0.35f)
    val band = TrazoPalette.Muted.copy(alpha = 0.15f)
    val weeks = load.weeks
    val top = niceTop(weeks.maxOfOrNull { maxOf(it.plannedSets ?: 0, it.completedSets ?: 0) } ?: 0)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.overview_load_axis), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().height(160.dp)) {
            Column(Modifier.fillMaxHeight().padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                Text("$top", color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                Text("${top / 2}", color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                Text("0", color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                val w = size.width
                val h = size.height
                val n = weeks.size
                if (n == 0) return@Canvas
                // Each week owns an equal slice; points sit in the middle of it.
                val slice = w / n
                fun x(index: Int) = slice * (index + 0.5f)
                fun y(value: Int) = h - value / top.toFloat() * h
                weeks.forEachIndexed { i, week ->
                    if (week.isDeload) drawRect(band, Offset(slice * i, 0f), Size(slice, h))
                }
                drawLine(grid, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 1f)
                drawLine(grid, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                drawLine(grid, Offset(0f, h), Offset(w, h), strokeWidth = 2f)
                drawLine(grid, Offset(0f, 0f), Offset(0f, h), strokeWidth = 2f)

                fun line(color: Color, value: (Int) -> Int?) {
                    // Consecutive weeks with a value are joined; a gap (week without data) breaks the line.
                    for (i in 0 until n) {
                        val a = value(i) ?: continue
                        val b = if (i + 1 < n) value(i + 1) else null
                        if (b != null) drawLine(color, Offset(x(i), y(a)), Offset(x(i + 1), y(b)), strokeWidth = 5f, cap = StrokeCap.Round)
                        drawCircle(color, radius = 7f, center = Offset(x(i), y(a)))
                    }
                }
                line(planned) { weeks[it].plannedSets }
                line(actual) { weeks[it].completedSets }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 28.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.overview_load_week, 1), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
            if (weeks.size > 2) Text(stringResource(R.string.overview_load_week, (weeks.size + 1) / 2), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
            if (weeks.size > 1) Text(stringResource(R.string.overview_load_week, weeks.size), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend(planned, stringResource(R.string.overview_load_planned))
            Legend(actual, stringResource(R.string.overview_load_actual))
        }
    }
}

/** Upper bound of the vertical axis: the max rounded up to an even number so the middle label is a whole number (min 10). */
internal fun niceTop(max: Int): Int {
    val step = if (max <= 40) 10 else 20
    return ((max + step - 1) / step * step).coerceAtLeast(10)
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
