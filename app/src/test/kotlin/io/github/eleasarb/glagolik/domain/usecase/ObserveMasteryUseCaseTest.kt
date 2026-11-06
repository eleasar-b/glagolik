package io.github.eleasarb.glagolik.domain.usecase

import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.FormRef
import io.github.eleasarb.glagolik.domain.model.MasteryCounts
import io.github.eleasarb.glagolik.domain.srs.Rating
import io.github.eleasarb.glagolik.testing.FakeProgressRepository
import io.github.eleasarb.glagolik.testing.FakeVerbRepository
import io.github.eleasarb.glagolik.testing.trainableForm
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveMasteryUseCaseTest {
    private val forms = listOf(
        trainableForm(1, FormKey.FUTURE_SG1, "бу́ду"),
        trainableForm(1, FormKey.PAST_M, "бы́л"),
        trainableForm(2, FormKey.FUTURE_SG1, "скажу́"),
        trainableForm(2, FormKey.PAST_M, "сказа́л"),
        trainableForm(2, FormKey.IMPERATIVE_SG, "скажи́"),
    )
    private val progress = FakeProgressRepository()

    private fun useCase(now: Instant = NOW) = ObserveMasteryUseCase(
        verbs = FakeVerbRepository(verbs = emptyList(), trainable = forms),
        progress = progress,
        clock = Clock.fixed(now, ZoneId.of("Europe/Zurich")),
    )

    @Test
    fun `without progress every trainable form is new`() = runTest {
        val overview = useCase()().first()

        assertEquals(MasteryCounts(new = 5), overview.total)
        assertEquals(MasteryCounts(new = 2), overview.byVerb[1])
        assertEquals(MasteryCounts(new = 3), overview.byVerb[2])
        assertEquals(0, overview.dueNow)
    }

    @Test
    fun `answered forms are counted per verb and in total`() = runTest {
        val learning = FormRef(2, FormKey.FUTURE_SG1)
        val young = FormRef(2, FormKey.PAST_M)
        progress.recordReview(learning, Rating.AGAIN, NOW)
        progress.recordReview(young, Rating.EASY, NOW)

        val overview = useCase()().first()

        assertEquals(MasteryCounts(new = 1, learning = 1, young = 1), overview.byVerb[2])
        assertEquals(MasteryCounts(new = 2), overview.byVerb[1])
        assertEquals(MasteryCounts(new = 3, learning = 1, young = 1), overview.total)
    }

    @Test
    fun `due forms are counted at the current time`() = runTest {
        progress.recordReview(FormRef(1, FormKey.PAST_M), Rating.AGAIN, NOW)

        assertEquals(0, useCase(now = NOW)().first().dueNow)
        assertEquals(1, useCase(now = NOW + Duration.ofMinutes(1))().first().dueNow)
    }

    @Test
    fun `cards of forms that are not trainable are ignored`() = runTest {
        progress.recordReview(FormRef(1, FormKey.PRESENT_SG1), Rating.AGAIN, NOW)
        progress.recordReview(FormRef(99, FormKey.PAST_M), Rating.AGAIN, NOW)

        val overview = useCase(now = NOW + Duration.ofDays(1))().first()

        assertEquals(MasteryCounts(new = 5), overview.total)
        assertEquals(0, overview.dueNow)
        assertEquals(setOf(1, 2), overview.byVerb.keys)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a new answer emits an updated overview`() = runTest {
        val totals = mutableListOf<MasteryCounts>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase()().collect { totals += it.total }
        }

        progress.recordReview(FormRef(1, FormKey.PAST_M), Rating.GOOD, NOW)

        assertEquals(listOf(MasteryCounts(new = 5), MasteryCounts(new = 4, learning = 1)), totals)
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-01-01T08:00:00Z")
    }
}
