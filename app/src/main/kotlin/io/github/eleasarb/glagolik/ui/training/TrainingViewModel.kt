package io.github.eleasarb.glagolik.ui.training

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.eleasarb.glagolik.data.repository.ProgressRepository
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import io.github.eleasarb.glagolik.data.settings.SettingsRepository
import io.github.eleasarb.glagolik.domain.checking.AnswerChecker
import io.github.eleasarb.glagolik.domain.checking.AnswerResult
import io.github.eleasarb.glagolik.domain.model.ConjugatedForm
import io.github.eleasarb.glagolik.domain.srs.Rating
import io.github.eleasarb.glagolik.domain.usecase.BuildSessionUseCase
import io.github.eleasarb.glagolik.domain.usecase.SessionCard
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
sealed interface TrainingUiState {
    data object Loading : TrainingUiState

    /** No due forms and no new forms left for today. */
    data object NothingToDo : TrainingUiState

    data class Question(
        val card: SessionCard,
        /** 1-based position of this card in the current queue. */
        val position: Int,
        val total: Int,
        val input: String = "",
        /** `null` while waiting for an answer. */
        val feedback: AnswerResult? = null,
        /**
         * All forms of the asked tense, shown after a wrong answer. Empty otherwise and while
         * they are loading.
         */
        val tenseForms: List<ConjugatedForm> = emptyList(),
    ) : TrainingUiState {
        /** Correct or Incorrect: the card is graded and the next step is "continue". */
        val isGraded: Boolean
            get() = feedback is AnswerResult.Correct || feedback is AnswerResult.Incorrect
    }

    data class Finished(val cards: Int, val correctFirstTry: Int) : TrainingUiState
}

/**
 * Runs one session. Rating (CLAUDE.md §6): wrong = AGAIN, right = GOOD, right + "easy" =
 * EASY. The rating is stored when the learner continues. Wrong cards come back at the end
 * of the session until they are answered correctly.
 */
@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val buildSession: BuildSessionUseCase,
    private val progress: ProgressRepository,
    private val verbs: VerbRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TrainingUiState>(TrainingUiState.Loading)
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    private val queue = ArrayDeque<SessionCard>()
    private var position = 0
    private var sessionCards = 0
    private var correctFirstTry = 0
    private val failedOnce = mutableSetOf<SessionCard>()
    private var checker = AnswerChecker()
    private var shownAt: Instant = clock.instant()
    private var answerDuration: Duration? = null

    init {
        viewModelScope.launch {
            checker = AnswerChecker(strictYo = settings.current().strictYo)
            val session = buildSession()
            queue.addAll(session.cards)
            sessionCards = session.cards.size
            if (queue.isEmpty()) {
                _uiState.value = TrainingUiState.NothingToDo
            } else {
                showNext()
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { state ->
            if (state is TrainingUiState.Question && !state.isGraded) {
                state.copy(input = text, feedback = null)
            } else {
                state
            }
        }
    }

    fun onSubmit() {
        val state = _uiState.value as? TrainingUiState.Question ?: return
        if (state.isGraded) return
        val result = checker.check(state.input, state.card.form.expected)
        if (result is AnswerResult.Correct || result is AnswerResult.Incorrect) {
            answerDuration = Duration.between(shownAt, clock.instant())
        }
        _uiState.value = state.copy(feedback = result)
        if (result is AnswerResult.Incorrect) showTense(state)
    }

    private fun showTense(state: TrainingUiState.Question) {
        val ref = state.card.form.ref
        viewModelScope.launch {
            val forms = verbs.getVerbDetail(ref.verbId)?.formsOf(ref.form.tense).orEmpty()
            _uiState.update { current ->
                // The learner may have moved on while the forms were loading.
                if (current is TrainingUiState.Question && current.position == state.position) {
                    current.copy(tenseForms = forms)
                } else {
                    current
                }
            }
        }
    }

    /** Stores the rating and moves on. [easy] only counts after a correct answer. */
    fun onContinue(easy: Boolean = false) {
        val state = _uiState.value as? TrainingUiState.Question ?: return
        val rating = when (state.feedback) {
            is AnswerResult.Correct -> if (easy) Rating.EASY else Rating.GOOD
            is AnswerResult.Incorrect -> Rating.AGAIN
            else -> return
        }
        val card = state.card
        queue.removeFirst()
        if (rating == Rating.AGAIN) {
            failedOnce += card
            queue.addLast(card)
        } else if (card !in failedOnce) {
            correctFirstTry++
        }
        val duration = answerDuration
        viewModelScope.launch {
            progress.recordReview(card.form.ref, rating, clock.instant(), duration)
        }
        if (queue.isEmpty()) {
            _uiState.value = TrainingUiState.Finished(sessionCards, correctFirstTry)
        } else {
            showNext()
        }
    }

    private fun showNext() {
        position++
        shownAt = clock.instant()
        answerDuration = null
        _uiState.value = TrainingUiState.Question(
            card = queue.first(),
            position = position,
            total = position - 1 + queue.size,
        )
    }
}
