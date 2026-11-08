package io.github.eleasarb.glagolik.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningsTest {
    @Test
    fun `commas and semicolons separate meanings`() {
        assertEquals(
            listOf("sehen", "treffen", "begegnen"),
            meaningsOf("sehen; treffen,  begegnen"),
        )
    }

    @Test
    fun `separators inside parentheses do not split`() {
        assertEquals(
            listOf("scheinen (den Anschein haben, anmuten)", "erscheinen"),
            meaningsOf("scheinen (den Anschein haben, anmuten), erscheinen"),
        )
    }

    @Test
    fun `empty parts and blank translations give no meanings`() {
        assertEquals(listOf("geben"), meaningsOf("geben, ,"))
        assertEquals(emptyList<String>(), meaningsOf("  "))
    }

    @Test
    fun `preview shows three short meanings`() {
        assertEquals("sprechen, reden, sagen", previewMeanings("sprechen, reden, sagen, quatschen"))
    }

    @Test
    fun `preview falls back to two meanings when three are too long`() {
        assertEquals(
            "verstehen, erkennen",
            previewMeanings("verstehen, erkennen; begreifen, einsehen", maxLength = 25),
        )
    }

    @Test
    fun `preview of a single meaning is that meaning`() {
        assertEquals("geben", previewMeanings("geben"))
    }

    @Test
    fun `meanings overlap per language, ignoring case`() {
        val sagen = Translation("sagen, mitteilen", "say, tell").comparableMeanings()
        val sprechen = Translation("sprechen, Sagen", "speak").comparableMeanings()
        val reden = Translation("reden", "talk, tell").comparableMeanings()
        val essen = Translation("essen", "eat").comparableMeanings()

        assertTrue(sagen.overlaps(sprechen))
        assertTrue(sagen.overlaps(reden))
        assertFalse(sagen.overlaps(essen))
    }

    @Test
    fun `a German meaning never matches an English one`() {
        val a = Translation("die", "x").comparableMeanings()
        val b = Translation("y", "die").comparableMeanings()

        assertFalse(a.overlaps(b))
    }

    @Test
    fun `exercise ids round-trip and form ids are FormKey names`() {
        val all = FormKey.entries.map { Exercise.Form(it) } + Exercise.MEANINGS
        all.forEach { assertEquals(it, Exercise.fromId(it.id)) }
        assertEquals("PAST_M", Exercise.Form(FormKey.PAST_M).id)
        assertEquals("MEANING_RU_DE", Exercise.MeaningRuDe.id)
        assertEquals("MEANING_DE_RU", Exercise.MeaningDeRu.id)
        assertEquals(null, Exercise.fromId("PAST_X"))
    }
}
