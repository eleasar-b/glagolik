package io.github.eleasarb.glagolik.ui.verbs

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.ConjugatedForm
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.MasteryLevel
import io.github.eleasarb.glagolik.domain.model.Translation
import io.github.eleasarb.glagolik.domain.model.VerbDetail
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VerbDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val podnimat =
        VerbSummary(6, "поднима́ть", Aspect.IMPERFECTIVE, Translation("heben", "raise"))

    private val podnyat = VerbDetail(
        summary = VerbSummary(5, "подня́ть", Aspect.PERFECTIVE, Translation("heben", "lift")),
        forms = listOf(
            ConjugatedForm(FormKey.FUTURE_SG1, listOf("подниму́"), isTrainable = true),
            ConjugatedForm(FormKey.PAST_M, listOf("по́днял", "подня́л"), isTrainable = true),
            ConjugatedForm(FormKey.IMPERATIVE_SG, listOf("подними́"), isTrainable = true),
        ),
        partners = listOf(podnimat),
    )

    private val byt = VerbDetail(
        summary = VerbSummary(1, "быть", Aspect.IMPERFECTIVE, Translation("sein", "be")),
        forms = listOf(
            ConjugatedForm(FormKey.PRESENT_SG1, listOf("есть"), isTrainable = false),
            ConjugatedForm(FormKey.FUTURE_SG1, listOf("бу́ду"), isTrainable = true),
        ),
        partners = emptyList(),
    )

    @Test
    fun `trained forms show their level and the verb a summary`() {
        show(
            VerbDetailUiState.Success(
                verb = podnyat,
                mastery = mapOf(
                    FormKey.FUTURE_SG1 to MasteryLevel.MATURE,
                    FormKey.PAST_M to MasteryLevel.LEARNING,
                    FormKey.IMPERATIVE_SG to MasteryLevel.NEW,
                ),
            ),
        )

        val summary = context.resources.getQuantityString(R.plurals.mastery_summary, 3, 1, 3)
        compose.onNodeWithText(summary).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.mastery_mature))
            .assertExists()
        compose.onNodeWithContentDescription(context.getString(R.string.mastery_learning))
            .assertExists()
    }

    @Test
    fun `untrained forms have no level`() {
        val mastery = mapOf(FormKey.FUTURE_SG1 to MasteryLevel.NEW)
        show(VerbDetailUiState.Success(byt, mastery = mastery))

        compose.onAllNodesWithContentDescription(context.getString(R.string.mastery_new))
            .assertCountEquals(1)
    }

    @Test
    fun `only tenses with forms are shown`() {
        show(VerbDetailUiState.Success(podnyat))

        compose.onNodeWithText(string(R.string.tense_future)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.tense_past)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.tense_imperative)).performScrollTo()
        compose.onNodeWithText(string(R.string.tense_present)).assertDoesNotExist()
    }

    @Test
    fun `header shows infinitive, aspect and translation`() {
        show(VerbDetailUiState.Success(podnyat))

        compose.onAllNodesWithText("подня́ть").onFirst().assertIsDisplayed()
        compose.onNodeWithText(string(R.string.aspect_perfective)).assertIsDisplayed()
        compose.onNodeWithText("lift").assertIsDisplayed()
    }

    @Test
    fun `variants are shown in one row`() {
        show(VerbDetailUiState.Success(podnyat))

        compose.onNodeWithText("по́днял · подня́л").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `speaker button speaks the infinitive and a tap on a form speaks its variants`() {
        val spoken = mutableListOf<String>()
        show(VerbDetailUiState.Success(podnyat, canSpeak = true), onSpeak = { spoken += it })

        compose.onNodeWithContentDescription(context.getString(R.string.speak)).performClick()
        compose.onNodeWithText("по́днял · подня́л").performScrollTo().performClick()

        assertEquals(listOf("подня́ть", "по́днял, подня́л"), spoken)
    }

    @Test
    fun `without a voice nothing can be spoken`() {
        val spoken = mutableListOf<String>()
        show(VerbDetailUiState.Success(podnyat, canSpeak = false), onSpeak = { spoken += it })

        compose.onNodeWithContentDescription(context.getString(R.string.speak)).assertDoesNotExist()
        compose.onNodeWithText("по́днял · подня́л").performScrollTo().performClick()

        assertEquals(emptyList<String>(), spoken)
    }

    @Test
    fun `tapping a partner reports its id`() {
        var clicked: Int? = null
        show(VerbDetailUiState.Success(podnyat), onPartnerClick = { clicked = it })

        val label = context.getString(
            R.string.partner_chip,
            podnimat.infinitive,
            string(R.string.aspect_imperfective),
        )
        compose.onNodeWithText(label).performClick()

        assertEquals(6, clicked)
    }

    @Test
    fun `note appears only when a tense has untrained forms`() {
        show(VerbDetailUiState.Success(byt))

        compose.onAllNodesWithText(string(R.string.forms_not_trained)).assertCountEquals(1)
    }

    @Test
    fun `no note for fully trainable verbs`() {
        show(VerbDetailUiState.Success(podnyat))

        compose.onNodeWithText(string(R.string.forms_not_trained)).assertDoesNotExist()
    }

    @Test
    fun `unknown verb shows a message`() {
        show(VerbDetailUiState.NotFound)

        compose.onNodeWithText(string(R.string.verb_not_found)).assertIsDisplayed()
    }

    @Test
    fun `back button navigates back`() {
        var back = false
        show(VerbDetailUiState.Success(podnyat), onBack = { back = true })

        compose.onNodeWithContentDescription(string(R.string.navigate_back)).performClick()

        assertEquals(true, back)
    }

    private fun string(id: Int): String = context.getString(id)

    private fun show(
        state: VerbDetailUiState,
        onBack: () -> Unit = {},
        onPartnerClick: (Int) -> Unit = {},
        onSpeak: (String) -> Unit = {},
    ) {
        compose.setContent {
            GlagolikTheme(dynamicColor = false) {
                VerbDetailScreen(
                    uiState = state,
                    onBack = onBack,
                    onPartnerClick = onPartnerClick,
                    onSpeak = onSpeak,
                )
            }
        }
    }
}
