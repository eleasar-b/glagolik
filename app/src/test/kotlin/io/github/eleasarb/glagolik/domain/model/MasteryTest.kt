package io.github.eleasarb.glagolik.domain.model

import io.github.eleasarb.glagolik.domain.srs.CardState
import io.github.eleasarb.glagolik.domain.srs.SrsCard
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MasteryTest {
    private val due = Instant.parse("2026-01-01T08:00:00Z")

    @Test
    fun `unanswered form is new`() {
        assertEquals(MasteryLevel.NEW, (null as SrsCard?).masteryLevel())
    }

    @Test
    fun `learning and relearning cards are learning`() {
        assertEquals(MasteryLevel.LEARNING, SrsCard(due = due).masteryLevel())
        assertEquals(
            MasteryLevel.LEARNING,
            SrsCard(due = due, state = CardState.RELEARNING, stability = 40.0).masteryLevel(),
        )
    }

    @Test
    fun `review cards split at three weeks of stability`() {
        fun review(stability: Double?) =
            SrsCard(due = due, state = CardState.REVIEW, step = null, stability = stability)

        assertEquals(MasteryLevel.YOUNG, review(null).masteryLevel())
        assertEquals(MasteryLevel.YOUNG, review(20.99).masteryLevel())
        assertEquals(MasteryLevel.MATURE, review(MATURE_STABILITY_DAYS).masteryLevel())
        assertEquals(MasteryLevel.MATURE, review(120.0).masteryLevel())
    }

    @Test
    fun `counts are grouped per level`() {
        val counts = MasteryCounts.of(
            listOf(
                MasteryLevel.NEW,
                MasteryLevel.NEW,
                MasteryLevel.LEARNING,
                MasteryLevel.MATURE,
            ),
        )

        assertEquals(MasteryCounts(new = 2, learning = 1, young = 0, mature = 1), counts)
        assertEquals(4, counts.total)
        assertEquals(2, counts[MasteryLevel.NEW])
        assertEquals(1, counts[MasteryLevel.MATURE])
    }

    @Test
    fun `a verb counts as started once one form was answered`() {
        assertFalse(MasteryCounts(new = 17).isStarted)
        assertFalse(MasteryCounts().isStarted)
        assertTrue(MasteryCounts(new = 16, learning = 1).isStarted)
    }
}
