package io.github.eleasarb.glagolik.ui.verbs

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.eleasarb.glagolik.data.repository.ProgressRepository
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.MasteryCounts
import io.github.eleasarb.glagolik.domain.model.MasteryLevel
import io.github.eleasarb.glagolik.domain.model.VerbDetail
import io.github.eleasarb.glagolik.domain.model.masteryLevel
import io.github.eleasarb.glagolik.ui.navigation.VERB_ID_ARG
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@Immutable
sealed interface VerbDetailUiState {
    data object Loading : VerbDetailUiState

    data object NotFound : VerbDetailUiState

    data class Success(
        val verb: VerbDetail,
        /** Level of every trainable form; forms marked NO_TRAINING have no entry. */
        val mastery: Map<FormKey, MasteryLevel> = emptyMap(),
    ) : VerbDetailUiState {
        val masteryCounts: MasteryCounts get() = MasteryCounts.of(mastery.values)
    }
}

@HiltViewModel
class VerbDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VerbRepository,
    private val progress: ProgressRepository,
) : ViewModel() {
    private val verbId: Int = checkNotNull(savedStateHandle[VERB_ID_ARG])

    private val detail = flow { emit(repository.getVerbDetail(verbId)) }

    val uiState: StateFlow<VerbDetailUiState> = combine(
        detail,
        progress.observeCards(verbId),
    ) { verb, cards ->
        if (verb == null) {
            VerbDetailUiState.NotFound
        } else {
            VerbDetailUiState.Success(
                verb = verb,
                mastery = verb.forms
                    .filter { it.isTrainable }
                    .associate { it.key to cards[it.key].masteryLevel() },
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = VerbDetailUiState.Loading,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
