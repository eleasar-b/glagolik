package io.github.eleasarb.glagolik.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import io.github.eleasarb.glagolik.domain.model.TrainableForm
import io.github.eleasarb.glagolik.domain.model.Translation
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import io.github.eleasarb.glagolik.domain.usecase.ConjugationTask
import io.github.eleasarb.glagolik.domain.usecase.MeaningChoice
import io.github.eleasarb.glagolik.domain.usecase.MeaningChoiceTask
import io.github.eleasarb.glagolik.domain.usecase.MeaningRecallTask
import io.github.eleasarb.glagolik.domain.usecase.SessionCard
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme
import io.github.eleasarb.glagolik.ui.verbs.localized

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
        onOptionSelect = viewModel::onOptionSelect,
        onSpeak = viewModel::onSpeak,
    )
}

@Composable
fun TrainingScreen(
    uiState: TrainingUiState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onContinue: (easy: Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onOptionSelect: (Int) -> Unit = {},
    onSpeak: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(uiState = uiState, onClose = onClose) },
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
                onOptionSelect = onOptionSelect,
                onContinue = onContinue,
                onSpeak = onSpeak,
                modifier = contentModifier,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingTopBar(
    uiState: TrainingUiState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
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
}

@Composable
private fun QuestionContent(
    state: TrainingUiState.Question,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOptionSelect: (Int) -> Unit,
    onContinue: (easy: Boolean) -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val task = state.card.task
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
            when (task) {
                is MeaningChoiceTask -> ChoiceOptions(
                    task = task,
                    chosen = state.chosenOption,
                    isGraded = state.isGraded,
                    onOptionSelect = onOptionSelect,
                )

                is ConjugationTask, is MeaningRecallTask -> AnswerField(
                    state = state,
                    label = if (task is MeaningRecallTask) {
                        stringResource(R.string.training_infinitive_label)
                    } else {
                        stringResource(R.string.training_answer_label)
                    },
                    onInputChange = onInputChange,
                    onSubmit = onSubmit,
                    onContinue = onContinue,
                )
            }
            val feedback = state.feedback
            if (feedback is AnswerResult.Correct || feedback is AnswerResult.Incorrect) {
                FeedbackCard(
                    feedback = feedback,
                    // RU→DE: the infinitive is on screen already, the translation is the answer.
                    answer = if (task is MeaningChoiceTask) {
                        task.verb.translation.localized()
                    } else {
                        feedback.russianAnswer()
                    },
                    onSpeak = onSpeak.takeIf { state.canSpeak },
                )
            }
            if (feedback is AnswerResult.Incorrect && task is ConjugationTask &&
                state.tenseForms.isNotEmpty()
            ) {
                TenseFormsCard(
                    tense = task.form.form.tense,
                    forms = state.tenseForms,
                    asked = task.form.form,
                )
            }
            Actions(state = state, onSubmit = onSubmit, onContinue = onContinue)
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
            // Multiple choice is graded by choosing an option.
            !state.isGraded && state.card.task is MeaningChoiceTask -> Unit

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
        task = ConjugationTask(
            TrainableForm(
                verb = VerbSummary(2, "сказа́ть", Aspect.PERFECTIVE, Translation("sagen", "say")),
                form = FormKey.FUTURE_SG2,
                expected = listOf("ска́жешь"),
            ),
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
        task = ConjugationTask(
            TrainableForm(
                verb = VerbSummary(2, "сказа́ть", Aspect.PERFECTIVE, Translation("sagen", "say")),
                form = FormKey.FUTURE_SG2,
                expected = listOf("ска́жешь"),
            ),
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

@Preview(showBackground = true)
@Composable
private fun TrainingScreenChoicePreview() {
    val options = listOf(
        VerbSummary(5, "есть", Aspect.IMPERFECTIVE, Translation("essen, fressen", "eat")),
        VerbSummary(2, "сказа́ть", Aspect.PERFECTIVE, Translation("sagen, mitteilen", "say")),
        VerbSummary(13, "жить", Aspect.IMPERFECTIVE, Translation("leben, wohnen", "live")),
        VerbSummary(36, "ждать", Aspect.IMPERFECTIVE, Translation("warten, erwarten", "wait")),
    )
    GlagolikTheme(dynamicColor = false) {
        TrainingScreen(
            uiState = TrainingUiState.Question(
                card = SessionCard(
                    task = MeaningChoiceTask(options[1], MeaningChoice(options, correctIndex = 1)),
                    isNew = true,
                ),
                position = 1,
                total = 20,
                feedback = AnswerResult.Incorrect(listOf("сказа́ть")),
                chosenOption = 3,
                canSpeak = true,
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
private fun TrainingScreenRecallPreview() {
    val verb = VerbSummary(2, "сказа́ть", Aspect.PERFECTIVE, Translation("sagen, mitteilen", "say"))
    GlagolikTheme(dynamicColor = false) {
        TrainingScreen(
            uiState = TrainingUiState.Question(
                card = SessionCard(task = MeaningRecallTask(verb), isNew = false),
                position = 4,
                total = 20,
                input = "сообщить",
                feedback = AnswerResult.OtherAnswer("сообщи́ть"),
            ),
            onInputChange = {},
            onSubmit = {},
            onContinue = {},
            onClose = {},
        )
    }
}
