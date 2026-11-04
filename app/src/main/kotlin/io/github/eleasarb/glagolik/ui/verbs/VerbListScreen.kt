package io.github.eleasarb.glagolik.ui.verbs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.Translation
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme

@Composable
fun VerbListRoute(
    onVerbClick: (Int) -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerbListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VerbListScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onVerbClick = onVerbClick,
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
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
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
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = uiState.verbs, key = { it.id }) { verb ->
            VerbRow(verb = verb, onClick = { onVerbClick(verb.id) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun VerbRow(verb: VerbSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
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
            ),
            onQueryChange = {},
            onVerbClick = {},
            onAboutClick = {},
        )
    }
}
