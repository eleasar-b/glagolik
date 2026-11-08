package io.github.eleasarb.glagolik.domain.model

import io.github.eleasarb.glagolik.domain.model.MasteryLevel.LEARNING
import io.github.eleasarb.glagolik.domain.model.MasteryLevel.MATURE
import io.github.eleasarb.glagolik.domain.model.MasteryLevel.NEW
import io.github.eleasarb.glagolik.domain.model.MasteryLevel.YOUNG
import org.junit.Assert.assertEquals
import org.junit.Test

class VerbLevelTest {
    @Test
    fun `a verb is new until one of its cards was answered`() {
        assertEquals(NEW, verbLevel(listOf(NEW, NEW)))
        assertEquals(LEARNING, verbLevel(listOf(NEW, MATURE)))
    }

    @Test
    fun `young or mature only when every card is`() {
        assertEquals(MATURE, verbLevel(listOf(MATURE, MATURE)))
        assertEquals(YOUNG, verbLevel(listOf(MATURE, YOUNG)))
        assertEquals(LEARNING, verbLevel(listOf(MATURE, YOUNG, LEARNING)))
    }
}
