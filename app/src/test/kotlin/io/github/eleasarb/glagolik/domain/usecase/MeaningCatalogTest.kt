package io.github.eleasarb.glagolik.domain.usecase

import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.CoreVerb
import io.github.eleasarb.glagolik.testing.verbSummary
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningCatalogTest {
    private val verbs = MEANING_VERBS.map { verb ->
        CoreVerb(verb, partnerIds = if (verb.id == SKAZAT) setOf(SPAT) else emptySet())
    }
    private val catalog = MeaningCatalog(verbs)

    @Test
    fun `verbs without both translations do not take part`() {
        assertTrue(catalog.contains(SKAZAT))
        assertFalse(catalog.contains(NO_TRANSLATION))
        assertNull(catalog.verb(NO_TRANSLATION))
    }

    @Test
    fun `distractors skip the verb, its partner and verbs sharing a meaning`() {
        repeat(20) { seed ->
            val choice = checkNotNull(catalog.choice(SKAZAT, Random(seed)))

            // 2, 3 and 4 share a meaning (German or English), 7 is the aspect partner.
            assertEquals(setOf(SKAZAT, 5, 6, 8), choice.options.map { it.id }.toSet())
            assertEquals(SKAZAT, choice.options[choice.correctIndex].id)
        }
    }

    @Test
    fun `the partner link counts in both directions`() {
        repeat(20) { seed ->
            val choice = checkNotNull(catalog.choice(SPAT, Random(seed)))
            assertFalse(SKAZAT in choice.options.map { it.id })
        }
    }

    @Test
    fun `there are always three distractors and the right answer moves around`() {
        val positions = (0 until 40).map { seed ->
            val choice = checkNotNull(catalog.choice(5, Random(seed)))
            assertEquals(4, choice.options.distinctBy { it.id }.size)
            choice.correctIndex
        }
        assertEquals(setOf(0, 1, 2, 3), positions.toSet())
    }

    @Test
    fun `too few distractors give no choice`() {
        val small = MeaningCatalog(verbs.take(4))

        assertNull(small.choice(SKAZAT, Random(1)))
    }

    @Test
    fun `also fitting are verbs with the same aspect and a shared meaning`() {
        // 2 shares "sagen" but is imperfective.
        assertEquals(listOf(3, 4), catalog.alsoFitting(SKAZAT).map { it.id })
        assertEquals(emptyList<Int>(), catalog.alsoFitting(5).map { it.id })
    }

    companion object {
        const val SKAZAT = 1
        const val SPAT = 7
        const val NO_TRANSLATION = 11

        /** Core verbs in frequency order, with overlapping meanings on purpose. */
        val MEANING_VERBS = listOf(
            verbSummary(1, "сказа́ть", Aspect.PERFECTIVE, "sagen, mitteilen", "say, tell"),
            verbSummary(2, "говори́ть", Aspect.IMPERFECTIVE, "sprechen, sagen", "speak, talk"),
            verbSummary(3, "сообщи́ть", Aspect.PERFECTIVE, "mitteilen, melden", "inform"),
            verbSummary(4, "рассказа́ть", Aspect.PERFECTIVE, "erzählen", "tell"),
            verbSummary(5, "есть", Aspect.IMPERFECTIVE, "essen", "eat"),
            verbSummary(6, "жить", Aspect.IMPERFECTIVE, "leben, wohnen", "live"),
            verbSummary(7, "спать", Aspect.IMPERFECTIVE, "schlafen", "sleep"),
            verbSummary(8, "пить", Aspect.IMPERFECTIVE, "trinken", "drink"),
            verbSummary(11, "быва́ть", Aspect.IMPERFECTIVE, "", "happen"),
        )
    }
}
