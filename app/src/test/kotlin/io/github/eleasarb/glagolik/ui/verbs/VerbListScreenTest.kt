package io.github.eleasarb.glagolik.ui.verbs

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.Translation
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VerbListScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val verbs = listOf(
        VerbSummary(1, "быть", Aspect.IMPERFECTIVE, Translation("sein", "be")),
        VerbSummary(2, "сказа́ть", Aspect.PERFECTIVE, Translation("sagen", "say")),
    )

    @Test
    fun `verbs are listed with translation and aspect`() {
        show(VerbListUiState(verbs = verbs, isLoading = false))

        compose.onNodeWithText("быть").assertIsDisplayed()
        compose.onNodeWithText("сказа́ть").assertIsDisplayed()
        compose.onNodeWithText("say").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.aspect_perfective)).assertIsDisplayed()
    }

    @Test
    fun `typing in the search field reports the query`() {
        var query = ""
        show(VerbListUiState(verbs = verbs, isLoading = false), onQueryChange = { query = it })

        compose.onNode(hasSetTextAction()).performTextInput("ска")

        assertEquals("ска", query)
    }

    @Test
    fun `tapping a verb reports its id`() {
        var clicked: Int? = null
        show(VerbListUiState(verbs = verbs, isLoading = false), onVerbClick = { clicked = it })

        compose.onNodeWithText("сказа́ть").performClick()

        assertEquals(2, clicked)
    }

    @Test
    fun `empty result shows a message`() {
        show(VerbListUiState(query = "xyz", verbs = emptyList(), isLoading = false))

        compose.onNodeWithText(context.getString(R.string.search_no_results)).assertIsDisplayed()
    }

    @Test
    fun `no empty message while loading`() {
        show(VerbListUiState(verbs = emptyList(), isLoading = true))

        compose.onNodeWithText(context.getString(R.string.search_no_results)).assertDoesNotExist()
    }

    @Test
    fun `info button opens about`() {
        var opened = false
        show(VerbListUiState(verbs = verbs, isLoading = false), onAboutClick = { opened = true })

        compose.onNodeWithContentDescription(context.getString(R.string.about_title)).performClick()

        assertEquals(true, opened)
    }

    private fun show(
        state: VerbListUiState,
        onQueryChange: (String) -> Unit = {},
        onVerbClick: (Int) -> Unit = {},
        onAboutClick: () -> Unit = {},
    ) {
        compose.setContent {
            GlagolikTheme(dynamicColor = false) {
                VerbListScreen(
                    uiState = state,
                    onQueryChange = onQueryChange,
                    onVerbClick = onVerbClick,
                    onAboutClick = onAboutClick,
                )
            }
        }
    }
}
