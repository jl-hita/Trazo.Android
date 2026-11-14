package com.trazo.android.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.android.data.model.ExerciseDetail
import com.trazo.android.data.model.ExerciseListItem
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.ExerciseFilters
import com.trazo.android.data.repo.ExerciseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogState(
    val filters: ExerciseFilters = ExerciseFilters(),
    val items: List<ExerciseListItem> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
    val totalCount: Int = 0,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: ApiError? = null
) {
    val canLoadMore: Boolean get() = page < totalPages && !loading && !loadingMore
}

/** Search and filters over `/api/v1/exercises`, with paging. Typing waits a moment before querying. */
class CatalogViewModel(private val exercises: ExerciseRepository, private val debounceMillis: Long = 350) : ViewModel() {
    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()
    private var job: Job? = null

    init { reload(0) }

    fun setQuery(query: String) {
        _state.update { it.copy(filters = it.filters.copy(query = query)) }
        reload(debounceMillis)
    }

    fun setFilters(transform: (ExerciseFilters) -> ExerciseFilters) {
        _state.update { it.copy(filters = transform(it.filters)) }
        reload(0)
    }

    fun clearFilters() = setFilters { it.copy(movementPattern = null, experience = null, equipment = null, muscle = null) }

    fun retry() = reload(0)

    fun loadMore() {
        val s = _state.value
        if (!s.canLoadMore) return
        _state.update { it.copy(loadingMore = true) }
        job = viewModelScope.launch {
            when (val r = exercises.search(s.filters, s.page + 1)) {
                is ApiResult.Success -> _state.update {
                    it.copy(items = it.items + r.value.items, page = r.value.page, totalPages = r.value.totalPages, loadingMore = false)
                }
                is ApiResult.Failure -> _state.update { it.copy(loadingMore = false, error = r.error) }
            }
        }
    }

    private fun reload(delayMillis: Long) {
        job?.cancel()
        _state.update { it.copy(loading = true, loadingMore = false, error = null) }
        job = viewModelScope.launch {
            if (delayMillis > 0) delay(delayMillis)
            val filters = _state.value.filters
            when (val r = exercises.search(filters, 1)) {
                is ApiResult.Success -> _state.update {
                    it.copy(items = r.value.items, page = r.value.page, totalPages = r.value.totalPages, totalCount = r.value.totalCount, loading = false)
                }
                is ApiResult.Failure -> _state.update { it.copy(items = emptyList(), totalCount = 0, loading = false, error = r.error) }
            }
        }
    }
}

data class ExerciseDetailState(val loading: Boolean = true, val detail: ExerciseDetail? = null, val error: ApiError? = null)

class ExerciseDetailViewModel(private val exercises: ExerciseRepository, private val id: String) : ViewModel() {
    private val _state = MutableStateFlow(ExerciseDetailState())
    val state: StateFlow<ExerciseDetailState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = ExerciseDetailState()
        viewModelScope.launch {
            _state.value = when (val r = exercises.detail(id)) {
                is ApiResult.Success -> ExerciseDetailState(false, r.value)
                is ApiResult.Failure -> ExerciseDetailState(false, error = r.error)
            }
        }
    }
}
