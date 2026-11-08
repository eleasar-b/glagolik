package io.github.eleasarb.glagolik.ui.training

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.checking.AnswerResult
import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.ConjugatedForm
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.TrainableForm
import io.github.eleasarb.glagolik.domain.model.Translation
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import io.github.eleasarb.glagolik.domain.usecase.ConjugationTask
import io.github.eleasarb.glagolik.domain.usecase.SessionCard
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrainingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val card = SessionCard(
        task = ConjugationTask(
            TrainableForm(
                verb = VerbSummary(2, "сказа́ть", Aspect.PERFECTIVE, Translation("sagen", "say")),
                form = FormKey.FUTURE_SG2,
                expected = listOf("ска́жешь"),
            ),
        ),
        isNew = true,
    )

    private fun question(input: String = "", feedback: AnswerResult? = null) =
        TrainingUiState.Question(card, position = 3, total = 20, input = input, feedback = feedback)

    @Test
    fun `question shows verb, tense, pronoun and progress`() {
        show(question())

        compose.onNodeWithText("сказа́ть").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.tense_future)).assertIsDisplayed()
        compose.onNodeWithText(
            context.getString(R.string.training_prompt, "ты"),
        ).assertIsDisplayed()
        compose.onNodeWithText("3 / 20").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.training_new_form)).assertIsDisplayed()
    }

    @Test
    fun `typing and checking report to the caller`() {
        var typed = ""
        var submitted = false
        show(question(), onInputChange = { typed = it }, onSubmit = { submitted = true })

        compose.onNode(hasSetTextAction()).performTextInput("скажешь")
        compose.onNodeWithText(string(R.string.training_check)).performScrollTo().performClick()

        assertEquals("скажешь", typed)
        assertEquals(true, submitted)
    }

    @Test
    fun `correct answer offers easy and continue`() {
        var continued: Boolean? = null
        show(
            question("скажешь", AnswerResult.Correct("ска́жешь")),
            onContinue = { continued = it },
        )

        compose.onNodeWithText(
            string(R.string.training_correct),
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(string(R.string.training_easy)).performScrollTo().performClick()

        assertEquals(true, continued)
    }

    @Test
    fun `wrong answer shows all expected variants and only continue`() {
        var continued: Boolean? = null
        val wrong = AnswerResult.Incorrect(listOf("по́днял", "подня́л"))
        show(question("поднимал", wrong), onContinue = { continued = it })

        compose.onNodeWithText("по́днял · подня́л").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(string(R.string.training_easy)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.training_continue)).performScrollTo().performClick()

        assertEquals(false, continued)
    }

    @Test
    fun `wrong answer shows the whole tense`() {
        val forms = listOf(
            FormKey.FUTURE_SG1 to "скажу́",
            FormKey.FUTURE_SG2 to "ска́жешь",
            FormKey.FUTURE_PL3 to "ска́жут",
        ).map { (key, form) -> ConjugatedForm(key, listOf(form), isTrainable = true) }
        val wrong = AnswerResult.Incorrect(listOf("ска́жешь"))
        show(question("скажишь", wrong).copy(tenseForms = forms))

        val title = context.getString(R.string.training_tense_forms, string(R.string.tense_future))
        compose.onNodeWithText(title).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("скажу́").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ска́жут").performScrollTo().assertIsDisplayed()
        // Once as the correct answer, once in the table.
        assertEquals(2, compose.onAllNodesWithText("ска́жешь").fetchSemanticsNodes().size)
    }

    @Test
    fun `correct answer shows no tense table`() {
        show(question("скажешь", AnswerResult.Correct("ска́жешь")))

        val title = context.getString(R.string.training_tense_forms, string(R.string.tense_future))
        compose.onNodeWithText(title).assertDoesNotExist()
    }

    @Test
    fun `speaker button appears after grading when a voice is available`() {
        var spoken = 0
        val graded = question("скажешь", AnswerResult.Correct("ска́жешь"))
        show(graded.copy(canSpeak = true), onSpeak = { spoken++ })

        compose.onNodeWithContentDescription(string(R.string.speak)).performScrollTo()
            .performClick()

        assertEquals(1, spoken)
    }

    @Test
    fun `no speaker button without a voice`() {
        show(question("скажешь", AnswerResult.Correct("ска́жешь")).copy(canSpeak = false))

        compose.onNodeWithContentDescription(string(R.string.speak)).assertDoesNotExist()
    }

    @Test
    fun `no speaker button before grading`() {
        show(question().copy(canSpeak = true))

        compose.onNodeWithContentDescription(string(R.string.speak)).assertDoesNotExist()
    }

    @Test
    fun `yo hint is shown`() {
        show(question("идет", AnswerResult.Correct("идёт", yoTypedAsYe = true)))

        compose.onNodeWithText(
            string(R.string.training_yo_hint),
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `latin letters are explained`() {
        show(question("ckaжу", AnswerResult.LatinLetters(listOf('c', 'k'))))

        compose.onNodeWithText(context.getString(R.string.training_latin_letters, "c k"))
            .assertIsDisplayed()
        compose.onNodeWithText(
            string(R.string.training_check),
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `finished session shows the summary`() {
        var closed = false
        show(TrainingUiState.Finished(cards = 20, correctFirstTry = 17), onClose = {
            closed = true
        })

        compose.onNodeWithText(
            context.resources.getQuantityString(R.plurals.training_finished_text, 20, 17, 20),
        )
            .assertIsDisplayed()
        compose.onNodeWithText(string(R.string.training_done)).performClick()

        assertEquals(true, closed)
    }

    @Test
    fun `nothing to do is explained`() {
        show(TrainingUiState.NothingToDo)

        compose.onNodeWithText(string(R.string.training_nothing_title)).assertIsDisplayed()
    }

    @Test
    fun `close button closes the session`() {
        var closed = false
        show(question(), onClose = { closed = true })

        compose.onNodeWithContentDescription(string(R.string.training_close)).performClick()

        assertEquals(true, closed)
    }

    private fun string(id: Int): String = context.getString(id)

    private fun show(
        state: TrainingUiState,
        onInputChange: (String) -> Unit = {},
        onSubmit: () -> Unit = {},
        onContinue: (Boolean) -> Unit = {},
        onClose: () -> Unit = {},
        onSpeak: () -> Unit = {},
    ) {
        compose.setContent {
            GlagolikTheme(dynamicColor = false) {
                TrainingScreen(
                    uiState = state,
                    onInputChange = onInputChange,
                    onSubmit = onSubmit,
                    onContinue = onContinue,
                    onClose = onClose,
                    onSpeak = onSpeak,
                )
            }
        }
    }
}
