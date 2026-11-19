package com.trazo.android.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trazo.android.R
import com.trazo.android.ui.common.Labels
import com.trazo.android.ui.common.MinTouch
import com.trazo.android.ui.theme.TrazoPalette

/** The rules of the muscle priorities the server accepts, kept in one place so the screens and the request agree with it. */
object MusclePriorities {
    const val GROW = "Grow"
    const val EMPHASIZE = "Emphasize"
    const val MAINTAIN = "Maintain"
    const val MAX_EMPHASIZED = 3

    /** The muscles the server lets the user prioritise (core and small stabilisers are not offered). */
    val muscles = listOf("Chest", "Lats", "UpperBack", "Deltoids", "Biceps", "Triceps", "Quadriceps", "Hamstrings", "Glutes", "Calves")

    fun of(priorities: Map<String, String>, muscle: String) = priorities[muscle] ?: GROW

    fun canEmphasize(priorities: Map<String, String>, muscle: String) =
        of(priorities, muscle) == EMPHASIZE || priorities.values.count { it == EMPHASIZE } < MAX_EMPHASIZED

    /** The map after choosing [priority] for [muscle]: Grow is the default and is never sent; a refused emphasis changes nothing. */
    fun with(priorities: Map<String, String>, muscle: String, priority: String): Map<String, String> = when {
        muscle !in muscles -> priorities
        priority == GROW -> priorities - muscle
        priority == EMPHASIZE && !canEmphasize(priorities, muscle) -> priorities
        priority == EMPHASIZE || priority == MAINTAIN -> priorities + (muscle to priority)
        else -> priorities
    }

    /** What is sent to the server: only for hypertrophy, only the muscles that differ from Grow, and null when nothing is chosen. */
    fun forRequest(goal: String, priorities: Map<String, String>): Map<String, String>? =
        priorities.filterKeys { it in muscles }.filterValues { it == EMPHASIZE || it == MAINTAIN }
            .takeIf { goal == "Hypertrophy" && it.isNotEmpty() }

    fun label(priority: String) = when (priority) {
        EMPHASIZE -> R.string.priority_emphasize
        MAINTAIN -> R.string.priority_maintain
        else -> R.string.priority_grow
    }
}

/** One row per muscle with the three choices; Emphasize is disabled once three muscles are emphasized. */
@Composable
fun MusclePriorityEditor(priorities: Map<String, String>, onChange: (muscle: String, priority: String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        MusclePriorities.muscles.forEach { muscle ->
            val current = MusclePriorities.of(priorities, muscle)
            Row(Modifier.fillMaxWidth().heightIn(min = MinTouch), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(Labels.muscle(muscle), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                listOf(MusclePriorities.MAINTAIN, MusclePriorities.GROW, MusclePriorities.EMPHASIZE).forEach { option ->
                    FilterChip(
                        selected = current == option, onClick = { onChange(muscle, option) },
                        enabled = option != MusclePriorities.EMPHASIZE || MusclePriorities.canEmphasize(priorities, muscle),
                        label = { Text(stringResource(MusclePriorities.label(option)), style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TrazoPalette.PrimaryButton, selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        )
                    )
                }
            }
        }
    }
}
