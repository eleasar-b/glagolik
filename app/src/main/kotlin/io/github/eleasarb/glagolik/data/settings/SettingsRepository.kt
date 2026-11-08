package io.github.eleasarb.glagolik.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val settings: Flow<TrainingSettings>

    suspend fun current(): TrainingSettings = settings.first()

    suspend fun setNewCardsPerDay(value: Int)

    suspend fun setSessionSize(value: Int)

    suspend fun setStrictYo(value: Boolean)

    suspend fun setAutoPlayAudio(value: Boolean)

    suspend fun setPracticeMeaning(value: Boolean)

    /** Ignored if [tenses] is empty: at least one tense must stay selected. */
    suspend fun setTenses(tenses: Set<Tense>)

    /** Replaces all settings at once (import). Values are clamped like the setters. */
    suspend fun replaceAll(settings: TrainingSettings)
}

/** Stored in `files/datastore/settings.preferences_pb`, included in backups. */
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<TrainingSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun setNewCardsPerDay(value: Int) {
        dataStore.edit { it[NEW_CARDS_PER_DAY] = value.coerceIn(TrainingSettings.NEW_CARDS_RANGE) }
    }

    override suspend fun setSessionSize(value: Int) {
        dataStore.edit { it[SESSION_SIZE] = value.coerceIn(TrainingSettings.SESSION_SIZE_RANGE) }
    }

    override suspend fun setStrictYo(value: Boolean) {
        dataStore.edit { it[STRICT_YO] = value }
    }

    override suspend fun setAutoPlayAudio(value: Boolean) {
        dataStore.edit { it[AUTO_PLAY_AUDIO] = value }
    }

    override suspend fun setPracticeMeaning(value: Boolean) {
        dataStore.edit { it[PRACTICE_MEANING] = value }
    }

    override suspend fun setTenses(tenses: Set<Tense>) {
        if (tenses.isEmpty()) return
        dataStore.edit { prefs -> prefs[TENSES] = tenses.mapTo(mutableSetOf()) { it.name } }
    }

    override suspend fun replaceAll(settings: TrainingSettings) {
        dataStore.edit { prefs ->
            prefs[NEW_CARDS_PER_DAY] =
                settings.newCardsPerDay.coerceIn(TrainingSettings.NEW_CARDS_RANGE)
            prefs[SESSION_SIZE] = settings.sessionSize.coerceIn(TrainingSettings.SESSION_SIZE_RANGE)
            prefs[STRICT_YO] = settings.strictYo
            prefs[AUTO_PLAY_AUDIO] = settings.autoPlayAudio
            prefs[PRACTICE_MEANING] = settings.practiceMeaning
            if (settings.tenses.isNotEmpty()) {
                prefs[TENSES] = settings.tenses.mapTo(mutableSetOf()) { it.name }
            }
        }
    }

    private fun Preferences.toSettings(): TrainingSettings {
        val defaults = TrainingSettings()
        val tenses = this[TENSES]
            ?.mapNotNull { name -> Tense.entries.firstOrNull { it.name == name } }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
        return TrainingSettings(
            newCardsPerDay = this[NEW_CARDS_PER_DAY] ?: defaults.newCardsPerDay,
            sessionSize = this[SESSION_SIZE] ?: defaults.sessionSize,
            strictYo = this[STRICT_YO] ?: defaults.strictYo,
            tenses = tenses ?: defaults.tenses,
            autoPlayAudio = this[AUTO_PLAY_AUDIO] ?: defaults.autoPlayAudio,
            practiceMeaning = this[PRACTICE_MEANING] ?: defaults.practiceMeaning,
        )
    }

    companion object {
        const val FILE_NAME = "settings"

        /** Key from the time the limit covered forms only; kept so the value stays. */
        private val NEW_CARDS_PER_DAY = intPreferencesKey("new_forms_per_day")
        private val SESSION_SIZE = intPreferencesKey("session_size")
        private val STRICT_YO = booleanPreferencesKey("strict_yo")
        private val TENSES = stringSetPreferencesKey("tenses")
        private val AUTO_PLAY_AUDIO = booleanPreferencesKey("auto_play_audio")
        private val PRACTICE_MEANING = booleanPreferencesKey("practice_meaning")
    }
}
