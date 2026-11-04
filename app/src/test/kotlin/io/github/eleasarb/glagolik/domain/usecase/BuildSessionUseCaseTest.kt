package io.github.eleasarb.glagolik.domain.usecase

import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.FormRef
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import io.github.eleasarb.glagolik.domain.srs.Rating
import io.github.eleasarb.glagolik.testing.FakeProgressRepository
import io.github.eleasarb.glagolik.testing.FakeSettingsRepository
import io.github.eleasarb.glagolik.testing.FakeVerbRepository
import io.github.eleasarb.glagolik.testing.trainableForm
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildSessionUseCaseTest {
    /** Two verbs with three forms each, in frequency order. */
    private val forms = listOf(
        trainableForm(1, FormKey.FUTURE_SG1, "бу́ду"),
        trainableForm(1, FormKey.PAST_M, "бы́л"),
        trainableForm(1, FormKey.IMPERATIVE_SG, "бу́дь"),
        trainableForm(2, FormKey.FUTURE_SG1, "скажу́"),
        trainableForm(2, FormKey.PAST_M, "сказа́л"),
        trainableForm(2, FormKey.IMPERATIVE_SG, "скажи́"),
    )
    private val progress = FakeProgressRepository()

    @Test
    fun `fresh start gives new forms up to the daily limit in frequency order`() = runTest {
        val session = build(TrainingSettings(newFormsPerDay = 4, sessionSize = 20))

        assertEquals(
            forms.take(4).map {
                it.ref
            }.toSet(),
            session.cards.map { it.form.ref }.toSet(),
        )
        assertTrue(session.cards.all { it.isNew })
        assertEquals(0, session.remainingNewToday)
    }

    @Test
    fun `due forms come first and are not new`() = runTest {
        val ref = FormRef(2, FormKey.PAST_M)
        progress.recordReview(ref, Rating.AGAIN, MORNING)

        val session = build(TrainingSettings(newFormsPerDay = 2), at = MORNING.plusSeconds(120))

        assertEquals(ref, session.cards.first().form.ref)
        assertEquals(false, session.cards.first().isNew)
        // The due form was introduced today, so only one of the two new slots is left.
        assertEquals(2, session.cards.size)
    }

    @Test
    fun `forms introduced today count against the daily limit`() = runTest {
        progress.recordReview(FormRef(1, FormKey.FUTURE_SG1), Rating.EASY, MORNING)
        progress.recordReview(FormRef(1, FormKey.PAST_M), Rating.EASY, MORNING)

        val session = build(TrainingSettings(newFormsPerDay = 3), at = MORNING.plusSeconds(60))

        assertEquals(1, session.cards.count { it.isNew })
        assertEquals(FormRef(1, FormKey.IMPERATIVE_SG), session.cards.single().form.ref)
    }

    @Test
    fun `the daily limit resets at local midnight`() = runTest {
        progress.recordReview(FormRef(1, FormKey.FUTURE_SG1), Rating.EASY, MORNING)

        val nextDay = MORNING + Duration.ofHours(16) // 00:00 local time (UTC+1) next day
        val session = build(TrainingSettings(newFormsPerDay = 2), at = nextDay)

        assertEquals(2, session.cards.count { it.isNew })
    }

    @Test
    fun `session size caps due and new forms`() = runTest {
        forms.take(3).forEach { progress.recordReview(it.ref, Rating.AGAIN, MORNING) }

        val session = build(
            TrainingSettings(newFormsPerDay = 10, sessionSize = 2),
            at = MORNING + Duration.ofDays(1),
        )

        assertEquals(2, session.cards.size)
        assertTrue(session.cards.none { it.isNew })
        assertEquals(1, session.remainingDue)
    }

    @Test
    fun `only selected tenses are used`() = runTest {
        val session = build(TrainingSettings(newFormsPerDay = 10, tenses = setOf(Tense.PAST)))

        assertEquals(setOf(FormKey.PAST_M), session.cards.map { it.form.ref.form }.toSet())
        assertEquals(2, session.cards.size)
    }

    @Test
    fun `nothing to do gives an empty session`() = runTest {
        val session = build(TrainingSettings(newFormsPerDay = 0))

        assertTrue(session.cards.isEmpty())
    }

    private suspend fun build(settings: TrainingSettings, at: Instant = MORNING): TrainingSession =
        BuildSessionUseCase(
            verbs = FakeVerbRepository(verbs = emptyList(), trainable = forms),
            progress = progress,
            settings = FakeSettingsRepository(settings),
            clock = Clock.fixed(at, ZONE),
            random = Random(seed = 7),
        ).invoke()

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Europe/Zurich")

        /** 08:00 local time (UTC+1 in January). */
        val MORNING: Instant = Instant.parse("2026-01-10T07:00:00Z")
    }
}
