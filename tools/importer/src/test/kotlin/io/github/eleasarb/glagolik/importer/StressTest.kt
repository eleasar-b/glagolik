package io.github.eleasarb.glagolik.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StressTest {
    @Test
    fun `source mark becomes combining acute after the vowel`() {
        assertEquals("говори́ть", toStressed("говори'ть"))
    }

    @Test
    fun `bare form drops marks in both notations and keeps ё`() {
        assertEquals("говорить", toBare("говори́ть"))
        assertEquals("говорить", toBare("говори'ть"))
        assertEquals("идёт", toBare("идёт"))
    }

    @Test
    fun `variants are split on comma and double slash`() {
        assertEquals(listOf("по'днял", "подня'л"), splitVariants("по'днял,подня'л"))
        assertEquals(listOf("уда'лось", "удало'сь"), splitVariants("уда'лось//удало'сь"))
        assertEquals(listOf("a", "b"), splitVariants(" a, b "))
        assertEquals(emptyList<String>(), splitVariants(""))
    }

    @Test
    fun `word with exactly one mark after a vowel is valid`() {
        assertNull(checkWord("говори'ть"))
    }

    @Test
    fun `monosyllable and ё need no mark`() {
        assertNull(checkWord("быть"))
        assertNull(checkWord("шёл"))
        assertNull(checkWord("идёшь"))
    }

    @Test
    fun `missing mark on a polysyllable is rejected`() {
        assertProblem("missing stress mark", checkWord("установить"))
    }

    @Test
    fun `two marks are rejected`() {
        assertProblem("more than one stress mark", checkWord("вы'дало'"))
    }

    @Test
    fun `mark after a consonant is rejected`() {
        assertProblem("not after a vowel", checkWord("гов'орить"))
    }

    @Test
    fun `latin lookalike letters are rejected`() {
        // Latin "a" (U+0061) instead of Cyrillic "а" (U+0430)
        assertProblem("non-Cyrillic", checkWord("знa'ть"))
    }

    private fun assertProblem(expected: String, actual: String?) {
        assertTrue("expected '$expected' in '$actual'", actual.orEmpty().contains(expected))
    }
}
