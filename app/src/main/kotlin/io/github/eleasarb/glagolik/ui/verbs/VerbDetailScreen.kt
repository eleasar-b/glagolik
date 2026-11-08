package io.github.eleasarb.glagolik.ui.verbs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.ConjugatedForm
import io.github.eleasarb.glagolik.domain.model.Exercise
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.MasteryCounts
import io.github.eleasarb.glagolik.domain.model.MasteryLevel
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.Translation
import io.github.eleasarb.glagolik.domain.model.VerbDetail
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import io.github.eleasarb.glagolik.ui.components.MasteryBar
import io.github.eleasarb.glagolik.ui.components.MasteryDot
import io.github.eleasarb.glagolik.ui.components.labelRes
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme

private const val VARIANT_SEPARATOR = " · "
private const val SPOKEN_VARIANT_SEPARATOR = ", "

@Composable
fun VerbDetailRoute(
    onBack: () -> Unit,
    onPartnerClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerbDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VerbDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onPartnerClick = onPartnerClick,
        modifier = modifier,
        onSpeak = viewModel::onSpeak,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerbDetailScreen(
    uiState: VerbDetailUiState,
    onBack: () -> Unit,
    onPartnerClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSpeak: (String) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (uiState is VerbDetailUiState.Success) Text(uiState.verb.summary.infinitive)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
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
            VerbDetailUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            VerbDetailUiState.NotFound -> Box(
                contentModifier,
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.verb_not_found))
            }

            is VerbDetailUiState.Success -> VerbDetailContent(
                verb = uiState.verb,
                mastery = uiState.mastery,
                meaning = uiState.meaning,
                masteryCounts = uiState.masteryCounts,
                onPartnerClick = onPartnerClick,
                onSpeak = onSpeak.takeIf { uiState.canSpeak },
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun VerbDetailContent(
    verb: VerbDetail,
    mastery: Map<FormKey, MasteryLevel>,
    meaning: Map<Exercise, MasteryLevel>,
    masteryCounts: MasteryCounts,
    onPartnerClick: (Int) -> Unit,
    onSpeak: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        VerbHeader(summary = verb.summary, meaning = meaning, onSpeak = onSpeak)
        if (masteryCounts.total > 0) VerbMasterySummary(counts = masteryCounts)
        if (verb.partners.isNotEmpty()) {
            PartnerChips(partners = verb.partners, onPartnerClick = onPartnerClick)
        }
        Tense.entries.forEach { tense ->
            val forms = verb.formsOf(tense)
            if (forms.isNotEmpty()) {
                TenseCard(tense = tense, forms = forms, mastery = mastery, onSpeak = onSpeak)
            }
        }
    }
}

@Composable
private fun VerbHeader(
    summary: VerbSummary,
    meaning: Map<Exercise, MasteryLevel>,
    onSpeak: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = summary.infinitive, style = MaterialTheme.typography.headlineLarge)
            if (onSpeak != null) {
                IconButton(onClick = { onSpeak(summary.infinitive) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_volume_up),
                        contentDescription = stringResource(R.string.speak),
                    )
                }
            }
        }
        Text(
            text = stringResource(summary.aspect.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summary.translation.localized(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f, fill = false),
            )
            MeaningDots(meaning = meaning, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** One dot per meaning direction (RU→DE, then DE→RU), each announced with its level. */
@Composable
private fun MeaningDots(meaning: Map<Exercise, MasteryLevel>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Exercise.MEANINGS.forEach { exercise ->
            val level = meaning[exercise] ?: return@forEach
            val description = stringResource(
                if (exercise == Exercise.MeaningRuDe) {
                    R.string.verb_meaning_ru_de
                } else {
                    R.string.verb_meaning_de_ru
                },
                stringResource(level.labelRes()),
            )
            MasteryDot(
                level = level,
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartnerChips(
    partners: List<VerbSummary>,
    onPartnerClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.aspect_partners),
            style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            partners.forEach { partner ->
                AssistChip(
                    onClick = { onPartnerClick(partner.id) },
                    label = {
                        Text(
                            stringResource(
                                R.string.partner_chip,
                                partner.infinitive,
                                stringResource(partner.aspect.labelRes()),
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun VerbMasterySummary(counts: MasteryCounts, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MasteryBar(counts = counts, modifier = Modifier.fillMaxWidth(), height = 8.dp)
        Text(
            text = pluralStringResource(
                R.plurals.mastery_summary,
                counts.total,
                counts.mature,
                counts.total,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TenseCard(
    tense: Tense,
    forms: List<ConjugatedForm>,
    mastery: Map<FormKey, MasteryLevel>,
    onSpeak: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(tense.labelRes()),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            forms.forEach { form ->
                FormRow(form = form, level = mastery[form.key], onSpeak = onSpeak)
            }
            if (forms.any { !it.isTrainable }) {
                Text(
                    text = stringResource(R.string.forms_not_trained),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * [level] is `null` for forms that are not trained. With [onSpeak], a tap speaks all variants
 * (a comma gives a short pause between them).
 */
@Composable
private fun FormRow(
    form: ConjugatedForm,
    level: MasteryLevel?,
    onSpeak: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val color = if (form.isTrainable) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val speakLabel = stringResource(R.string.speak)
    val clickable = if (onSpeak != null) {
        Modifier.clickable(onClickLabel = speakLabel) {
            onSpeak(form.variants.joinToString(SPOKEN_VARIANT_SEPARATOR))
        }
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .then(clickable)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(form.key.pronounRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = form.variants.joinToString(VARIANT_SEPARATOR),
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier.weight(1f),
        )
        if (level != null) {
            val description = stringResource(level.labelRes())
            MasteryDot(
                level = level,
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VerbDetailScreenPreview() {
    val summary = VerbSummary(
        id = 78,
        infinitive = "подня́ть",
        aspect = Aspect.PERFECTIVE,
        translation = Translation(german = "heben", english = "lift"),
    )
    val detail = VerbDetail(
        summary = summary,
        forms = listOf(
            ConjugatedForm(FormKey.FUTURE_SG1, listOf("подниму́"), isTrainable = true),
            ConjugatedForm(FormKey.FUTURE_SG2, listOf("подни́мешь"), isTrainable = true),
            ConjugatedForm(FormKey.PAST_M, listOf("по́днял", "подня́л"), true),
            ConjugatedForm(FormKey.IMPERATIVE_SG, listOf("подними́"), isTrainable = true),
        ),
        partners = listOf(
            VerbSummary(300, "поднима́ть", Aspect.IMPERFECTIVE, Translation("heben", "raise")),
        ),
    )
    GlagolikTheme(dynamicColor = false) {
        VerbDetailScreen(
            uiState = VerbDetailUiState.Success(
                verb = detail,
                mastery = mapOf(
                    FormKey.FUTURE_SG1 to MasteryLevel.MATURE,
                    FormKey.FUTURE_SG2 to MasteryLevel.YOUNG,
                    FormKey.PAST_M to MasteryLevel.LEARNING,
                    FormKey.IMPERATIVE_SG to MasteryLevel.NEW,
                ),
                meaning = mapOf(
                    Exercise.MeaningRuDe to MasteryLevel.YOUNG,
                    Exercise.MeaningDeRu to MasteryLevel.LEARNING,
                ),
                canSpeak = true,
            ),
            onBack = {},
            onPartnerClick = {},
        )
    }
}
