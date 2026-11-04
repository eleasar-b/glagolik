package io.github.eleasarb.glagolik.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.eleasarb.glagolik.data.settings.SettingsRepository
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) :
    ViewModel() {
    val uiState: StateFlow<TrainingSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TrainingSettings(),
    )

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
        val current = uiState.value.tenses
        val updated = if (tense in current) current - tense else current + tense
        viewModelScope.launch { repository.setTenses(updated) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
