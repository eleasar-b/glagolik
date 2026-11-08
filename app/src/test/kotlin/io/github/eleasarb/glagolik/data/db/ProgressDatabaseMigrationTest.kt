package io.github.eleasarb.glagolik.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Upgrades a progress.db created exactly like version 1 (from the exported schema in
 * app/schemas) with the production builder. Room validates the migrated schema against the
 * current entities when it opens the file and throws if they differ, so a successful open is
 * already the schema check; the assertions cover the data.
 */
@RunWith(RobolectricTestRunner::class)
class ProgressDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file: File = context.getDatabasePath(NAME)
    private var database: ProgressDatabase? = null

    @Before
    fun setUp() {
        file.parentFile?.mkdirs()
        context.deleteDatabase(NAME)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(NAME)
    }

    @Test
    fun `version 1 progress survives the upgrade to version 2`() = runTest {
        createVersion1 { db ->
            db.execSQL(
                "INSERT INTO card (verb_id, form, state, step, stability, difficulty, due_at, " +
                    "last_review_at, introduced_at) VALUES " +
                    "(2, 'FUTURE_SG1', 'REVIEW', NULL, 2.3065, 2.1181, 1767427260000, " +
                    "1767254460000, 1767254400000), " +
                    "(1, 'PAST_M', 'LEARNING', 0, NULL, NULL, 1767254400000, NULL, 1767254400000)",
            )
            db.execSQL(
                "INSERT INTO review_log (id, verb_id, form, rating, reviewed_at, duration_ms) " +
                    "VALUES (7, 2, 'FUTURE_SG1', 'AGAIN', 1767254400000, 4000), " +
                    "(8, 2, 'FUTURE_SG1', 'GOOD', 1767254460000, NULL)",
            )
        }

        val migrated = open()

        val cards = migrated.progressBackupDao().getAllCards().sortedBy { it.verbId }
        assertEquals(
            listOf(
                CardEntity(
                    verbId = 1,
                    item = "PAST_M",
                    state = "LEARNING",
                    step = 0,
                    stability = null,
                    difficulty = null,
                    dueAt = 1767254400000,
                    lastReviewAt = null,
                    introducedAt = 1767254400000,
                ),
                CardEntity(
                    verbId = 2,
                    item = "FUTURE_SG1",
                    state = "REVIEW",
                    step = null,
                    stability = 2.3065,
                    difficulty = 2.1181,
                    dueAt = 1767427260000,
                    lastReviewAt = 1767254460000,
                    introducedAt = 1767254400000,
                ),
            ),
            cards,
        )
        assertEquals(
            listOf(
                ReviewLogEntity(7, 2, "FUTURE_SG1", "AGAIN", 1767254400000, 4000),
                ReviewLogEntity(8, 2, "FUTURE_SG1", "GOOD", 1767254460000, null),
            ),
            migrated.progressBackupDao().getAllLogs(),
        )
    }

    @Test
    fun `new log ids continue after the highest migrated id`() = runTest {
        createVersion1 { db ->
            db.execSQL(
                "INSERT INTO review_log (id, verb_id, form, rating, reviewed_at, duration_ms) " +
                    "VALUES (41, 2, 'PAST_M', 'GOOD', 1767254400000, NULL)",
            )
        }

        val migrated = open()
        migrated.progressDao().insertLog(
            ReviewLogEntity(
                verbId = 2,
                item = "MEANING_RU_DE",
                rating = "GOOD",
                reviewedAt = 1,
                durationMs = null,
            ),
        )

        val ids = migrated.progressBackupDao().getAllLogs().map { it.id }
        assertTrue(ids.toString(), ids.max() > 41)
    }

    @Test
    fun `an empty version 1 database upgrades too`() = runTest {
        createVersion1 {}

        assertEquals(emptyList<CardEntity>(), open().progressBackupDao().getAllCards())
    }

    @Test
    fun `migrations cover every version up to the current one`() {
        val reachable = ProgressDatabase.MIGRATIONS.fold(1) { version, migration ->
            assertEquals(version, migration.startVersion)
            migration.endVersion
        }
        assertEquals(ProgressDatabase.VERSION, reachable)
    }

    /** Tables, indices and Room's identity hash of version 1, plus [fill] for the data. */
    private fun createVersion1(fill: (SQLiteDatabase) -> Unit) {
        val schema = JSONObject(File(SCHEMA_DIR, "1.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace(TABLE_NAME, table))
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) {
                    val index = indices.getJSONObject(j).getString("createSql")
                    db.execSQL(index.replace(TABLE_NAME, table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) {
                db.execSQL(setup.getString(i))
            }
            db.version = 1
            fill(db)
        }
    }

    private fun open(): ProgressDatabase = ProgressDatabase.builder(context, NAME)
        .allowMainThreadQueries()
        .build()
        .also { database = it }

    private companion object {
        const val NAME = "migration-test.db"
        const val TABLE_NAME = "\${TABLE_NAME}"
        val SCHEMA_DIR = File("schemas/${ProgressDatabase::class.qualifiedName}")
    }
}
