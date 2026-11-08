package io.github.eleasarb.glagolik.ui.training

import io.github.eleasarb.glagolik.data.tts.SpeechAvailability
import io.github.eleasarb.glagolik.domain.checking.AnswerResult
import io.github.eleasarb.glagolik.domain.model.ConjugatedForm
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.TrainableForm
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import io.github.eleasarb.glagolik.domain.model.VerbDetail
import io.github.eleasarb.glagolik.domain.srs.Rating
import io.github.eleasarb.glagolik.domain.usecase.BuildSessionUseCase
import io.github.eleasarb.glagolik.testing.FakeProgressRepository
import io.github.eleasarb.glagolik.testing.FakeSettingsRepository
import io.github.eleasarb.glagolik.testing.FakeSpeaker
import io.github.eleasarb.glagolik.testing.FakeVerbRepository
import io.github.eleasarb.glagolik.testing.MainDispatcherRule
import io.github.eleasarb.glagolik.testing.trainableForm
import io.github.eleasarb.glagolik.testing.verbSummary
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TrainingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val skazhu = trainableForm(2, FormKey.FUTURE_SG1, "скажу́")
    private val idet = trainableForm(9, FormKey.PRESENT_SG3, "идёт")
    private val progress = FakeProgressRepository()
    private val speaker = FakeSpeaker()

    private val future = listOf(
        FormKey.FUTURE_SG1 to "скажу́",
        FormKey.FUTURE_SG2 to "ска́жешь",
        FormKey.FUTURE_PL3 to "ска́жут",
    ).map { (key, form) -> ConjugatedForm(key, listOf(form), isTrainable = true) }
    private val skazatDetail = VerbDetail(
        summary = verbSummary(2, "сказа́ть"),
        forms = future + ConjugatedForm(FormKey.PAST_M, listOf("сказа́л"), isTrainable = true),
        partners = emptyList(),
    )

    private fun viewModel(
        settings: TrainingSettings = TrainingSettings(newFormsPerDay = 1),
        forms: List<TrainableForm> = listOf(skazhu),
        details: Map<Int, VerbDetail> = mapOf(2 to skazatDetail),
    ): TrainingViewModel {
        val clock = Clock.fixed(NOW, ZoneOffset.UTC)
        val settingsRepository = FakeSettingsRepository(settings)
        val verbs = FakeVerbRepository(verbs = emptyList(), details = details, trainable = forms)
        val buildSession = BuildSessionUseCase(
            verbs = verbs,
            progress = progress,
            settings = settingsRepository,
            clock = clock,
            random = Random(seed = 1),
        )
        return TrainingViewModel(buildSession, progress, verbs, settingsRepository, speaker, clock)
    }

    private fun TrainingViewModel.question(): TrainingUiState.Question =
        uiState.value as TrainingUiState.Question

    private fun TrainingViewModel.answer(text: String) {
        onInputChange(text)
        onSubmit()
    }

    @Test
    fun `session starts with the first card`() {
        val question = viewModel().question()

        assertEquals(skazhu, question.card.form)
        assertEquals(1, question.position)
        assertEquals(1, question.total)
        assertEquals(null, question.feedback)
    }

    @Test
    fun `correct answer is rated GOOD on continue and finishes the session`() {
        val vm = viewModel()
        vm.answer("скажу")
        assertEquals(AnswerResult.Correct("скажу́"), vm.question().feedback)

        vm.onContinue()

        assertEquals(listOf(skazhu.ref to Rating.GOOD), progress.reviews)
        assertEquals(TrainingUiState.Finished(cards = 1, correctFirstTry = 1), vm.uiState.value)
    }

    @Test
    fun `easy after a correct answer is rated EASY`() {
        val vm = viewModel()
        vm.answer("скажу")
        vm.onContinue(easy = true)

        assertEquals(listOf(skazhu.ref to Rating.EASY), progress.reviews)
    }

    @Test
    fun `wrong answer is rated AGAIN and the card comes back`() {
        val vm = viewModel()
        vm.answer("скажет")
        assertTrue(vm.question().feedback is AnswerResult.Incorrect)

        vm.onContinue(easy = true) // easy is ignored after a wrong answer

        assertEquals(listOf(skazhu.ref to Rating.AGAIN), progress.reviews)
        val again = vm.question()
        assertEquals(skazhu, again.card.form)
        assertEquals(2, again.position)
        assertEquals("", again.input)

        vm.answer("скажу")
        vm.onContinue()
        assertEquals(TrainingUiState.Finished(cards = 1, correctFirstTry = 0), vm.uiState.value)
    }

    @Test
    fun `wrong answer shows all forms of the asked tense`() {
        val vm = viewModel()
        vm.answer("скажет")

        assertEquals(future, vm.question().tenseForms)

        vm.onContinue()
        assertEquals(emptyList<ConjugatedForm>(), vm.question().tenseForms)
    }

    @Test
    fun `correct answer shows no tense forms`() {
        val vm = viewModel()
        vm.answer("скажу")

        assertEquals(emptyList<ConjugatedForm>(), vm.question().tenseForms)
    }

    @Test
    fun `unknown verb shows the answer without tense forms`() {
        val vm = viewModel(details = emptyMap())
        vm.answer("скажет")

        assertTrue(vm.question().feedback is AnswerResult.Incorrect)
        assertEquals(emptyList<ConjugatedForm>(), vm.question().tenseForms)
    }

    @Test
    fun `the correct form is spoken only after grading`() {
        val vm = viewModel()
        vm.onSpeak()
        vm.answer("скажет")
        assertEquals(emptyList<String>(), speaker.spoken)

        vm.onSpeak()

        assertEquals(listOf("скажу́"), speaker.spoken)
    }

    @Test
    fun `auto play speaks the matched form after a correct answer`() {
        val vm = viewModel(TrainingSettings(newFormsPerDay = 1, autoPlayAudio = true))
        vm.answer("скажу")

        assertEquals(listOf("скажу́"), speaker.spoken)
    }

    @Test
    fun `auto play does not speak for an ungraded input`() {
        val vm = viewModel(TrainingSettings(newFormsPerDay = 1, autoPlayAudio = true))
        vm.answer("ckaжу")

        assertEquals(emptyList<String>(), speaker.spoken)
    }

    @Test
    fun `speaker availability is reflected in the question`() {
        val vm = viewModel()
        assertEquals(true, vm.question().canSpeak)

        speaker.availability.value = SpeechAvailability.NO_ENGINE

        assertEquals(false, vm.question().canSpeak)
    }

    @Test
    fun `latin letters and empty input are not graded`() {
        val vm = viewModel()
        vm.answer("ckaжу")
        assertEquals(AnswerResult.LatinLetters(listOf('c', 'k', 'a')), vm.question().feedback)
        vm.onContinue()
        assertTrue(progress.reviews.isEmpty())

        vm.answer("  ")
        assertEquals(AnswerResult.Empty, vm.question().feedback)
        vm.onContinue()
        assertTrue(progress.reviews.isEmpty())
    }

    @Test
    fun `typing clears a warning but not a graded result`() {
        val vm = viewModel()
        vm.answer("")
        vm.onInputChange("с")
        assertEquals(null, vm.question().feedback)

        vm.answer("скажу")
        vm.onInputChange("x")
        assertEquals("скажу", vm.question().input)
    }

    @Test
    fun `strict yo setting is applied`() {
        val vm = viewModel(TrainingSettings(newFormsPerDay = 1, strictYo = true), listOf(idet))
        vm.answer("идет")

        assertEquals(
            AnswerResult.Incorrect(listOf("идёт"), yoTypedAsYe = true),
            vm.question().feedback,
        )
    }

    @Test
    fun `empty session shows nothing to do`() {
        val vm = viewModel(TrainingSettings(newFormsPerDay = 0))
        assertEquals(TrainingUiState.NothingToDo, vm.uiState.value)
    }

    @Test
    fun `progress counts the remaining queue`() {
        val vm = viewModel(
            TrainingSettings(newFormsPerDay = 2),
            listOf(skazhu, trainableForm(2, FormKey.PAST_M, "сказа́л")),
        )
        assertEquals(2, vm.question().total)
        val first = vm.question().card.form.ref
        vm.answer("неверно")
        vm.onContinue()

        // The failed card goes to the end: one more card in total, the other form is next.
        assertEquals(3, vm.question().total)
        assertEquals(2, vm.question().position)
        assertTrue(vm.question().card.form.ref != first)
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-01-10T07:00:00Z")
    }
}
