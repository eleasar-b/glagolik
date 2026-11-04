package io.github.eleasarb.glagolik.ui.verbs

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class VerbListUiState(
    val query: String = "",
    val verbs: List<VerbSummary> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class VerbListViewModel @Inject constructor(private val repository: VerbRepository) : ViewModel() {
    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val results = query.flatMapLatest { text ->
        repository.observeVerbs(text).map { verbs -> SearchResult(text, verbs) }
    }

    /** The query is taken from [query] directly so the text field never lags behind. */
    val uiState: StateFlow<VerbListUiState> = combine(query, results) { text, result ->
        VerbListUiState(
            query = text,
            verbs = result.verbs,
            isLoading = result.query != text,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = VerbListUiState(),
    )

    fun onQueryChange(text: String) {
        query.value = text
    }

    private data class SearchResult(val query: String, val verbs: List<VerbSummary>)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
