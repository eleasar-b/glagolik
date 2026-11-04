package io.github.eleasarb.glagolik.ui.verbs

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import io.github.eleasarb.glagolik.domain.model.VerbDetail
import io.github.eleasarb.glagolik.ui.navigation.VERB_ID_ARG
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@Immutable
sealed interface VerbDetailUiState {
    data object Loading : VerbDetailUiState

    data object NotFound : VerbDetailUiState

    data class Success(val verb: VerbDetail) : VerbDetailUiState
}

@HiltViewModel
class VerbDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VerbRepository,
) : ViewModel() {
    private val verbId: Int = checkNotNull(savedStateHandle[VERB_ID_ARG])

    val uiState: StateFlow<VerbDetailUiState> = flow {
        val detail = repository.getVerbDetail(verbId)
        emit(if (detail == null) VerbDetailUiState.NotFound else VerbDetailUiState.Success(detail))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = VerbDetailUiState.Loading,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
