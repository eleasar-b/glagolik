package io.github.eleasarb.glagolik.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.checking.AnswerResult
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import io.github.eleasarb.glagolik.domain.model.previewMeanings
import io.github.eleasarb.glagolik.domain.usecase.ConjugationTask
import io.github.eleasarb.glagolik.domain.usecase.MeaningChoiceTask
import io.github.eleasarb.glagolik.domain.usecase.MeaningRecallTask
import io.github.eleasarb.glagolik.domain.usecase.SessionCard
import io.github.eleasarb.glagolik.ui.verbs.labelRes
import io.github.eleasarb.glagolik.ui.verbs.localized
import io.github.eleasarb.glagolik.ui.verbs.pronounRes
import io.github.eleasarb.glagolik.ui.verbs.shortLabelRes

/* Parts of a question in TrainingScreen: prompt per exercise, answer field or options, feedback. */

private const val SEPARATOR = " · "
private const val RUSSIAN = "ru"

/** What is asked, by exercise; "new card" on top for cards never answered. */
@Composable
internal fun Prompt(card: SessionCard, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (card.isNew) {
            Text(
                text = stringResource(R.string.training_new_card),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        when (val task = card.task) {
            is ConjugationTask -> ConjugationPrompt(task)
            is MeaningChoiceTask -> ChoicePrompt(task.verb)
            is MeaningRecallTask -> RecallPrompt(task.verb)
        }
    }
}

@Composable
private fun ConjugationPrompt(task: ConjugationTask, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val verb = task.verb
        val form = task.form.form
        Text(text = verb.infinitive, style = MaterialTheme.typography.headlineLarge)
        Text(
            text = stringResource(verb.aspect.labelRes()) + SEPARATOR +
                verb.translation.localized(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(form.tense.labelRes()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.training_prompt, stringResource(form.pronounRes())),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

/** RU→DE: the infinitive with its aspect; the options follow below. */
@Composable
private fun ChoicePrompt(verb: VerbSummary, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = verb.infinitive, style = MaterialTheme.typography.headlineLarge)
        Text(
            text = stringResource(verb.aspect.labelRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.training_choice_prompt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** DE→RU: translation and short aspect, e.g. "sagen, mitteilen (pf.)". */
@Composable
private fun RecallPrompt(verb: VerbSummary, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.training_recall_prompt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(
                R.string.training_recall_translation,
                verb.translation.localized(),
                stringResource(verb.aspect.shortLabelRes()),
            ),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

/**
 * Four options with the first meanings of each verb. Once answered, the right option is marked
 * as correct and a wrong choice as wrong; the options can no longer be chosen.
 */
@Composable
internal fun ChoiceOptions(
    task: MeaningChoiceTask,
    chosen: Int?,
    isGraded: Boolean,
    onOptionSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val correctLabel = stringResource(R.string.training_correct)
    val wrongLabel = stringResource(R.string.training_option_wrong)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        task.choice.options.forEachIndexed { index, option ->
            val isCorrect = index == task.choice.correctIndex
            val marked = when {
                !isGraded -> null
                isCorrect -> true
                index == chosen -> false
                else -> null
            }
            val container = when (marked) {
                true -> MaterialTheme.colorScheme.primaryContainer
                false -> MaterialTheme.colorScheme.errorContainer
                null -> Color.Transparent
            }
            OutlinedButton(
                onClick = { onOptionSelect(index) },
                enabled = !isGraded,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        if (marked != null) {
                            stateDescription = if (marked) correctLabel else wrongLabel
                        }
                    },
                colors = ButtonDefaults.outlinedButtonColors(
                    disabledContainerColor = container,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(
                    text = previewMeanings(option.translation.localized()),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
internal fun AnswerField(
    state: TrainingUiState.Question,
    label: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onContinue: (easy: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.position) {
        if (!state.isGraded) focusRequester.requestFocus()
    }
    OutlinedTextField(
        value = state.input,
        onValueChange = onInputChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        readOnly = state.isGraded,
        singleLine = true,
        label = { Text(label) },
        isError = state.feedback is AnswerResult.LatinLetters,
        supportingText = inputHint(state.feedback)?.let { hint -> { Text(hint) } },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            hintLocales = LocaleList(RUSSIAN),
        ),
        keyboardActions = KeyboardActions(
            onDone = { if (state.isGraded) onContinue(false) else onSubmit() },
        ),
    )
}

/** The Russian answer of a graded typed card: the matched form or all accepted ones. */
internal fun AnswerResult.russianAnswer(): String = when (this) {
    is AnswerResult.Correct -> matched
    is AnswerResult.Incorrect -> expected.joinToString(SEPARATOR)
    else -> ""
}

@Composable
private fun inputHint(feedback: AnswerResult?): String? = when (feedback) {
    is AnswerResult.LatinLetters -> stringResource(
        R.string.training_latin_letters,
        feedback.letters.joinToString(" "),
    )

    AnswerResult.Empty -> stringResource(R.string.training_empty_answer)

    is AnswerResult.OtherAnswer -> stringResource(R.string.training_other_verb)

    else -> null
}

/**
 * Verdict and [answer]. [onSpeak] is `null` when no voice is available; then no speaker button
 * is shown.
 */
@Composable
internal fun FeedbackCard(
    feedback: AnswerResult,
    answer: String,
    onSpeak: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val correct = feedback is AnswerResult.Correct
    val colors = if (correct) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    }
    val (headline, yoHint) = when (feedback) {
        is AnswerResult.Correct -> stringResource(R.string.training_correct) to
            feedback.yoTypedAsYe

        is AnswerResult.Incorrect -> stringResource(R.string.training_incorrect) to
            feedback.yoTypedAsYe

        else -> return
    }
    Card(modifier = modifier.fillMaxWidth(), colors = colors) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = headline, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = answer,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                if (onSpeak != null) {
                    IconButton(onClick = onSpeak) {
                        Icon(
                            painter = painterResource(R.drawable.ic_volume_up),
                            contentDescription = stringResource(R.string.speak),
                        )
                    }
                }
            }
            if (yoHint) {
                Text(
                    text = stringResource(R.string.training_yo_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
