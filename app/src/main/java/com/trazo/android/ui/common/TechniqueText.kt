package com.trazo.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.trazo.android.R
import com.trazo.android.data.model.TechniqueView
import com.trazo.android.data.model.Techniques

/** The name of a technique; a drop set of two or three drops is shown as descending sets. */
@Composable
fun techniqueName(type: String, maxSegments: Int = 1): String = stringResource(
    when (type) {
        Techniques.DROP_SET -> if (maxSegments > 1) R.string.technique_descending else R.string.technique_drop_set
        Techniques.MYO_REPS -> R.string.technique_myo_reps
        Techniques.MATCH -> R.string.technique_myo_match
        else -> R.string.technique_rest_pause
    }
)

/** How to do it, in one sentence. */
@Composable
fun techniqueHelp(t: TechniqueView): String {
    val d = Techniques.defaults(t.type)
    return when (t.type) {
        Techniques.DROP_SET -> stringResource(R.string.technique_help_drop, t.loadDropPercent ?: d.loadDropPercent ?: 20, t.restSeconds)
        Techniques.MYO_REPS -> stringResource(
            R.string.technique_help_myo, t.restSeconds, t.segmentMinRepetitions ?: d.segmentMinRepetitions ?: 3,
            t.segmentMaxRepetitions ?: d.segmentMaxRepetitions ?: 5
        )
        Techniques.MATCH -> stringResource(R.string.technique_help_match, t.restSeconds)
        else -> stringResource(R.string.technique_help_rest_pause, t.restSeconds)
    }
}

/** Why the plan added a technique or a superset; null for the user's own and for a reason this version does not know. */
@Composable
fun techniqueReason(reason: String?): String? = when (reason) {
    "PriorityMuscle" -> stringResource(R.string.technique_reason_priority)
    "ShortOnTime" -> stringResource(R.string.technique_reason_time)
    else -> null
}

/** "Drop set · Last set" and how to do it. */
@Composable
fun techniqueSummary(t: TechniqueView): String =
    techniqueName(t.type, t.maxSegments) + " · " +
        stringResource(if (t.appliesTo == Techniques.ALL_SETS) R.string.technique_scope_all else R.string.technique_scope_last)
