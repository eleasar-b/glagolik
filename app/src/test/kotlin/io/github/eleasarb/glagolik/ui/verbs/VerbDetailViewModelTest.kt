package io.github.eleasarb.glagolik.ui.verbs

import androidx.lifecycle.SavedStateHandle
import io.github.eleasarb.glagolik.data.tts.SpeechAvailability
import io.github.eleasarb.glagolik.domain.model.ConjugatedForm
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.MasteryLevel
import io.github.eleasarb.glagolik.domain.model.VerbDetail
import io.github.eleasarb.glagolik.domain.srs.Rating
import io.github.eleasarb.glagolik.testing.FakeProgressRepository
import io.github.eleasarb.glagolik.testing.FakeSpeaker
import io.github.eleasarb.glagolik.testing.FakeVerbRepository
import io.github.eleasarb.glagolik.testing.MainDispatcherRule
import io.github.eleasarb.glagolik.testing.formKey
import io.github.eleasarb.glagolik.testing.verbSummary
import io.github.eleasarb.glagolik.ui.navigation.VERB_ID_ARG
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VerbDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val detail = VerbDetail(
        summary = verbSummary(2, "сказать"),
        forms = listOf(
            ConjugatedForm(FormKey.FUTURE_SG1, listOf("скажу́"), isTrainable = true),
            ConjugatedForm(FormKey.PAST_M, listOf("сказа́л"), isTrainable = true),
            ConjugatedForm(FormKey.PAST_N, listOf("сказа́ло"), isTrainable = false),
        ),
        partners = listOf(verbSummary(6, "говорить")),
    )
    private val progress = FakeProgressRepository()
    private val speaker = FakeSpeaker(SpeechAvailability.INITIALIZING)
    private val repository = FakeVerbRepository(verbs = emptyList(), details = mapOf(2 to detail))

    @Test
    fun `known id gives success`() = runTest {
        val viewModel = viewModelFor(2)
        verbDetailViewModelCollected(viewModel)
        val state = viewModel.uiState.value as VerbDetailUiState.Success
        assertEquals(detail, state.verb)
    }

    @Test
    fun `trainable forms have a level, untrained forms none`() = runTest {
        val viewModel = viewModelFor(2)
        verbDetailViewModelCollected(viewModel)

        val state = viewModel.uiState.value as VerbDetailUiState.Success
        assertEquals(
            mapOf(FormKey.FUTURE_SG1 to MasteryLevel.NEW, FormKey.PAST_M to MasteryLevel.NEW),
            state.mastery,
        )
    }

    @Test
    fun `an answer updates the level of the form`() = runTest {
        val viewModel = viewModelFor(2)
        verbDetailViewModelCollected(viewModel)

        progress.recordReview(formKey(2, FormKey.PAST_M), Rating.EASY, NOW)
        progress.recordReview(formKey(3, FormKey.FUTURE_SG1), Rating.EASY, NOW)

        val state = viewModel.uiState.value as VerbDetailUiState.Success
        assertEquals(MasteryLevel.YOUNG, state.mastery[FormKey.PAST_M])
        assertEquals(MasteryLevel.NEW, state.mastery[FormKey.FUTURE_SG1])
        assertEquals(1, state.masteryCounts.young)
    }

    @Test
    fun `forms can be spoken once a voice is ready`() = runTest {
        val viewModel = viewModelFor(2)
        verbDetailViewModelCollected(viewModel)
        assertEquals(false, (viewModel.uiState.value as VerbDetailUiState.Success).canSpeak)

        speaker.availability.value = SpeechAvailability.READY
        viewModel.onSpeak("скажу́")

        assertEquals(true, (viewModel.uiState.value as VerbDetailUiState.Success).canSpeak)
        assertEquals(listOf("скажу́"), speaker.spoken)
    }

    @Test
    fun `unknown id gives not found`() = runTest {
        val viewModel = viewModelFor(999)
        verbDetailViewModelCollected(viewModel)
        assertEquals(VerbDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun `state is loading until collected`() {
        assertEquals(VerbDetailUiState.Loading, viewModelFor(2).uiState.value)
    }

    private fun viewModelFor(id: Int) = VerbDetailViewModel(
        SavedStateHandle(mapOf(VERB_ID_ARG to id)),
        repository,
        progress,
        speaker,
    )

    /** Collects uiState eagerly, as the UI would, so WhileSubscribed upstreams run. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.verbDetailViewModelCollected(viewModel: VerbDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-01-01T08:00:00Z")
    }
}
