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
import com.trazo.android.ui.plan.MusclePriorities
import com.trazo.android.ui.plan.MusclePriorityEditor
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoLink
import com.trazo.android.ui.theme.TrazoPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onBack: () -> Unit, onPremium: () -> Unit, onManualPlan: () -> Unit, onCreated: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.created) { if (state.created) onCreated() }
    // The account turned out not to have Premium (the app could not know, or it lapsed): same choice as before opening this form.
    var promptDismissed by remember(state.error) { mutableStateOf(false) }
    var showEquipmentDialog by remember { mutableStateOf(false) }
    val availableEquipmentOptions = state.options?.equipment.orEmpty()
    if (state.error?.kind == com.trazo.android.data.net.ErrorKind.SubscriptionRequired && !promptDismissed) {
        com.trazo.android.ui.billing.PremiumRequiredDialog(onPremium = onPremium, onManualPlan = onManualPlan, onDismiss = { promptDismissed = true })
    }
    if (showEquipmentDialog && state.options != null) {
        val equipmentOptions = availableEquipmentOptions
        AlertDialog(
            onDismissRequest = { showEquipmentDialog = false },
            title = { Text(stringResource(R.string.onb_equipment)) },
            text = {
                Column(
                    Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    equipmentOptions.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = MinTouch),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item in state.equipment,
                                onCheckedChange = { viewModel.toggleEquipment(item) }
                            )
                            Text(
                                Labels.equipment(item),
                                modifier = Modifier.weight(1f),
                                color = TrazoPalette.Text
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEquipmentDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
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

                // The starting point is only offered when the server announces the preparation.
                options.preparation?.let { prep ->
                    SectionTitle(stringResource(R.string.onb_start))
                    val context = androidx.compose.ui.platform.LocalContext.current
                    ChipGroup(
                        prep.readiness.filter { it in listOf(PreparationChoice.REGULAR, PreparationChoice.NEW, PreparationChoice.RETURNING) },
                        setOf(state.readiness),
                        {
                            context.getString(when (it) {
                                PreparationChoice.NEW -> R.string.onb_ready_new
                                PreparationChoice.RETURNING -> R.string.onb_ready_returning
                                else -> R.string.onb_ready_regular
                            })
                        },
                        viewModel::setReadiness
                    )
                    if (state.readiness != PreparationChoice.REGULAR) {
                        Text(stringResource(R.string.onb_ready_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        if (state.readiness == PreparationChoice.RETURNING) {
                            ChipGroup(
                                listOf("short", "long"), setOf(if (state.longBreak) "long" else "short"),
                                { context.getString(if (it == "long") R.string.onb_break_long else R.string.onb_break_short, prep.longBreakMonths) },
                                { viewModel.setLongBreak(it == "long") }
                            )
                        }
                        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.onb_prep_weeks, state.preparationWeeks, state.preparationWeeks))
                        Slider(
                            value = state.preparationWeeks.toFloat(),
                            onValueChange = { viewModel.setPreparationWeeks(it.toInt()) },
                            valueRange = prep.minimumWeeks.toFloat()..prep.maximumWeeks.toFloat(),
                            steps = (prep.maximumWeeks - prep.minimumWeeks - 1).coerceAtLeast(0),
                            modifier = Modifier.heightIn(min = MinTouch)
                        )
                        val after = PreparationChoice.weeksAfter(state.totalWeeks, state.preparationWeeks)
                        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.onb_prep_then, after, after), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // The mode is only offered when the server announces it, so an older server keeps the fixed-days flow.
                if ("Sequence" in options.schedulingModes) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    SectionTitle(stringResource(R.string.onb_mode))
                    ChipGroup(
                        listOf("Calendar", "Sequence"), setOf(state.schedulingMode),
                        { context.getString(if (it == "Sequence") R.string.onb_mode_sequence else R.string.onb_mode_calendar) },
                        viewModel::setSchedulingMode
                    )
                    if (state.isSequence) Text(stringResource(R.string.onb_mode_sequence_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }

                if (state.isSequence) {
                    SectionTitle(stringResource(R.string.onb_sessions_per_cycle))
                    Text(stringResource(R.string.onb_sessions_hint, options.minimumSessionsPerPeriod, options.maximumSessionsPerPeriod), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    ChipGroup(
                        (options.minimumSessionsPerPeriod..options.maximumSessionsPerPeriod).map { it.toString() }, setOf(state.sessionsPerPeriod.toString()),
                        { it }, { viewModel.setSessionsPerPeriod(it.toInt()) }
                    )
                } else {
                    SectionTitle(stringResource(R.string.onb_days))
                    Text(stringResource(R.string.onb_days_hint, options.minimumTrainingDays, options.maximumTrainingDays), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    DayPicker(options.days, state.days, viewModel::toggleDay)
                }

                SectionTitle(stringResource(R.string.onb_split))
                Text(stringResource(R.string.onb_split_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                ChipGroup((listOf("Automatic") + options.splits).distinct(), setOf(state.split), Labels::split, viewModel::setSplit)

                Row(Modifier.fillMaxWidth().heightIn(min = MinTouch), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle(stringResource(R.string.onb_deload))
                        Text(stringResource(R.string.onb_deload_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = state.includeDeload, onCheckedChange = viewModel::setIncludeDeload)
                }
                if (state.includeDeload) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null, 4, 5, 6).forEach { every ->
                            FilterChip(
                                selected = state.deloadEvery == every, onClick = { viewModel.setDeloadEvery(every) },
                                label = { Text(if (every == null) stringResource(R.string.onb_deload_auto) else stringResource(R.string.onb_deload_every, every)) },
                                modifier = Modifier.heightIn(min = MinTouch),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TrazoPalette.PrimaryButton, selectedLabelColor = androidx.compose.ui.graphics.Color.White)
                            )
                        }
                    }
                }

                Text(stringResource(R.string.onb_minutes,state.sessionMinutes), modifier = Modifier.padding(top = 12.dp))
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
                    valueRange = (state.options?.preparation?.let { PreparationChoice.minimumTotalWeeks(options.minimumTotalWeeks, state.preparationWeeks, it) } ?: options.minimumTotalWeeks)
                        .toFloat()..options.maximumTotalWeeks.toFloat(),
                    modifier = Modifier.heightIn(min = MinTouch)
                )

                SectionTitle(stringResource(R.string.onb_start_date))
                StartDatePicker(state.startDate, viewModel::setStartDate)

                SectionTitle(stringResource(R.string.onb_equipment))
                Text(stringResource(R.string.onb_equipment_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = { showEquipmentDialog = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)
                ) {
                    Text(if (state.equipment.isEmpty()) stringResource(R.string.onb_equipment_select)
                        else stringResource(R.string.onb_equipment_selected_count, state.equipment.size))
                }
                if (state.equipment.isNotEmpty()) {
                    Text(
                        state.equipment.sorted().take(4).joinToString(" · ", transform = Labels::equipment) +
                            if (state.equipment.size > 4) " · +${state.equipment.size - 4}" else "",
                        color = TrazoPalette.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                SectionTitle(stringResource(R.string.onb_limitations))
                Text(stringResource(R.string.onb_limitations_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                ChipGroup(options.limitations, state.limitations, Labels::limitation, viewModel::toggleLimitation)

                if (state.goal == "Hypertrophy") {
                    SectionTitle(stringResource(R.string.onb_priorities))
                    Text(stringResource(R.string.onb_priorities_hint, MusclePriorities.MAX_EMPHASIZED), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    MusclePriorityEditor(state.musclePriorities, viewModel::setMusclePriority)
                }

                state.error?.let { InlineError(it, Modifier.padding(top = 8.dp)) }
                if (state.error?.kind == com.trazo.android.data.net.ErrorKind.SubscriptionRequired) {
                    TrazoLink(onClick = onPremium, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.premium_get)) }
                }
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

/** Today (the default) or a later day picked from a calendar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDatePicker(selected: java.time.LocalDate?, onChange: (java.time.LocalDate?) -> Unit) {
    val today = java.time.LocalDate.now()
    var picking by remember { mutableStateOf(false) }
    val other = stringResource(R.string.finish_when_other)
    val todayLabel = stringResource(R.string.finish_when_today)
    val otherLabel = selected?.let { java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).format(it) } ?: other
    ChipGroup(
        listOf("today", "other"), setOf(if (selected == null) "today" else "other"),
        { if (it == "today") todayLabel else otherLabel },
        { if (it == "today") onChange(null) else picking = true }
    )
    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (selected ?: today).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate().isBefore(today)
            }
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val date = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        onChange(date.takeIf { d -> d.isAfter(today) })
                    }
                    picking = false
                }) { Text(stringResource(R.string.finish_when_ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state) }
    }
}
