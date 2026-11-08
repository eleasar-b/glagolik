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
import io.github.eleasarb.glagolik.domain.model.ExerciseKind
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.MasteryCounts
import io.github.eleasarb.glagolik.domain.model.MasteryLevel
import io.github.eleasarb.glagolik.domain.model.VerbDetail
import io.github.eleasarb.glagolik.domain.model.hasMeaningCards
import io.github.eleasarb.glagolik.domain.model.masteryLevel
import io.github.eleasarb.glagolik.ui.navigation.VERB_ID_ARG
import java.time.Clock
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
        /**
         * Level of the meaning cards (RU→DE, DE→RU); empty for verbs without meaning cards
         * (reference verbs, missing translation).
         */
        val meaning: Map<Exercise, MasteryLevel> = emptyMap(),
        /** Trainable cards of this verb (forms and meaning) due when the state was built. */
        val dueNow: Int = 0,
        /** A Russian voice is available; infinitive and forms can be spoken. */
        val canSpeak: Boolean = false,
    ) : VerbDetailUiState {
        val masteryCounts: MasteryCounts get() = MasteryCounts.of(mastery.values)

        /** Both meaning directions; `null` for verbs without meaning cards. */
        val meaningCounts: MasteryCounts?
            get() = meaning.takeIf { it.isNotEmpty() }?.let { MasteryCounts.of(it.values) }
    }
}

@HiltViewModel
class VerbDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VerbRepository,
    private val progress: ProgressRepository,
    private val speaker: Speaker,
    private val clock: Clock,
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
            val trainable = verb.forms.filter { it.isTrainable }.map { Exercise.Form(it.key) } +
                if (verb.isCore &&
                    verb.summary.hasMeaningCards()
                ) {
                    Exercise.MEANINGS
                } else {
                    emptyList()
                }
            val now = clock.instant()
            VerbDetailUiState.Success(
                verb = verb,
                mastery = trainable.filterIsInstance<Exercise.Form>()
                    .associate { it.form to cards[it].masteryLevel() },
                meaning = trainable.filter { it.kind == ExerciseKind.MEANING }
                    .associateWith { cards[it].masteryLevel() },
                dueNow = trainable.count { cards[it]?.due?.isAfter(now) == false },
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
