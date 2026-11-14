package com.trazo.android.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.android.R
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoPalette

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onBack: () -> Unit, onCreated: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.created) { if (state.created) onCreated() }
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.onb_title), onBack)
        val options = state.options
        when {
            state.loadingOptions -> LoadingBox()
            options == null -> ErrorBox(state.error ?: com.trazo.android.data.net.ApiError(com.trazo.android.data.net.ErrorKind.Unknown), viewModel::loadOptions)
            else -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.onb_intro), color = TrazoPalette.TextSecondary)

                SectionTitle(stringResource(R.string.onb_goal))
                ChipGroup(options.goals, setOf(state.goal), Labels::goal, viewModel::setGoal)

                SectionTitle(stringResource(R.string.onb_level))
                ChipGroup(options.experience, setOf(state.experience), Labels::experience, viewModel::setExperience)

                SectionTitle(stringResource(R.string.onb_days))
                Text(stringResource(R.string.onb_days_hint, options.minimumTrainingDays, options.maximumTrainingDays), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                DayPicker(options.days, state.days, viewModel::toggleDay)

                Text(stringResource(R.string.onb_minutes, state.sessionMinutes), modifier = Modifier.padding(top = 12.dp))
                Slider(
                    value = state.sessionMinutes.toFloat(),
                    onValueChange = { viewModel.setSessionMinutes((it / 5).toInt() * 5) },
                    valueRange = options.minimumSessionMinutes.toFloat()..options.maximumSessionMinutes.toFloat(),
                    modifier = Modifier.heightIn(min = MinTouch)
                )
                Text(stringResource(R.string.onb_weeks, state.totalWeeks))
                Slider(
                    value = state.totalWeeks.toFloat(),
                    onValueChange = { viewModel.setTotalWeeks(it.toInt()) },
                    valueRange = options.minimumTotalWeeks.toFloat()..options.maximumTotalWeeks.toFloat(),
                    modifier = Modifier.heightIn(min = MinTouch)
                )

                SectionTitle(stringResource(R.string.onb_equipment))
                Text(stringResource(R.string.onb_equipment_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                ChipGroup(options.equipment, state.equipment, Labels::equipment, viewModel::toggleEquipment)

                SectionTitle(stringResource(R.string.onb_limitations))
                Text(stringResource(R.string.onb_limitations_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                ChipGroup(options.limitations, state.limitations, Labels::limitation, viewModel::toggleLimitation)

                state.error?.let { InlineError(it, Modifier.padding(top = 8.dp)) }
            }
        }
        if (options != null) {
            TrazoButton(
                onClick = viewModel::submit, enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = MinTouch)
            ) { Text(stringResource(if (state.submitting) R.string.onb_creating else R.string.onb_create)) }
        }
    }
}
