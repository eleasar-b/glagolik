package io.github.eleasarb.glagolik.ui.verbs

import androidx.lifecycle.SavedStateHandle
import io.github.eleasarb.glagolik.domain.model.VerbDetail
import io.github.eleasarb.glagolik.testing.FakeVerbRepository
import io.github.eleasarb.glagolik.testing.MainDispatcherRule
import io.github.eleasarb.glagolik.testing.verbSummary
import io.github.eleasarb.glagolik.ui.navigation.VERB_ID_ARG
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
        forms = emptyList(),
        partners = listOf(verbSummary(6, "говорить")),
    )
    private val repository = FakeVerbRepository(verbs = emptyList(), details = mapOf(2 to detail))

    @Test
    fun `known id gives success`() = runTest {
        val viewModel = viewModelFor(2)
        verbDetailViewModelCollected(viewModel)
        assertEquals(VerbDetailUiState.Success(detail), viewModel.uiState.value)
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

    private fun viewModelFor(id: Int) =
        VerbDetailViewModel(SavedStateHandle(mapOf(VERB_ID_ARG to id)), repository)

    /** Collects uiState eagerly, as the UI would, so WhileSubscribed upstreams run. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.verbDetailViewModelCollected(viewModel: VerbDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
    }
}
