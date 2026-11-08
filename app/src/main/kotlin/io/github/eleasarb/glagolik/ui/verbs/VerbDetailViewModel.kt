package io.github.eleasarb.glagolik.ui.verbs

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.eleasarb.glagolik.data.repository.ProgressRepository
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import io.github.eleasarb.glagolik.data.tts.Speaker
import io.github.eleasarb.glagolik.data.tts.SpeechAvailability
import io.github.eleasarb.glagolik.domain.model.Exercise
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
        /** A Russian voice is available; infinitive and forms can be spoken. */
        val canSpeak: Boolean = false,
    ) : VerbDetailUiState {
        val masteryCounts: MasteryCounts get() = MasteryCounts.of(mastery.values)
    }
}

@HiltViewModel
class VerbDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VerbRepository,
    private val progress: ProgressRepository,
    private val speaker: Speaker,
) : ViewModel(speaker) {
    private val verbId: Int = checkNotNull(savedStateHandle[VERB_ID_ARG])

    private val detail = flow { emit(repository.getVerbDetail(verbId)) }

    val uiState: StateFlow<VerbDetailUiState> = combine(
        detail,
        progress.observeCards(verbId),
        speaker.availability,
    ) { verb, cards, availability ->
        if (verb == null) {
            VerbDetailUiState.NotFound
        } else {
            VerbDetailUiState.Success(
                verb = verb,
                mastery = verb.forms
                    .filter { it.isTrainable }
                    .associate { it.key to cards[Exercise.Form(it.key)].masteryLevel() },
                canSpeak = availability == SpeechAvailability.READY,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = VerbDetailUiState.Loading,
    )

    /** Speaks [text] as written (stress marks included). */
    fun onSpeak(text: String) {
        speaker.speak(text)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
