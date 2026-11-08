package io.github.eleasarb.glagolik.domain.usecase

import io.github.eleasarb.glagolik.domain.model.CardKey
import io.github.eleasarb.glagolik.domain.model.Exercise
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainableForm
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import io.github.eleasarb.glagolik.domain.srs.Rating
import io.github.eleasarb.glagolik.testing.FakeProgressRepository
import io.github.eleasarb.glagolik.testing.FakeSettingsRepository
import io.github.eleasarb.glagolik.testing.FakeVerbRepository
import io.github.eleasarb.glagolik.testing.formKey
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
        val session = build(TrainingSettings(newCardsPerDay = 4, sessionSize = 20))

        assertEquals(
            forms.take(4).map {
                it.key
            }.toSet(),
            session.cards.map { it.task.key }.toSet(),
        )
        assertTrue(session.cards.all { it.isNew })
        assertEquals(0, session.remainingNewToday)
    }

    @Test
    fun `due forms come first and are not new`() = runTest {
        val ref = formKey(2, FormKey.PAST_M)
        progress.recordReview(ref, Rating.AGAIN, MORNING)

        val session = build(TrainingSettings(newCardsPerDay = 2), at = MORNING.plusSeconds(120))

        assertEquals(ref, session.cards.first().task.key)
        assertEquals(false, session.cards.first().isNew)
        // The due form was introduced today, so only one of the two new slots is left.
        assertEquals(2, session.cards.size)
    }

    @Test
    fun `forms introduced today count against the daily limit`() = runTest {
        progress.recordReview(formKey(1, FormKey.FUTURE_SG1), Rating.EASY, MORNING)
        progress.recordReview(formKey(1, FormKey.PAST_M), Rating.EASY, MORNING)

        val session = build(TrainingSettings(newCardsPerDay = 3), at = MORNING.plusSeconds(60))

        assertEquals(1, session.cards.count { it.isNew })
        assertEquals(formKey(1, FormKey.IMPERATIVE_SG), session.cards.single().task.key)
    }

    @Test
    fun `the daily limit resets at local midnight`() = runTest {
        progress.recordReview(formKey(1, FormKey.FUTURE_SG1), Rating.EASY, MORNING)

        val nextDay = MORNING + Duration.ofHours(16) // 00:00 local time (UTC+1) next day
        val session = build(TrainingSettings(newCardsPerDay = 2), at = nextDay)

        assertEquals(2, session.cards.count { it.isNew })
    }

    @Test
    fun `session size caps due and new forms`() = runTest {
        forms.take(3).forEach { progress.recordReview(it.key, Rating.AGAIN, MORNING) }

        val session = build(
            TrainingSettings(newCardsPerDay = 10, sessionSize = 2),
            at = MORNING + Duration.ofDays(1),
        )

        assertEquals(2, session.cards.size)
        assertTrue(session.cards.none { it.isNew })
        assertEquals(1, session.remainingDue)
    }

    @Test
    fun `only selected tenses are used`() = runTest {
        val session = build(TrainingSettings(newCardsPerDay = 10, tenses = setOf(Tense.PAST)))

        assertEquals(
            setOf(FormKey.PAST_M),
            session.cards.map {
                (it.task.key.exercise as Exercise.Form).form
            }.toSet(),
        )
        assertEquals(2, session.cards.size)
    }

    @Test
    fun `nothing to do gives an empty session`() = runTest {
        val session = build(TrainingSettings(newCardsPerDay = 0))

        assertTrue(session.cards.isEmpty())
    }

    @Test
    fun `new cards per verb come as RU-DE, DE-RU, then the forms`() = runTest {
        val session = buildWithMeaning(TrainingSettings(newCardsPerDay = 5))

        assertEquals(
            setOf(
                CardKey(1, Exercise.MeaningRuDe),
                CardKey(1, Exercise.MeaningDeRu),
                formKey(1, FormKey.FUTURE_SG1),
                formKey(1, FormKey.PAST_M),
                CardKey(2, Exercise.MeaningRuDe),
            ),
            session.cards.map { it.task.key }.toSet(),
        )
        assertTrue(session.cards.all { it.isNew })
    }

    @Test
    fun `meaning tasks carry options and also fitting verbs`() = runTest {
        val session = buildWithMeaning(TrainingSettings(newCardsPerDay = 2))
        val choice = session.cards.map { it.task }.filterIsInstance<MeaningChoiceTask>().single()
        val recall = session.cards.map { it.task }.filterIsInstance<MeaningRecallTask>().single()

        assertEquals(4, choice.choice.options.size)
        assertEquals("сказа́ть", choice.choice.options[choice.choice.correctIndex].infinitive)
        assertEquals(listOf("сказа́ть"), recall.expected)
        assertEquals(listOf(3, 4), recall.alsoFitting.map { it.id })
    }

    @Test
    fun `RU-DE of a verb always comes before its DE-RU`() = runTest {
        repeat(30) { seed ->
            val session = buildWithMeaning(TrainingSettings(newCardsPerDay = 12), seed = seed)
            val keys = session.cards.map { it.task.key }
            MeaningCatalogTest.MEANING_VERBS.forEach { verb ->
                val ruDe = keys.indexOf(CardKey(verb.id, Exercise.MeaningRuDe))
                val deRu = keys.indexOf(CardKey(verb.id, Exercise.MeaningDeRu))
                if (deRu >= 0) assertTrue("seed $seed: $keys", ruDe in 0 until deRu)
            }
        }
    }

    @Test
    fun `meaning and form cards share the daily limit`() = runTest {
        progress.recordReview(CardKey(1, Exercise.MeaningRuDe), Rating.EASY, MORNING)
        progress.recordReview(CardKey(1, Exercise.MeaningDeRu), Rating.EASY, MORNING)

        val session = buildWithMeaning(TrainingSettings(newCardsPerDay = 3), at = MORNING)

        assertEquals(listOf(formKey(1, FormKey.FUTURE_SG1)), session.cards.map { it.task.key })
        assertEquals(0, session.remainingNewToday)
    }

    @Test
    fun `due meaning cards come first`() = runTest {
        val key = CardKey(5, Exercise.MeaningDeRu)
        progress.recordReview(key, Rating.AGAIN, MORNING)

        val session = buildWithMeaning(
            TrainingSettings(newCardsPerDay = 3),
            at = MORNING.plusSeconds(120),
        )

        assertEquals(key, session.cards.first().task.key)
        assertEquals(false, session.cards.first().isNew)
        assertTrue(session.cards.first().task is MeaningRecallTask)
    }

    @Test
    fun `without meaning practice there are no meaning cards, due or new`() = runTest {
        progress.recordReview(CardKey(5, Exercise.MeaningRuDe), Rating.AGAIN, MORNING)

        val session = buildWithMeaning(
            TrainingSettings(newCardsPerDay = 10, practiceMeaning = false),
            at = MORNING + Duration.ofDays(1),
        )

        assertTrue(session.cards.isNotEmpty())
        assertTrue(session.cards.all { it.task is ConjugationTask })
        assertEquals(0, session.remainingDue)
    }

    @Test
    fun `the tense selection does not affect meaning cards`() = runTest {
        val session = buildWithMeaning(
            TrainingSettings(newCardsPerDay = 3, tenses = setOf(Tense.PAST)),
        )

        assertEquals(
            setOf(
                CardKey(1, Exercise.MeaningRuDe),
                CardKey(1, Exercise.MeaningDeRu),
                formKey(1, FormKey.PAST_M),
            ),
            session.cards.map { it.task.key }.toSet(),
        )
    }

    private suspend fun build(settings: TrainingSettings, at: Instant = MORNING): TrainingSession =
        BuildSessionUseCase(
            verbs = FakeVerbRepository(verbs = emptyList(), trainable = forms),
            progress = progress,
            settings = FakeSettingsRepository(settings),
            clock = Clock.fixed(at, ZONE),
            random = Random(seed = 7),
        ).invoke()

    /** Verbs with translations (see MeaningCatalogTest); forms only for verbs 1 and 5. */
    private suspend fun buildWithMeaning(
        settings: TrainingSettings,
        at: Instant = MORNING,
        seed: Int = 7,
    ): TrainingSession {
        val verbs = MeaningCatalogTest.MEANING_VERBS
        val trainable = listOf(
            TrainableForm(verbs[0], FormKey.FUTURE_SG1, listOf("скажу́")),
            TrainableForm(verbs[0], FormKey.PAST_M, listOf("сказа́л")),
            TrainableForm(verbs[4], FormKey.PAST_M, listOf("е́л")),
        )
        return BuildSessionUseCase(
            verbs = FakeVerbRepository(
                verbs = verbs,
                trainable = trainable,
                partners = mapOf(1 to setOf(7)),
            ),
            progress = progress,
            settings = FakeSettingsRepository(settings),
            clock = Clock.fixed(at, ZONE),
            random = Random(seed),
        ).invoke()
    }

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Europe/Zurich")

        /** 08:00 local time (UTC+1 in January). */
        val MORNING: Instant = Instant.parse("2026-01-10T07:00:00Z")
    }
}
