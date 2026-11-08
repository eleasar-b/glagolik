package io.github.eleasarb.glagolik.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.data.backup.BackupSummary
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme
import io.github.eleasarb.glagolik.ui.verbs.labelRes

private const val STEP = 5
private const val BACKUP_MIME_TYPE = "application/json"

/** Some file managers report JSON as text or binary, so accept those too. */
private val IMPORT_MIME_TYPES = arrayOf("application/json", "text/*", "application/octet-stream")
private const val MINUS = "−"
private const val PLUS = "+"

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    // Coming back from the system settings: a voice may have been installed meanwhile.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingImport by rememberSaveable { mutableStateOf<Uri?>(null) }
    // Storage Access Framework: the learner picks the location, no permission needed.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
    ) { uri -> uri?.let(viewModel::onExport) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingImport = uri }

    SettingsScreen(
        uiState = uiState,
        onNewCardsPerDayChange = viewModel::onNewCardsPerDayChange,
        onSessionSizeChange = viewModel::onSessionSizeChange,
        onStrictYoChange = viewModel::onStrictYoChange,
        onAutoPlayAudioChange = viewModel::onAutoPlayAudioChange,
        onSpeechTest = viewModel::onSpeechTest,
        onInstallVoice = { context.openVoiceInstall() },
        onOpenSpeechSettings = { context.openSpeechSettings() },
        onTenseToggle = viewModel::onTenseToggle,
        onExportClick = { exportLauncher.launch(viewModel.exportFileName()) },
        onImportClick = { importLauncher.launch(IMPORT_MIME_TYPES) },
        onBack = onBack,
        modifier = modifier,
    )

    pendingImport?.let { uri ->
        ConfirmImportDialog(
            onConfirm = {
                pendingImport = null
                viewModel.onImport(uri)
            },
            onDismiss = { pendingImport = null },
        )
    }
}

@Composable
private fun ConfirmImportDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.backup_import_confirm_title)) },
        text = { Text(stringResource(R.string.backup_import_confirm_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.backup_import_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.backup_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNewCardsPerDayChange: (Int) -> Unit,
    onSessionSizeChange: (Int) -> Unit,
    onStrictYoChange: (Boolean) -> Unit,
    onAutoPlayAudioChange: (Boolean) -> Unit,
    onSpeechTest: () -> Unit,
    onInstallVoice: () -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onTenseToggle: (Tense) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TrainingSettingsSection(
                settings = uiState.settings,
                onNewCardsPerDayChange = onNewCardsPerDayChange,
                onSessionSizeChange = onSessionSizeChange,
                onStrictYoChange = onStrictYoChange,
                onTenseToggle = onTenseToggle,
            )
            HorizontalDivider()
            AudioSettingsSection(
                autoPlay = uiState.settings.autoPlayAudio,
                speech = uiState.speech,
                onAutoPlayChange = onAutoPlayAudioChange,
                onTest = onSpeechTest,
                onInstallVoice = onInstallVoice,
                onOpenSystemSettings = onOpenSpeechSettings,
            )
            HorizontalDivider()
            BackupSection(
                status = uiState.backupStatus,
                onExportClick = onExportClick,
                onImportClick = onImportClick,
            )
        }
    }
}

@Composable
internal fun SwitchSetting(
    label: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The whole row toggles, so label and switch form one target for touch and TalkBack.
    Row(
        modifier = modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrainingSettingsSection(
    settings: TrainingSettings,
    onNewCardsPerDayChange: (Int) -> Unit,
    onSessionSizeChange: (Int) -> Unit,
    onStrictYoChange: (Boolean) -> Unit,
    onTenseToggle: (Tense) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NumberSetting(
            label = stringResource(R.string.settings_new_forms),
            value = settings.newCardsPerDay,
            range = TrainingSettings.NEW_CARDS_RANGE,
            onValueChange = onNewCardsPerDayChange,
        )
        NumberSetting(
            label = stringResource(R.string.settings_session_size),
            value = settings.sessionSize,
            range = TrainingSettings.SESSION_SIZE_RANGE,
            onValueChange = onSessionSizeChange,
        )
        HorizontalDivider()
        SwitchSetting(
            label = stringResource(R.string.settings_strict_yo),
            summary = stringResource(R.string.settings_strict_yo_summary),
            checked = settings.strictYo,
            onCheckedChange = onStrictYoChange,
        )
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
private fun BackupSection(
    status: BackupStatus?,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val working = status == BackupStatus.Working
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.backup_title),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.backup_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExportClick, enabled = !working) {
                Text(stringResource(R.string.backup_export))
            }
            OutlinedButton(onClick = onImportClick, enabled = !working) {
                Text(stringResource(R.string.backup_import))
            }
        }
        backupStatusText(status)?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (status == BackupStatus.InvalidFile || status == BackupStatus.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun backupStatusText(status: BackupStatus?): String? = when (status) {
    null -> null

    BackupStatus.Working -> stringResource(R.string.backup_working)

    is BackupStatus.Exported -> pluralStringResource(
        R.plurals.backup_exported,
        status.summary.cards,
        status.summary.cards,
    )

    is BackupStatus.Imported -> pluralStringResource(
        R.plurals.backup_imported,
        status.summary.cards,
        status.summary.cards,
    )

    BackupStatus.InvalidFile -> stringResource(R.string.backup_invalid_file)

    BackupStatus.Failed -> stringResource(R.string.backup_failed)
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
            uiState = SettingsUiState(
                settings = TrainingSettings(tenses = setOf(Tense.FUTURE, Tense.PAST)),
                backupStatus = BackupStatus.Exported(BackupSummary(cards = 42, reviews = 120)),
            ),
            onNewCardsPerDayChange = {},
            onSessionSizeChange = {},
            onStrictYoChange = {},
            onAutoPlayAudioChange = {},
            onSpeechTest = {},
            onInstallVoice = {},
            onOpenSpeechSettings = {},
            onTenseToggle = {},
            onExportClick = {},
            onImportClick = {},
            onBack = {},
        )
    }
}
