package io.github.eleasarb.glagolik.domain.model

import io.github.eleasarb.glagolik.domain.model.MasteryLevel.LEARNING
import io.github.eleasarb.glagolik.domain.model.MasteryLevel.MATURE
import io.github.eleasarb.glagolik.domain.model.MasteryLevel.NEW
import io.github.eleasarb.glagolik.domain.model.MasteryLevel.YOUNG
import io.github.eleasarb.glagolik.domain.srs.SrsCard
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class VerbLevelTest {
    @Test
    fun `a verb is new until one of its cards was answered`() {
        assertEquals(NEW, verbLevel(listOf(NEW, NEW)))
        assertEquals(NEW, verbLevel(emptyList()))
    }

    @Test
    fun `the weakest answered card counts, new cards do not pull it down`() {
        assertEquals(YOUNG, verbLevel(listOf(NEW, YOUNG, YOUNG)))
        assertEquals(MATURE, verbLevel(listOf(NEW, MATURE)))
        assertEquals(YOUNG, verbLevel(listOf(MATURE, YOUNG)))
        assertEquals(LEARNING, verbLevel(listOf(MATURE, NEW, LEARNING)))
    }

    @Test
    fun `progress is stability over three weeks, at most one`() {
        val now = Instant.EPOCH
        assertEquals(0.0, (null as SrsCard?).progress(), 0.0)
        assertEquals(0.5, SrsCard(due = now, stability = 10.5).progress(), 1e-9)
        assertEquals(1.0, SrsCard(due = now, stability = 40.0).progress(), 0.0)
        assertEquals(0.0, SrsCard(due = now).progress(), 0.0)
    }

    @Test
    fun `average progress counts cards never answered as zero`() {
        val half = SrsCard(due = Instant.EPOCH, stability = 10.5)
        assertEquals(0.25, averageProgress(listOf(half, null)), 1e-9)
        assertEquals(0.0, averageProgress(emptyList()), 0.0)
    }
}
