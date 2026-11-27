package com.trazo.android.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trazo.android.R
import com.trazo.android.data.model.ProgramSheetView
import com.trazo.android.data.model.ProgramView
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.diagnostics.launchGuarded
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoPalette
import com.trazo.android.ui.workout.WorkoutViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProgramsState(val loading: Boolean = true, val programs: List<ProgramView> = emptyList(), val error: ApiError? = null)

/** The ready-made programs the server offers. */
class ProgramsViewModel(private val plans: PlanRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProgramsState())
    val state: StateFlow<ProgramsState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launchGuarded(R.string.feature_onboarding, onFailure = { _state.update { it.copy(loading = false, error = ApiError(ErrorKind.Unknown)) } }) {
            when (val result = plans.programs()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, programs = result.value) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }
}

@Composable
fun ProgramsScreen(viewModel: ProgramsViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.programs_title), onBack)
        val error = state.error
        when {
            state.loading -> LoadingBox()
            error != null -> ErrorBox(error, viewModel::load)
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.programs, key = { it.id }) { program ->
                    TrazoCard(Modifier.fillMaxWidth().clickable { onOpen(program.id) }) {
                        Text(program.name, style = MaterialTheme.typography.titleMedium)
                        Text(program.description, color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(programFacts(program), color = TrazoPalette.Link, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/** "4 days a week · 60 min per session · From level: Intermediate", with a Premium mark when it cannot be started yet. */
@Composable
private fun programFacts(program: ProgramView): String = buildList {
    add(stringResource(R.string.program_days, program.sessionsPerWeek))
    add(stringResource(R.string.program_minutes, program.sessionMinutes))
    add(stringResource(R.string.program_level, Labels.experience(program.minimumExperience)))
    if (program.locked) add("Premium")
}.joinToString(" · ")

/** What a program looks like for the chosen equipment: weekly sets per muscle and one session. */
@Composable
fun ProgramSheet(sheet: ProgramSheetView) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(sheet.program.description, color = TrazoPalette.TextSecondary)
        Text(programFacts(sheet.program), color = TrazoPalette.Link, style = MaterialTheme.typography.labelMedium)
        if (sheet.equipment.isNotEmpty()) {
            Text(sheet.equipment.joinToString(" · ", transform = Labels::equipment), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        SectionTitle(stringResource(R.string.program_weekly_sets))
        TrazoCard(Modifier.fillMaxWidth()) {
            sheet.weeklySets.forEach { Text("${Labels.muscle(it.muscle)}: ${WorkoutViewModel.formatKg(it.weeklySets)}") }
        }
        SectionTitle(stringResource(R.string.program_sample))
        TrazoCard(Modifier.fillMaxWidth()) {
            Text(sheet.sampleSession.title + " · " + stringResource(R.string.plan_session_minutes, sheet.sampleSession.estimatedDurationMinutes), style = MaterialTheme.typography.titleSmall)
            sheet.sampleSession.exercises.forEach {
                Text(it.name + " — " + stringResource(R.string.session_sets_reps, it.sets, it.minRepetitions, it.maxRepetitions))
            }
        }
    }
}
