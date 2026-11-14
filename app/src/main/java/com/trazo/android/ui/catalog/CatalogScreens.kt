package com.trazo.android.ui.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.android.R
import com.trazo.android.data.model.ExerciseListItem
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoField
import com.trazo.android.ui.theme.TrazoLink
import com.trazo.android.ui.theme.TrazoPalette

@Composable
fun CatalogScreen(viewModel: CatalogViewModel, onOpen: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Load the next page when the user gets close to the end of the list.
    val nearEnd by remember { derivedStateOf { listState.layoutInfo.let { (it.visibleItemsInfo.lastOrNull()?.index ?: 0) >= it.totalItemsCount - 4 } } }
    LaunchedEffect(nearEnd, state.items.size) { if (nearEnd) viewModel.loadMore() }

    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.catalog_title))
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            TrazoField(state.filters.query, viewModel::setQuery, Modifier.weight(1f), { Text(stringResource(R.string.catalog_search)) })
            TextButton(onClick = { showFilters = !showFilters }, modifier = Modifier.heightIn(min = MinTouch)) {
                Text(stringResource(R.string.catalog_filters) + if (state.filters.active) " •" else "")
            }
        }
        if (showFilters) FiltersPanel(state.filters, viewModel)
        when {
            state.loading -> LoadingBox()
            state.error != null && state.items.isEmpty() -> ErrorBox(state.error!!, viewModel::retry)
            state.items.isEmpty() -> EmptyBox(stringResource(R.string.catalog_empty))
            else -> LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text(stringResource(R.string.catalog_count, state.totalCount), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall) }
                items(state.items, key = { it.id }) { ExerciseRow(it) { onOpen(it.id) } }
                if (state.loadingMore) item { LoadingBox(Modifier.height(80.dp)) }
                state.error?.let { err -> item { InlineError(err); TrazoLink(onClick = viewModel::loadMore) { Text(stringResource(R.string.retry)) } } }
            }
        }
    }
}

@Composable
private fun FiltersPanel(filters: com.trazo.android.data.repo.ExerciseFilters, vm: CatalogViewModel) {
    Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(stringResource(R.string.catalog_pattern))
        ChipGroup(Labels.movementPatterns, setOfNotNull(filters.movementPattern), Labels::pattern, onToggle = { v -> vm.setFilters { it.copy(movementPattern = if (it.movementPattern == v) null else v) } })
        SectionTitle(stringResource(R.string.catalog_difficulty))
        ChipGroup(Labels.experienceLevels, setOfNotNull(filters.experience), Labels::experience, onToggle = { v -> vm.setFilters { it.copy(experience = if (it.experience == v) null else v) } })
        SectionTitle(stringResource(R.string.catalog_equipment))
        ChipGroup(Labels.commonEquipment, setOfNotNull(filters.equipment), Labels::equipment, onToggle = { v -> vm.setFilters { it.copy(equipment = if (it.equipment == v) null else v) } })
        SectionTitle(stringResource(R.string.catalog_muscle))
        ChipGroup(Labels.muscleGroups, setOfNotNull(filters.muscle), Labels::muscle, onToggle = { v -> vm.setFilters { it.copy(muscle = if (it.muscle == v) null else v) } })
        if (filters.active) TrazoLink(onClick = vm::clearFilters, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.catalog_clear_filters)) }
    }
}

@Composable
private fun ExerciseRow(item: ExerciseListItem, onClick: () -> Unit) {
    TrazoCard(onClick = onClick) {
        Text(item.nameEs, style = MaterialTheme.typography.titleMedium)
        Text(Labels.pattern(item.movementPattern) + " · " + Labels.muscles(item.primaryMuscles), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text(
            item.requiredEquipment.takeIf { it.isNotEmpty() }?.joinToString(", ") { Labels.equipment(it) } ?: stringResource(R.string.exercise_no_equipment),
            color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ExerciseDetailScreen(viewModel: ExerciseDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(state.detail?.nameEs ?: stringResource(R.string.catalog_title), onBack)
        val d = state.detail
        when {
            state.loading -> LoadingBox()
            d == null -> ErrorBox(state.error ?: com.trazo.android.data.net.ApiError(com.trazo.android.data.net.ErrorKind.Unknown), viewModel::load)
            else -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(d.nameEs, style = MaterialTheme.typography.headlineSmall)
                if (d.nameEn.isNotBlank()) Text(d.nameEn, color = TrazoPalette.Muted)
                if (d.descriptionEs.isNotBlank()) Text(d.descriptionEs)
                SectionTitle(stringResource(R.string.exercise_primary))
                Text(Labels.muscles(d.primaryMuscles))
                if (d.secondaryMuscles.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.exercise_secondary))
                    Text(Labels.muscles(d.secondaryMuscles))
                }
                SectionTitle(stringResource(R.string.exercise_equipment))
                Text(d.requiredEquipment.takeIf { it.isNotEmpty() }?.joinToString(", ") { Labels.equipment(it) } ?: stringResource(R.string.exercise_no_equipment))
                SectionTitle(stringResource(R.string.exercise_level))
                Text(Labels.difficulty(d.difficulty) + " · " + Labels.pattern(d.movementPattern))
                if (d.instructionsEs.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.exercise_instructions))
                    d.instructionsEs.forEachIndexed { i, step -> Text("${i + 1}. $step") }
                }
                if (d.tipsEs.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.exercise_tips))
                    d.tipsEs.forEach { Text("• $it") }
                }
            }
        }
    }
}
