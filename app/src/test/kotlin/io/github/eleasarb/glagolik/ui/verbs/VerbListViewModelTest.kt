package io.github.eleasarb.glagolik.ui.verbs

import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.FormRef
import io.github.eleasarb.glagolik.domain.model.MasteryCounts
import io.github.eleasarb.glagolik.domain.srs.Rating
import io.github.eleasarb.glagolik.domain.usecase.ObserveMasteryUseCase
import io.github.eleasarb.glagolik.testing.FakeProgressRepository
import io.github.eleasarb.glagolik.testing.FakeVerbRepository
import io.github.eleasarb.glagolik.testing.MainDispatcherRule
import io.github.eleasarb.glagolik.testing.trainableForm
import io.github.eleasarb.glagolik.testing.verbSummary
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VerbListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeVerbRepository(
        verbs = listOf(verbSummary(1, "быть"), verbSummary(2, "сказать"), verbSummary(3, "мочь")),
        trainable = listOf(
            trainableForm(2, FormKey.FUTURE_SG1, "скажу́"),
            trainableForm(2, FormKey.PAST_M, "сказа́л"),
            trainableForm(3, FormKey.PAST_M, "мог"),
        ),
    )
    private val progress = FakeProgressRepository()

    private fun viewModel() = VerbListViewModel(
        repository = repository,
        observeMastery = ObserveMasteryUseCase(
            verbs = repository,
            progress = progress,
            clock = Clock.fixed(NOW, ZoneOffset.UTC),
        ),
    )

    @Test
    fun `initial state is loading`() {
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `all verbs are shown for an empty query`() = runTest {
        val viewModel = viewModel()
        verbListViewModelCollected(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(1, 2, 3), state.verbs.map { it.id })
    }

    @Test
    fun `query filters the list and is reflected in the state`() = runTest {
        val viewModel = viewModel()
        verbListViewModelCollected(viewModel)

        viewModel.onQueryChange("ска")

        val state = viewModel.uiState.value
        assertEquals("ска", state.query)
        assertEquals(listOf(2), state.verbs.map { it.id })
    }

    @Test
    fun `query without matches gives an empty list`() = runTest {
        val viewModel = viewModel()
        verbListViewModelCollected(viewModel)

        viewModel.onQueryChange("xyz")

        assertTrue(viewModel.uiState.value.verbs.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `mastery is part of the state`() = runTest {
        val viewModel = viewModel()
        verbListViewModelCollected(viewModel)

        val mastery = viewModel.uiState.value.mastery
        assertEquals(MasteryCounts(new = 3), mastery?.total)
        assertEquals(MasteryCounts(new = 2), mastery?.byVerb?.get(2))
    }

    @Test
    fun `an answer updates the mastery`() = runTest {
        val viewModel = viewModel()
        verbListViewModelCollected(viewModel)

        progress.recordReview(FormRef(2, FormKey.PAST_M), Rating.GOOD, NOW)

        val mastery = viewModel.uiState.value.mastery
        assertEquals(MasteryCounts(new = 1, learning = 1), mastery?.byVerb?.get(2))
        assertEquals(MasteryCounts(new = 2, learning = 1), mastery?.total)
    }

    /** Collects uiState eagerly, as the UI would, so WhileSubscribed upstreams run. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.verbListViewModelCollected(viewModel: VerbListViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-01-01T08:00:00Z")
    }
}
