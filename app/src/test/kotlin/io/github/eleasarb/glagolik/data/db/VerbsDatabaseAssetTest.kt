package io.github.eleasarb.glagolik.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.eleasarb.glagolik.data.repository.OfflineVerbRepository
import io.github.eleasarb.glagolik.domain.usecase.MeaningCatalog
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Opens the real bundled verbs.db through Room, exactly as the app does on first start.
 * Fails if Room rejects the pre-packaged database.
 */
@RunWith(RobolectricTestRunner::class)
class VerbsDatabaseAssetTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: VerbsDatabase

    @Before
    fun open() {
        context.deleteDatabase("verbs.db")
        database = VerbsDatabase.create(context)
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun `core verbs are available in frequency order`() = runTest {
        val verbs = database.verbDao().observeCoreVerbs("").first()

        assertEquals(CORE_VERBS, verbs.size)
        assertEquals("быть", verbs.first().infinitive)
    }

    @Test
    fun `search matches infinitive and translation`() = runTest {
        val dao = database.verbDao()

        assertTrue(dao.observeCoreVerbs("говорить").first().any { it.infinitiveBare == "говорить" })
        assertTrue(dao.observeCoreVerbs("sagen").first().any { it.infinitiveBare == "сказать" })
    }

    @Test
    fun `every core verb gets both meaning cards with three distractors`() = runTest {
        val core = OfflineVerbRepository(database.verbDao()).getCoreVerbs()
        val catalog = MeaningCatalog(core)

        assertEquals(CORE_VERBS, catalog.verbs.size)
        core.forEach { verb ->
            val choice = catalog.choice(verb.summary.id, Random(verb.summary.id))
            assertNotNull(verb.summary.infinitive, choice)
            val options = checkNotNull(choice).options.map { it.id }
            assertTrue(options.none { it in verb.partnerIds })
        }
        val skazat = core.first { it.summary.infinitive == "сказа́ть" }
        val alsoFitting = catalog.alsoFitting(skazat.summary.id).map { it.infinitive }
        assertTrue(alsoFitting.toString(), "сообщи́ть" in alsoFitting)
    }

    @Test
    fun `aspect partners resolve to verbs`() = runTest {
        val dao = database.verbDao()
        val skazat = dao.observeCoreVerbs("сказать").first().first {
            it.infinitiveBare == "сказать"
        }

        val partners = dao.getPartners(skazat.id).map { it.infinitiveBare }

        assertTrue(partners.toString(), "говорить" in partners)
    }

    // ASCII name: test lambdas become class files, and non-UTF-8 file systems reject Cyrillic.
    @Test
    fun `present of byt is stored but not trainable`() = runTest {
        val dao = database.verbDao()
        val byt = dao.observeCoreVerbs("быть").first().first { it.infinitiveBare == "быть" }

        val present = dao.getForms(byt.id).filter { it.form.startsWith("PRESENT") }

        assertEquals(6, present.size)
        assertTrue(present.none { it.isTrainable })
    }

    private companion object {
        const val CORE_VERBS = 600
    }
}
