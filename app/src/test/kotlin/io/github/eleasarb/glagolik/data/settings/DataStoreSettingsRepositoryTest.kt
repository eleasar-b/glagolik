package io.github.eleasarb.glagolik.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import java.io.File
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun TestScope.repository(): DataStoreSettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(temp.root, "settings.preferences_pb")
        }
        return DataStoreSettingsRepository(dataStore)
    }

    @Test
    fun `defaults apply when nothing is stored`() = runTest {
        assertEquals(TrainingSettings(), repository().current())
    }

    @Test
    fun `values are stored and read back`() = runTest {
        val repository = repository()
        repository.setNewFormsPerDay(25)
        repository.setSessionSize(30)
        repository.setStrictYo(true)
        repository.setAutoPlayAudio(true)
        repository.setTenses(setOf(Tense.PAST, Tense.IMPERATIVE))

        assertEquals(
            TrainingSettings(
                newFormsPerDay = 25,
                sessionSize = 30,
                strictYo = true,
                tenses = setOf(Tense.PAST, Tense.IMPERATIVE),
                autoPlayAudio = true,
            ),
            repository.current(),
        )
    }

    @Test
    fun `numbers are clamped to their ranges`() = runTest {
        val repository = repository()
        repository.setNewFormsPerDay(-5)
        repository.setSessionSize(1_000)

        val settings = repository.current()
        assertEquals(TrainingSettings.NEW_FORMS_RANGE.first, settings.newFormsPerDay)
        assertEquals(TrainingSettings.SESSION_SIZE_RANGE.last, settings.sessionSize)
    }

    @Test
    fun `empty tense selection is ignored`() = runTest {
        val repository = repository()
        repository.setTenses(setOf(Tense.PAST))
        repository.setTenses(emptySet())

        assertEquals(setOf(Tense.PAST), repository.current().tenses)
    }
}
