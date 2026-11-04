package io.github.eleasarb.glagolik.domain.srs

import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsSchedulerTest {
    private val scheduler = FsrsScheduler(fuzz = null)

    /**
     * Replays every scenario of `srs/fsrs-reference.csv`, produced by py-fsrs 6.3.2 with
     * `srs/generate_fsrs_reference.py`, and compares each step.
     */
    @Test
    fun `port matches py-fsrs reference values`() {
        val scenarios = readReference().groupBy { it.scenario }
        assertEquals(5, scenarios.size)
        scenarios.forEach { (name, steps) ->
            var card = SrsCard(due = Instant.ofEpochSecond(steps.first().reviewedAt))
            steps.forEachIndexed { index, expected ->
                val label = "$name step $index"
                card =
                    scheduler.review(
                        card,
                        expected.rating,
                        Instant.ofEpochSecond(expected.reviewedAt),
                    )
                assertEquals(label, expected.state, card.state)
                assertEquals(label, expected.step, card.step)
                assertEquals(label, expected.stability, checkNotNull(card.stability), TOLERANCE)
                assertEquals(label, expected.difficulty, checkNotNull(card.difficulty), TOLERANCE)
                assertEquals(label, expected.due, card.due.epochSecond)
                assertEquals(label, expected.reviewedAt, card.lastReview?.epochSecond)
            }
        }
    }

    @Test
    fun `new card is due immediately and has no memory state`() {
        val card = SrsCard(due = START)
        assertEquals(CardState.LEARNING, card.state)
        assertEquals(0, card.step)
        assertEquals(0.0, scheduler.retrievability(card, START), 0.0)
    }

    @Test
    fun `retrievability is 0_9 after the scheduled interval`() {
        // Interval days are rounded, so retrievability at due is close to, not exactly, 0.9.
        val card = scheduler.review(SrsCard(due = START), Rating.EASY, START)
        val atDue = scheduler.retrievability(card, card.due)
        assertEquals(FsrsScheduler.DEFAULT_RETENTION, atDue, RETENTION_TOLERANCE)
    }

    @Test
    fun `fuzz keeps review intervals within the documented range`() {
        val fuzzy = FsrsScheduler(fuzz = Random(seed = 42))
        val reviewed = scheduler.review(SrsCard(due = START), Rating.EASY, START)
        val second = reviewed.due
        val exact = scheduler.review(reviewed, Rating.EASY, second)
        val exactDays = Duration.between(second, exact.due).toDays()

        repeat(FUZZ_SAMPLES) {
            val fuzzed = fuzzy.review(reviewed, Rating.EASY, second)
            val days = Duration.between(second, fuzzed.due).toDays()
            assertTrue("$days vs $exactDays", days in (exactDays * 9 / 10)..(exactDays * 11 / 10))
        }
    }

    @Test
    fun `fuzz does not touch learning steps`() {
        val fuzzy = FsrsScheduler(fuzz = Random(seed = 1))
        val card = fuzzy.review(SrsCard(due = START), Rating.GOOD, START)
        assertEquals(START + Duration.ofMinutes(10), card.due)
    }

    private data class ReferenceStep(
        val scenario: String,
        val rating: Rating,
        val reviewedAt: Long,
        val state: CardState,
        val step: Int?,
        val stability: Double,
        val difficulty: Double,
        val due: Long,
    )

    private fun readReference(): List<ReferenceStep> {
        val stream = checkNotNull(javaClass.getResourceAsStream("/srs/fsrs-reference.csv"))
        return stream.bufferedReader().readLines().drop(1).map { line ->
            val cells = line.split(',')
            ReferenceStep(
                scenario = cells[0],
                rating = Rating.valueOf(cells[1]),
                reviewedAt = cells[2].toLong(),
                state = CardState.valueOf(cells[3]),
                step = cells[4].toIntOrNull(),
                stability = cells[5].toDouble(),
                difficulty = cells[6].toDouble(),
                due = cells[7].toLong(),
            )
        }
    }

    private companion object {
        val START: Instant = Instant.parse("2026-01-01T08:00:00Z")
        const val TOLERANCE = 1e-9
        const val RETENTION_TOLERANCE = 0.01
        const val FUZZ_SAMPLES = 200
    }
}
