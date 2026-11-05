package io.github.eleasarb.glagolik.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.eleasarb.glagolik.data.db.ProgressDatabase
import io.github.eleasarb.glagolik.data.repository.OfflineProgressRepository
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.FormRef
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import io.github.eleasarb.glagolik.domain.srs.FsrsScheduler
import io.github.eleasarb.glagolik.domain.srs.Rating
import io.github.eleasarb.glagolik.testing.FakeSettingsRepository
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Real in-memory Room databases and real file Uris through the ContentResolver. */
@RunWith(RobolectricTestRunner::class)
class DocumentBackupRepositoryTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = Clock.fixed(NOW, ZoneOffset.UTC)
    private lateinit var source: ProgressDatabase
    private lateinit var target: ProgressDatabase

    @Before
    fun setUp() {
        source = inMemory()
        target = inMemory()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    @Test
    fun `export and import round-trip progress and settings`() = runTest {
        val progress = OfflineProgressRepository(source.progressDao(), FsrsScheduler(fuzz = null))
        progress.recordReview(SKAZAT, Rating.AGAIN, NOW, Duration.ofSeconds(3))
        progress.recordReview(SKAZAT, Rating.GOOD, NOW + Duration.ofMinutes(1))
        progress.recordReview(BYT, Rating.EASY, NOW)
        val settings =
            TrainingSettings(newFormsPerDay = 30, strictYo = true, tenses = setOf(Tense.PAST))
        val file = Uri.fromFile(File(temp.root, "export.json"))

        val exported = repository(source, FakeSettingsRepository(settings)).export(file)
        val targetSettings = FakeSettingsRepository()
        val imported = repository(target, targetSettings).import(file)

        assertEquals(BackupSummary(cards = 2, reviews = 3), exported)
        assertEquals(exported, imported)
        assertEquals(
            source.progressBackupDao().getAllCards().toSet(),
            target.progressBackupDao().getAllCards().toSet(),
        )
        assertEquals(
            source.progressBackupDao().getAllLogs().map { it.copy(id = 0) },
            target.progressBackupDao().getAllLogs().map { it.copy(id = 0) },
        )
        assertEquals(settings, targetSettings.current())
    }

    @Test
    fun `import replaces existing progress`() = runTest {
        val sourceProgress =
            OfflineProgressRepository(source.progressDao(), FsrsScheduler(fuzz = null))
        sourceProgress.recordReview(SKAZAT, Rating.GOOD, NOW)
        val targetProgress =
            OfflineProgressRepository(target.progressDao(), FsrsScheduler(fuzz = null))
        targetProgress.recordReview(BYT, Rating.GOOD, NOW)
        val file = Uri.fromFile(File(temp.root, "export.json"))

        repository(source).export(file)
        repository(target).import(file)

        assertEquals(setOf(SKAZAT), targetProgress.getKnownForms())
    }

    @Test
    fun `invalid file changes nothing`() = runTest {
        val targetProgress =
            OfflineProgressRepository(target.progressDao(), FsrsScheduler(fuzz = null))
        targetProgress.recordReview(BYT, Rating.GOOD, NOW)
        val settings = FakeSettingsRepository(TrainingSettings(sessionSize = 40))
        val file = File(temp.root, "broken.json").apply { writeText("""{"format":"other"}""") }

        assertThrows(BackupFormatException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository(target, settings).import(Uri.fromFile(file))
            }
        }

        assertEquals(setOf(BYT), targetProgress.getKnownForms())
        assertEquals(40, settings.current().sessionSize)
    }

    @Test
    fun `exported file is readable JSON with format marker`() = runTest {
        val file = File(temp.root, "export.json")
        repository(source).export(Uri.fromFile(file))

        val text = file.readText()
        assertTrue(text.contains("\"format\": \"glagolik-progress\""))
        assertTrue(text.contains("\"exportedAt\": \"2026-01-10T07:00:00Z\""))
    }

    private fun repository(
        database: ProgressDatabase,
        settings: FakeSettingsRepository = FakeSettingsRepository(),
    ) = DocumentBackupRepository(
        context.contentResolver,
        database.progressBackupDao(),
        settings,
        clock,
    )

    private fun inMemory(): ProgressDatabase =
        Room.inMemoryDatabaseBuilder(context, ProgressDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private companion object {
        val NOW: Instant = Instant.parse("2026-01-10T07:00:00Z")
        val SKAZAT = FormRef(2, FormKey.FUTURE_SG1)
        val BYT = FormRef(1, FormKey.PAST_M)
    }
}
