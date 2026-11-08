package io.github.eleasarb.glagolik.ui.settings

import android.net.Uri
import io.github.eleasarb.glagolik.data.backup.BackupFormatException
import io.github.eleasarb.glagolik.data.backup.BackupRepository
import io.github.eleasarb.glagolik.data.backup.BackupSummary
import io.github.eleasarb.glagolik.data.tts.SpeechAvailability
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import io.github.eleasarb.glagolik.testing.FakeSettingsRepository
import io.github.eleasarb.glagolik.testing.FakeSpeaker
import io.github.eleasarb.glagolik.testing.MainDispatcherRule
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric only for android.net.Uri. */
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()
    private val backup = FakeBackupRepository()
    private val speaker = FakeSpeaker(SpeechAvailability.INITIALIZING)
    private val clock = Clock.fixed(
        Instant.parse("2026-11-05T07:00:00Z"),
        ZoneId.of("Europe/Zurich"),
    )
    private val uri = Uri.parse("content://test/backup.json")

    private fun viewModel() = SettingsViewModel(settings, backup, speaker, clock)

    @Test
    fun `settings changes are passed to the repository`() = runTest {
        val vm = viewModel()
        collect(vm)
        vm.onNewFormsPerDayChange(20)
        vm.onTenseToggle(Tense.PRESENT)

        assertEquals(20, vm.uiState.value.settings.newFormsPerDay)
        assertEquals(Tense.entries.toSet() - Tense.PRESENT, vm.uiState.value.settings.tenses)
    }

    @Test
    fun `speech status is shown and the test sentence is spoken`() = runTest {
        val vm = viewModel()
        collect(vm)
        assertEquals(SpeechAvailability.INITIALIZING, vm.uiState.value.speech)

        speaker.availability.value = SpeechAvailability.READY
        vm.onSpeechTest()

        assertEquals(SpeechAvailability.READY, vm.uiState.value.speech)
        assertEquals(1, speaker.spoken.size)
    }

    @Test
    fun `resume checks the voice again`() {
        viewModel().onResume()

        assertEquals(1, speaker.refreshes)
    }

    @Test
    fun `successful export and import are reported`() = runTest {
        val vm = viewModel()
        collect(vm)

        vm.onExport(uri)
        assertEquals(BackupStatus.Exported(BackupSummary(5, 9)), vm.uiState.value.backupStatus)

        vm.onImport(uri)
        assertEquals(BackupStatus.Imported(BackupSummary(5, 9)), vm.uiState.value.backupStatus)
    }

    @Test
    fun `invalid file and io errors are reported`() = runTest {
        val vm = viewModel()
        collect(vm)

        backup.failure = BackupFormatException("bad")
        vm.onImport(uri)
        assertEquals(BackupStatus.InvalidFile, vm.uiState.value.backupStatus)

        backup.failure = IOException("disk")
        vm.onExport(uri)
        assertEquals(BackupStatus.Failed, vm.uiState.value.backupStatus)
    }

    @Test
    fun `export file name contains the local date`() {
        assertEquals("glagolik-progress-2026-11-05.json", viewModel().exportFileName())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.collect(vm: SettingsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
    }

    private class FakeBackupRepository : BackupRepository {
        var failure: Exception? = null

        override suspend fun export(uri: Uri): BackupSummary = result()

        override suspend fun import(uri: Uri): BackupSummary = result()

        private fun result(): BackupSummary {
            failure?.let { throw it }
            return BackupSummary(cards = 5, reviews = 9)
        }
    }
}
