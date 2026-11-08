package io.github.eleasarb.glagolik.ui.verbs

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.MasteryCounts
import io.github.eleasarb.glagolik.domain.model.MasteryOverview
import io.github.eleasarb.glagolik.domain.model.Translation
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private val mastery = MasteryOverview(
        forms = MasteryCounts(new = 30, learning = 2, young = 3, mature = 4),
        byVerb = mapOf(
            1 to MasteryCounts(new = 17),
            2 to MasteryCounts(new = 8, learning = 2, young = 3, mature = 4),
        ),
        dueNow = 5,
        meaning = MasteryCounts(new = 6, young = 1, mature = 1),
        startedVerbs = 1,
        totalVerbs = 2,
    )

    @Test
    fun `overview shows learned cards with share, started verbs and the due cards`() {
        show(VerbListUiState(verbs = verbs, isLoading = false, mastery = mastery))

        compose.onNodeWithText(context.getString(R.string.mastery_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.mastery_due, 5)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.mastery_forms)).assertIsDisplayed()
        // Learned = learning + young + mature, rounded down.
        compose.onNodeWithText("9 of 39 learned · 23%").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.mastery_meaning)).assertIsDisplayed()
        compose.onNodeWithText("2 of 8 learned · 25%").assertIsDisplayed()
        compose.onNodeWithText("Verbs started: 1 of 2").assertIsDisplayed()
        val meaningBar =
            context.resources.getQuantityString(R.plurals.mastery_summary_cards, 8, 1, 8)
        compose.onNodeWithContentDescription(meaningBar).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.mastery_mature)).assertIsDisplayed()
    }

    @Test
    fun `overview is hidden while searching`() {
        show(VerbListUiState(query = "ска", verbs = verbs, isLoading = false, mastery = mastery))

        compose.onNodeWithText(context.getString(R.string.mastery_title)).assertDoesNotExist()
    }

    @Test
    fun `only started verbs show their mastery`() {
        show(VerbListUiState(verbs = verbs, isLoading = false, mastery = mastery))
        // Below the overview card (index 0) and the first verb.
        compose.onNodeWithTag(VERB_LIST_TAG).performScrollToIndex(2)

        // 9 of 17 learned = 52.9 %, rounded down.
        compose.onNodeWithText("52%").assertIsDisplayed()
        compose.onNodeWithText("0%").assertDoesNotExist()
        val summary = context.resources.getQuantityString(R.plurals.mastery_summary, 17, 4, 17)
        compose.onNodeWithContentDescription(summary).assertIsDisplayed()
    }

    @Test
    fun `last verb can be scrolled above the training button`() {
        val many = (1..LONG_LIST_SIZE).map { id ->
            VerbSummary(id, "глагол$id", Aspect.IMPERFECTIVE, Translation("de$id", "en$id"))
        }
        show(VerbListUiState(verbs = many, isLoading = false))

        compose.onNodeWithTag(VERB_LIST_TAG).performScrollToIndex(many.lastIndex)

        val lastRow = compose.onNodeWithText("en$LONG_LIST_SIZE").getBoundsInRoot()
        val fab = compose.onNodeWithText(context.getString(R.string.training_start))
            .getBoundsInRoot()
        assertTrue(
            "last row ends at ${lastRow.bottom}, FAB starts at ${fab.top}",
            lastRow.bottom <= fab.top,
        )
    }

    @Test
    fun `info button opens about`() {
        var opened = false
        show(VerbListUiState(verbs = verbs, isLoading = false), onAboutClick = { opened = true })

        compose.onNodeWithContentDescription(context.getString(R.string.about_title)).performClick()

        assertEquals(true, opened)
    }

    @Test
    fun `training and settings buttons report clicks`() {
        var training = false
        var settings = false
        show(
            VerbListUiState(verbs = verbs, isLoading = false),
            onTrainingClick = { training = true },
            onSettingsClick = { settings = true },
        )

        compose.onNodeWithText(context.getString(R.string.training_start)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.settings_title))
            .performClick()

        assertEquals(true, training)
        assertEquals(true, settings)
    }

    private fun show(
        state: VerbListUiState,
        onQueryChange: (String) -> Unit = {},
        onVerbClick: (Int) -> Unit = {},
        onTrainingClick: () -> Unit = {},
        onSettingsClick: () -> Unit = {},
        onAboutClick: () -> Unit = {},
    ) {
        compose.setContent {
            GlagolikTheme(dynamicColor = false) {
                VerbListScreen(
                    uiState = state,
                    onQueryChange = onQueryChange,
                    onVerbClick = onVerbClick,
                    onTrainingClick = onTrainingClick,
                    onSettingsClick = onSettingsClick,
                    onAboutClick = onAboutClick,
                )
            }
        }
    }

    private companion object {
        const val LONG_LIST_SIZE = 40
    }
}
