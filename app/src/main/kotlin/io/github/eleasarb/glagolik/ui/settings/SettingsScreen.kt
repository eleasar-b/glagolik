package io.github.eleasarb.glagolik.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme
import io.github.eleasarb.glagolik.ui.verbs.labelRes

private const val STEP = 5
private const val MINUS = "−"
private const val PLUS = "+"

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        settings = settings,
        onNewFormsPerDayChange = viewModel::onNewFormsPerDayChange,
        onSessionSizeChange = viewModel::onSessionSizeChange,
        onStrictYoChange = viewModel::onStrictYoChange,
        onTenseToggle = viewModel::onTenseToggle,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: TrainingSettings,
    onNewFormsPerDayChange: (Int) -> Unit,
    onSessionSizeChange: (Int) -> Unit,
    onStrictYoChange: (Boolean) -> Unit,
    onTenseToggle: (Tense) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
        SettingsContent(
            settings = settings,
            onNewFormsPerDayChange = onNewFormsPerDayChange,
            onSessionSizeChange = onSessionSizeChange,
            onStrictYoChange = onStrictYoChange,
            onTenseToggle = onTenseToggle,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsContent(
    settings: TrainingSettings,
    onNewFormsPerDayChange: (Int) -> Unit,
    onSessionSizeChange: (Int) -> Unit,
    onStrictYoChange: (Boolean) -> Unit,
    onTenseToggle: (Tense) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NumberSetting(
            label = stringResource(R.string.settings_new_forms),
            value = settings.newFormsPerDay,
            range = TrainingSettings.NEW_FORMS_RANGE,
            onValueChange = onNewFormsPerDayChange,
        )
        NumberSetting(
            label = stringResource(R.string.settings_session_size),
            value = settings.sessionSize,
            range = TrainingSettings.SESSION_SIZE_RANGE,
            onValueChange = onSessionSizeChange,
        )
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_strict_yo),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.settings_strict_yo_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = settings.strictYo, onCheckedChange = onStrictYoChange)
        }
        HorizontalDivider()
        Text(
            text = stringResource(R.string.settings_tenses),
            style = MaterialTheme.typography.bodyLarge,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tense.entries.forEach { tense ->
                FilterChip(
                    selected = tense in settings.tenses,
                    onClick = { onTenseToggle(tense) },
                    label = { Text(stringResource(tense.labelRes())) },
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_tenses_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NumberSetting(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        val decrease = stringResource(R.string.settings_decrease)
        val increase = stringResource(R.string.settings_increase)
        OutlinedIconButton(
            onClick = { onValueChange((value - STEP).coerceIn(range)) },
            enabled = value > range.first,
            modifier = Modifier.semantics { contentDescription = "$decrease $label" },
        ) {
            Text(text = MINUS)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 48.dp),
        )
        OutlinedIconButton(
            onClick = { onValueChange((value + STEP).coerceIn(range)) },
            enabled = value < range.last,
            modifier = Modifier.semantics { contentDescription = "$increase $label" },
        ) {
            Text(text = PLUS)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    GlagolikTheme(dynamicColor = false) {
        SettingsScreen(
            settings = TrainingSettings(tenses = setOf(Tense.FUTURE, Tense.PAST)),
            onNewFormsPerDayChange = {},
            onSessionSizeChange = {},
            onStrictYoChange = {},
            onTenseToggle = {},
            onBack = {},
        )
    }
}
