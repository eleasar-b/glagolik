package io.github.eleasarb.glagolik.ui.verbs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.MasteryCounts
import io.github.eleasarb.glagolik.domain.model.MasteryOverview
import io.github.eleasarb.glagolik.domain.model.Translation
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import io.github.eleasarb.glagolik.ui.components.MasteryBar
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme

private const val OVERVIEW_KEY = "mastery-overview"

internal const val VERB_LIST_TAG = "verb-list"

/**
 * Space below the last row, so the FAB never covers it: FAB height (56 dp), the Scaffold's FAB
 * margin (16 dp) and 16 dp of breathing room.
 */
private val FAB_CLEARANCE = 88.dp

/** Wide enough for "18/18", so the bars of all rows end at the same position. */
private val PERCENT_MIN_WIDTH = 40.dp

@Composable
fun VerbListRoute(
    onVerbClick: (Int) -> Unit,
    onTrainingClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerbListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VerbListScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onVerbClick = onVerbClick,
        onTrainingClick = onTrainingClick,
        onSettingsClick = onSettingsClick,
        onAboutClick = onAboutClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerbListScreen(
    uiState: VerbListUiState,
    onQueryChange: (String) -> Unit,
    onVerbClick: (Int) -> Unit,
    onTrainingClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onTrainingClick) {
                Text(stringResource(R.string.training_start))
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                    IconButton(onClick = onAboutClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = stringResource(R.string.about_title),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = {
                    Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            VerbList(uiState = uiState, onVerbClick = onVerbClick)
        }
    }
}

@Composable
private fun VerbList(
    uiState: VerbListUiState,
    onVerbClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.verbs.isEmpty() && !uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.search_no_results),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }
    val mastery = uiState.mastery
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(VERB_LIST_TAG),
        contentPadding = PaddingValues(bottom = FAB_CLEARANCE),
    ) {
        if (mastery != null && uiState.query.isBlank()) {
            item(key = OVERVIEW_KEY) {
                MasteryOverviewCard(
                    overview = mastery,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        items(items = uiState.verbs, key = { it.id }) { verb ->
            VerbRow(
                verb = verb,
                mastery = mastery?.byVerb?.get(verb.id),
                progress = mastery?.progressByVerb?.get(verb.id) ?: 0.0,
                onClick = { onVerbClick(verb.id) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun VerbRow(
    verb: VerbSummary,
    mastery: MasteryCounts?,
    progress: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        ListItem(
            headlineContent = {
                Text(text = verb.infinitive, style = MaterialTheme.typography.titleMedium)
            },
            supportingContent = {
                Text(
                    text = verb.translation.localized(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingContent = {
                Text(
                    text = stringResource(verb.aspect.labelRes()),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
        )
        // Below the ListItem, so the trailing aspect label does not shorten the bar.
        if (mastery != null && (mastery.isStarted || progress > 0)) {
            VerbMastery(
                counts = mastery,
                progress = progress,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }
    }
}

/**
 * Bar of the forms by level and the progress of the whole verb (forms and meaning) for a verb
 * with at least one answered card.
 */
@Composable
private fun VerbMastery(counts: MasteryCounts, progress: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MasteryBar(counts = counts, modifier = Modifier.weight(1f))
        Text(
            text = shareText(progress),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = PERCENT_MIN_WIDTH),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VerbListScreenPreview() {
    GlagolikTheme(dynamicColor = false) {
        VerbListScreen(
            uiState = VerbListUiState(
                query = "",
                verbs = listOf(
                    VerbSummary(1, "быть", Aspect.IMPERFECTIVE, Translation("sein", "be")),
                    VerbSummary(2, "сказа́ть", Aspect.PERFECTIVE, Translation("sagen", "say")),
                ),
                isLoading = false,
                mastery = MasteryOverview(
                    forms = MasteryCounts(new = 22, learning = 2, young = 4, mature = 4),
                    byVerb = mapOf(
                        1 to MasteryCounts(new = 9, learning = 1, young = 3, mature = 4),
                        2 to MasteryCounts(new = 13, learning = 1, young = 1),
                    ),
                    dueNow = 3,
                    meaning = MasteryCounts(new = 2, young = 1, mature = 1),
                    verbs = MasteryCounts(new = 1, learning = 1),
                    progress = 0.03,
                    progressByVerb = mapOf(2 to 0.44),
                ),
            ),
            onQueryChange = {},
            onVerbClick = {},
            onTrainingClick = {},
            onSettingsClick = {},
            onAboutClick = {},
        )
    }
}
