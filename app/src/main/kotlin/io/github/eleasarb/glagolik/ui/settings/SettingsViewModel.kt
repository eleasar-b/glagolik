package io.github.eleasarb.glagolik.ui.settings

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.eleasarb.glagolik.data.backup.BackupFormatException
import io.github.eleasarb.glagolik.data.backup.BackupRepository
import io.github.eleasarb.glagolik.data.backup.BackupSummary
import io.github.eleasarb.glagolik.data.settings.SettingsRepository
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Result of the last export or import, shown below the backup buttons. */
sealed interface BackupStatus {
    data object Working : BackupStatus

    data class Exported(val summary: BackupSummary) : BackupStatus

    data class Imported(val summary: BackupSummary) : BackupStatus

    /** The chosen file is not a valid Glagolik export; nothing was changed. */
    data object InvalidFile : BackupStatus

    /** The file could not be read or written. */
    data object Failed : BackupStatus
}

@Immutable
data class SettingsUiState(
    val settings: TrainingSettings = TrainingSettings(),
    val backupStatus: BackupStatus? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val backup: BackupRepository,
    private val clock: Clock,
) : ViewModel() {
    private val backupStatus = MutableStateFlow<BackupStatus?>(null)

    val uiState: StateFlow<SettingsUiState> =
        combine(repository.settings, backupStatus, ::SettingsUiState).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettingsUiState(),
        )

    /** Suggested name for the export, e.g. `glagolik-progress-2026-11-05.json`. */
    fun exportFileName(): String = "glagolik-progress-${LocalDate.now(clock)}.json"

    fun onNewFormsPerDayChange(value: Int) {
        viewModelScope.launch { repository.setNewFormsPerDay(value) }
    }

    fun onSessionSizeChange(value: Int) {
        viewModelScope.launch { repository.setSessionSize(value) }
    }

    fun onStrictYoChange(value: Boolean) {
        viewModelScope.launch { repository.setStrictYo(value) }
    }

    fun onTenseToggle(tense: Tense) {
        val current = uiState.value.settings.tenses
        val updated = if (tense in current) current - tense else current + tense
        viewModelScope.launch { repository.setTenses(updated) }
    }

    fun onExport(uri: Uri) = runBackup { BackupStatus.Exported(backup.export(uri)) }

    /** Call only after the learner confirmed that the current progress is replaced. */
    fun onImport(uri: Uri) = runBackup { BackupStatus.Imported(backup.import(uri)) }

    private fun runBackup(action: suspend () -> BackupStatus) {
        backupStatus.value = BackupStatus.Working
        viewModelScope.launch {
            backupStatus.value = try {
                action()
            } catch (_: BackupFormatException) {
                BackupStatus.InvalidFile
            } catch (_: IOException) {
                BackupStatus.Failed
            } catch (_: SecurityException) {
                // access to the document was revoked
                BackupStatus.Failed
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
