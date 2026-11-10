package io.github.eleasarb.glagolik.ui.verbs

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/** German puts a no-break space before the percent sign. */
private const val NBSP = "\u00A0"

class LearnedPercentTest {
    private val german = Locale.GERMAN
    private val english = Locale.US

    @Test
    fun `one decimal below ten percent, in the device language`() {
        assertEquals(LearnedPercent("0,4$NBSP%"), learnedPercent(37, 8877, german))
        assertEquals(LearnedPercent("0.4%"), learnedPercent(37, 8877, english))
        assertEquals(LearnedPercent("5,8$NBSP%"), learnedPercent(1, 17, german))
    }

    @Test
    fun `whole numbers from ten percent on`() {
        assertEquals(LearnedPercent("52%"), learnedPercent(9, 17, english))
        assertEquals(LearnedPercent("10%"), learnedPercent(1, 10, english))
    }

    @Test
    fun `rounded down, so only everything is a hundred percent`() {
        assertEquals(LearnedPercent("99%"), learnedPercent(8876, 8877, english))
        assertEquals(LearnedPercent("100%"), learnedPercent(8877, 8877, english))
        assertEquals(LearnedPercent("0.9%"), learnedPercent(99, 10_000, english))
    }

    @Test
    fun `shares round down without floating point surprises`() {
        assertEquals(LearnedPercent("30%"), sharePercent(0.3, english))
        assertEquals(LearnedPercent("0.3%"), sharePercent(0.003, english))
        assertEquals(LearnedPercent("44%"), sharePercent(0.442, english))
        assertEquals(LearnedPercent("0.1%", isBelowSmallest = true), sharePercent(0.0002, english))
    }

    @Test
    fun `something learned is never shown as zero`() {
        assertEquals(
            LearnedPercent("0.1%", isBelowSmallest = true),
            learnedPercent(1, 8877, english),
        )
        assertEquals(LearnedPercent("0%"), learnedPercent(0, 8877, english))
        assertEquals(LearnedPercent("0%"), learnedPercent(0, 0, english))
    }
}
