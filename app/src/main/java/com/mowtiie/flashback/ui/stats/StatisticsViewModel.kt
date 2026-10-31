package com.mowtiie.flashback.ui.stats

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.repository.StatisticsSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone

@Immutable
data class StatisticsUiState(
    /** False until the first load lands, so the empty state doesn't flash. */
    val loaded: Boolean = false,
    val data: StatisticsData = StatisticsData(),
)

class StatisticsViewModel(
    private val source: StatisticsSource,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> TimeZone = TimeZone::getDefault,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    /** Called on resume too, since reviews are made on other screens. */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val data = StatisticsAssembler.assemble(source, clock(), zone())
            _uiState.value = StatisticsUiState(loaded = true, data = data)
        }
    }
}
