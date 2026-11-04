package io.github.eleasarb.glagolik.domain.checking

import org.junit.Assert.assertEquals
import org.junit.Test

/** Test names are ASCII on purpose, see CLAUDE.md §8. */
class AnswerCheckerTest {
    private val checker = AnswerChecker()

    @Test
    fun `exact answer without stress is correct`() {
        assertEquals(
            AnswerResult.Correct("скажу́"),
            checker.check("скажу", listOf("скажу́")),
        )
    }

    @Test
    fun `stress typed as combining acute, grave or apostrophe is ignored`() {
        val expected = listOf("говори́ть")
        listOf("говори\u0301ть", "говори\u0300ть", "говори'ть").forEach { input ->
            assertEquals(input, AnswerResult.Correct("говори́ть"), checker.check(input, expected))
        }
    }

    @Test
    fun `case and surrounding whitespace are ignored`() {
        assertEquals(
            AnswerResult.Correct("бу́ду говори́ть"),
            checker.check("  Буду   ГОВОРИТЬ ", listOf("бу́ду говори́ть")),
        )
    }

    @Test
    fun `decomposed yo and short i are normalized`() {
        // е + U+0308 and и + U+0306, as some keyboards produce them
        val result = checker.check("иде\u0308т", listOf("идёт"))
        assertEquals(AnswerResult.Correct("идёт"), result)
        assertEquals(AnswerResult.Correct("ду́май"), checker.check("думаи\u0306", listOf("ду́май")))
    }

    @Test
    fun `any variant is accepted`() {
        val variants = listOf("по́днял", "подня́л")
        assertEquals(AnswerResult.Correct("по́днял"), checker.check("поднял", variants))
    }

    @Test
    fun `wrong answer lists all expected variants`() {
        val variants = listOf("по́днял", "подня́л")
        assertEquals(AnswerResult.Incorrect(variants), checker.check("поднимал", variants))
    }

    @Test
    fun `yo typed as ye is accepted with hint`() {
        assertEquals(
            AnswerResult.Correct("идёт", yoTypedAsYe = true),
            checker.check("идет", listOf("идёт")),
        )
    }

    @Test
    fun `yo typed as ye is wrong in strict mode`() {
        assertEquals(
            AnswerResult.Incorrect(listOf("идёт"), yoTypedAsYe = true),
            AnswerChecker(strictYo = true).check("идет", listOf("идёт")),
        )
    }

    @Test
    fun `ye typed as yo is wrong`() {
        assertEquals(
            AnswerResult.Incorrect(listOf("е́ли")),
            checker.check("ёли", listOf("е́ли")),
        )
    }

    @Test
    fun `latin lookalike letters are reported, not graded`() {
        // Latin a (U+0061) and o (U+006F) inside otherwise Cyrillic input
        assertEquals(
            AnswerResult.LatinLetters(listOf('a', 'o')),
            checker.check("зн\u0061ю г\u006Fворю", listOf("зна́ю")),
        )
    }

    @Test
    fun `fully latin input is reported`() {
        val result = checker.check("znaju", listOf("зна́ю"))
        assertEquals(AnswerResult.LatinLetters(listOf('z', 'n', 'a', 'j', 'u')), result)
    }

    @Test
    fun `blank input is empty`() {
        assertEquals(AnswerResult.Empty, checker.check("   ", listOf("зна́ю")))
        assertEquals(AnswerResult.Empty, checker.check("'", listOf("зна́ю")))
    }
}
