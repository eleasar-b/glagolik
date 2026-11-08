package io.github.eleasarb.glagolik.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.eleasarb.glagolik.data.db.ProgressDatabase
import io.github.eleasarb.glagolik.domain.model.CardKey
import io.github.eleasarb.glagolik.domain.model.Exercise
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.srs.CardState
import io.github.eleasarb.glagolik.domain.srs.FsrsScheduler
import io.github.eleasarb.glagolik.domain.srs.Rating
import io.github.eleasarb.glagolik.domain.srs.SrsCard
import io.github.eleasarb.glagolik.testing.formKey
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Runs against a real in-memory Room database (Robolectric), no fakes for SQL. */
@RunWith(RobolectricTestRunner::class)
class OfflineProgressRepositoryTest {
    private lateinit var database: ProgressDatabase
    private lateinit var repository: OfflineProgressRepository

    private val scheduler = FsrsScheduler(fuzz = null)
    private val skazatFuture = formKey(2, FormKey.FUTURE_SG1)
    private val skazatPast = formKey(2, FormKey.PAST_M)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ProgressDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OfflineProgressRepository(database.progressDao(), scheduler)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `unknown form has no card`() = runTest {
        assertNull(repository.getCard(skazatFuture))
    }

    @Test
    fun `first review stores the scheduled state`() = runTest {
        val stored = repository.recordReview(skazatFuture, Rating.GOOD, NOW)

        assertEquals(stored, repository.getCard(skazatFuture))
        assertEquals(CardState.LEARNING, stored.state)
        assertEquals(NOW + Duration.ofMinutes(10), stored.due)
    }

    @Test
    fun `repeated reviews continue from the stored state`() = runTest {
        repository.recordReview(skazatFuture, Rating.GOOD, NOW)
        val second = repository.recordReview(skazatFuture, Rating.GOOD, NOW + TEN_MINUTES)

        val expected = scheduler.review(
            scheduler.review(SrsCard(due = NOW), Rating.GOOD, NOW),
            Rating.GOOD,
            NOW + TEN_MINUTES,
        )
        assertEquals(expected, second)
        assertEquals(CardState.REVIEW, second.state)
    }

    @Test
    fun `every review is logged with rating and duration`() = runTest {
        repository.recordReview(skazatFuture, Rating.AGAIN, NOW, Duration.ofSeconds(4))
        repository.recordReview(skazatFuture, Rating.GOOD, NOW + TEN_MINUTES)

        val logs = database.progressDao().getLogs(2, FormKey.FUTURE_SG1.name)
        assertEquals(listOf("AGAIN", "GOOD"), logs.map { it.rating })
        assertEquals(listOf(4_000L, null), logs.map { it.durationMs })
    }

    @Test
    fun `due cards are ordered by due time and limited`() = runTest {
        repository.recordReview(skazatFuture, Rating.GOOD, NOW)
        repository.recordReview(skazatPast, Rating.AGAIN, NOW)

        val due = repository.getDueCards(NOW + TEN_MINUTES, limit = 10)
        assertEquals(listOf(skazatPast, skazatFuture), due.map { it.key })
        assertEquals(1, repository.getDueCards(NOW + TEN_MINUTES, limit = 1).size)
        assertEquals(emptyList<Any>(), repository.getDueCards(NOW, limit = 10))
    }

    @Test
    fun `introduced count uses the first review only`() = runTest {
        val tomorrow = NOW + Duration.ofDays(1)
        repository.recordReview(skazatFuture, Rating.GOOD, NOW)
        repository.recordReview(skazatFuture, Rating.GOOD, tomorrow)
        repository.recordReview(skazatPast, Rating.GOOD, tomorrow)

        assertEquals(1, repository.countIntroducedSince(tomorrow))
        assertEquals(2, repository.countIntroducedSince(NOW))
        assertEquals(setOf(skazatFuture, skazatPast), repository.getKnownCards())
    }

    @Test
    fun `observed cards reflect every stored review`() = runTest {
        assertEquals(emptyMap<CardKey, SrsCard>(), repository.observeCards().first())

        val future = repository.recordReview(skazatFuture, Rating.GOOD, NOW)
        val past = repository.recordReview(skazatPast, Rating.AGAIN, NOW)
        val other = formKey(3, FormKey.PAST_M)
        repository.recordReview(other, Rating.GOOD, NOW)

        assertEquals(future, repository.observeCards().first()[skazatFuture])
        assertEquals(3, repository.observeCards().first().size)
        assertEquals(
            mapOf(
                Exercise.Form(FormKey.FUTURE_SG1) to future,
                Exercise.Form(FormKey.PAST_M) to past,
            ),
            repository.observeCards(verbId = 2).first(),
        )
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-01-01T08:00:00Z")
        val TEN_MINUTES: Duration = Duration.ofMinutes(10)
    }
}
