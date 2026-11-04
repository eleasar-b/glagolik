package io.github.eleasarb.glagolik.ui.verbs

import io.github.eleasarb.glagolik.testing.FakeVerbRepository
import io.github.eleasarb.glagolik.testing.MainDispatcherRule
import io.github.eleasarb.glagolik.testing.verbSummary
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
    )

    @Test
    fun `initial state is loading`() {
        val viewModel = VerbListViewModel(repository)
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `all verbs are shown for an empty query`() = runTest {
        val viewModel = VerbListViewModel(repository)
        verbListViewModelCollected(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(1, 2, 3), state.verbs.map { it.id })
    }

    @Test
    fun `query filters the list and is reflected in the state`() = runTest {
        val viewModel = VerbListViewModel(repository)
        verbListViewModelCollected(viewModel)

        viewModel.onQueryChange("ска")

        val state = viewModel.uiState.value
        assertEquals("ска", state.query)
        assertEquals(listOf(2), state.verbs.map { it.id })
    }

    @Test
    fun `query without matches gives an empty list`() = runTest {
        val viewModel = VerbListViewModel(repository)
        verbListViewModelCollected(viewModel)

        viewModel.onQueryChange("xyz")

        assertTrue(viewModel.uiState.value.verbs.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    /** Collects uiState eagerly, as the UI would, so WhileSubscribed upstreams run. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.verbListViewModelCollected(viewModel: VerbListViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
    }
}
