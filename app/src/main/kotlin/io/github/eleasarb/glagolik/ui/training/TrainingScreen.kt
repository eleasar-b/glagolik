package io.github.eleasarb.glagolik.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.checking.AnswerResult
import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.ConjugatedForm
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.FormRef
import io.github.eleasarb.glagolik.domain.model.TrainableForm
import io.github.eleasarb.glagolik.domain.model.Translation
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import io.github.eleasarb.glagolik.domain.usecase.SessionCard
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme
import io.github.eleasarb.glagolik.ui.verbs.labelRes
import io.github.eleasarb.glagolik.ui.verbs.localized
import io.github.eleasarb.glagolik.ui.verbs.pronounRes

private const val VARIANT_SEPARATOR = " · "
private const val RUSSIAN = "ru"

@Composable
fun TrainingRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrainingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TrainingScreen(
        uiState = uiState,
        onInputChange = viewModel::onInputChange,
        onSubmit = viewModel::onSubmit,
        onContinue = viewModel::onContinue,
        onClose = onClose,
        modifier = modifier,
        onSpeak = viewModel::onSpeak,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    uiState: TrainingUiState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onContinue: (easy: Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onSpeak: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val title = if (uiState is TrainingUiState.Question) {
                        stringResource(R.string.training_progress, uiState.position, uiState.total)
                    } else {
                        stringResource(R.string.training_title)
                    }
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.training_close),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (uiState) {
            TrainingUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            TrainingUiState.NothingToDo -> Message(
                title = stringResource(R.string.training_nothing_title),
                text = stringResource(R.string.training_nothing_text),
                onClose = onClose,
                modifier = contentModifier,
            )

            is TrainingUiState.Finished -> Message(
                title = stringResource(R.string.training_finished_title),
                text = pluralStringResource(
                    R.plurals.training_finished_text,
                    uiState.cards,
                    uiState.correctFirstTry,
                    uiState.cards,
                ),
                onClose = onClose,
                modifier = contentModifier,
            )

            is TrainingUiState.Question -> QuestionContent(
                state = uiState,
                onInputChange = onInputChange,
                onSubmit = onSubmit,
                onContinue = onContinue,
                onSpeak = onSpeak,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun QuestionContent(
    state: TrainingUiState.Question,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onContinue: (easy: Boolean) -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.position) {
        if (!state.isGraded) focusRequester.requestFocus()
    }
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { (state.position - 1).toFloat() / state.total },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Prompt(card = state.card)
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                readOnly = state.isGraded,
                singleLine = true,
                label = { Text(stringResource(R.string.training_answer_label)) },
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
            val feedback = state.feedback
            if (feedback is AnswerResult.Correct || feedback is AnswerResult.Incorrect) {
                FeedbackCard(
                    feedback = feedback,
                    onSpeak = onSpeak.takeIf { state.canSpeak },
                )
            }
            if (feedback is AnswerResult.Incorrect && state.tenseForms.isNotEmpty()) {
                TenseFormsCard(
                    tense = state.card.form.ref.form.tense,
                    forms = state.tenseForms,
                    asked = state.card.form.ref.form,
                )
            }
            Actions(state = state, onSubmit = onSubmit, onContinue = onContinue)
        }
    }
}

@Composable
private fun Prompt(card: SessionCard, modifier: Modifier = Modifier) {
    val verb = card.form.verb
    val form = card.form.ref.form
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (card.isNew) {
            Text(
                text = stringResource(R.string.training_new_form),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Text(text = verb.infinitive, style = MaterialTheme.typography.headlineLarge)
        Text(
            text = stringResource(verb.aspect.labelRes()) + VARIANT_SEPARATOR +
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

@Composable
private fun inputHint(feedback: AnswerResult?): String? = when (feedback) {
    is AnswerResult.LatinLetters -> stringResource(
        R.string.training_latin_letters,
        feedback.letters.joinToString(" "),
    )

    AnswerResult.Empty -> stringResource(R.string.training_empty_answer)

    else -> null
}

/** [onSpeak] is `null` when no voice is available; then no speaker button is shown. */
@Composable
private fun FeedbackCard(
    feedback: AnswerResult,
    onSpeak: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val correct = feedback is AnswerResult.Correct
    val colors = if (correct) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    }
    val (headline, answer, yoHint) = when (feedback) {
        is AnswerResult.Correct -> Triple(
            stringResource(R.string.training_correct),
            feedback.matched,
            feedback.yoTypedAsYe,
        )

        is AnswerResult.Incorrect -> Triple(
            stringResource(R.string.training_incorrect),
            feedback.expected.joinToString(VARIANT_SEPARATOR),
            feedback.yoTypedAsYe,
        )

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

@Composable
private fun Actions(
    state: TrainingUiState.Question,
    onSubmit: () -> Unit,
    onContinue: (easy: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        when {
            !state.isGraded -> Button(onClick = onSubmit) {
                Text(stringResource(R.string.training_check))
            }

            state.feedback is AnswerResult.Correct -> {
                OutlinedButton(onClick = { onContinue(true) }) {
                    Text(stringResource(R.string.training_easy))
                }
                Button(onClick = { onContinue(false) }) {
                    Text(stringResource(R.string.training_continue))
                }
            }

            else -> Button(onClick = { onContinue(false) }) {
                Text(stringResource(R.string.training_continue))
            }
        }
    }
}

@Composable
private fun Message(
    title: String,
    text: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(text = text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Button(onClick = onClose) {
            Text(stringResource(R.string.training_done))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainingScreenPreview() {
    val card = SessionCard(
        form = TrainableForm(
            ref = FormRef(2, FormKey.FUTURE_SG2),
            verb = VerbSummary(2, "сказа́ть", Aspect.PERFECTIVE, Translation("sagen", "say")),
            expected = listOf("ска́жешь"),
        ),
        isNew = true,
    )
    GlagolikTheme(dynamicColor = false) {
        TrainingScreen(
            uiState = TrainingUiState.Question(
                card = card,
                position = 3,
                total = 20,
                input = "скажешь",
                feedback = AnswerResult.Correct("ска́жешь"),
            ),
            onInputChange = {},
            onSubmit = {},
            onContinue = {},
            onClose = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainingScreenIncorrectPreview() {
    val card = SessionCard(
        form = TrainableForm(
            ref = FormRef(2, FormKey.FUTURE_SG2),
            verb = VerbSummary(2, "сказа́ть", Aspect.PERFECTIVE, Translation("sagen", "say")),
            expected = listOf("ска́жешь"),
        ),
        isNew = false,
    )
    val forms = listOf(
        FormKey.FUTURE_SG1 to "скажу́",
        FormKey.FUTURE_SG2 to "ска́жешь",
        FormKey.FUTURE_SG3 to "ска́жет",
        FormKey.FUTURE_PL1 to "ска́жем",
        FormKey.FUTURE_PL2 to "ска́жете",
        FormKey.FUTURE_PL3 to "ска́жут",
    ).map { (key, form) -> ConjugatedForm(key, listOf(form), isTrainable = true) }
    GlagolikTheme(dynamicColor = false) {
        TrainingScreen(
            uiState = TrainingUiState.Question(
                card = card,
                position = 3,
                total = 20,
                input = "скажишь",
                feedback = AnswerResult.Incorrect(listOf("ска́жешь")),
                tenseForms = forms,
                canSpeak = true,
            ),
            onInputChange = {},
            onSubmit = {},
            onContinue = {},
            onClose = {},
        )
    }
}
