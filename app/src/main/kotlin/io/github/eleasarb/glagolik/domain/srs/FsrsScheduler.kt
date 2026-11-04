package io.github.eleasarb.glagolik.domain.srs

import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.random.Random

/*
 * FSRS-6 scheduler, a Kotlin port of py-fsrs 6.3.2 (Scheduler.review_card and helpers).
 * py-fsrs: Copyright (c) 2022 Open Spaced Repetition, MIT License,
 * https://github.com/open-spaced-repetition/py-fsrs
 *
 * Behaviour matches py-fsrs with the same parameters; FsrsSchedulerTest checks this
 * against values produced by py-fsrs itself.
 */

enum class Rating(val value: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4),
}

enum class CardState {
    LEARNING,
    REVIEW,
    RELEARNING,
}

/** Scheduling state of one card. A new card is LEARNING at step 0 without memory state. */
data class SrsCard(
    val due: Instant,
    val state: CardState = CardState.LEARNING,
    val step: Int? = 0,
    val stability: Double? = null,
    val difficulty: Double? = null,
    val lastReview: Instant? = null,
)

class FsrsScheduler(
    parameters: List<Double> = DEFAULT_PARAMETERS,
    desiredRetention: Double = DEFAULT_RETENTION,
    private val learningSteps: List<Duration> = DEFAULT_LEARNING_STEPS,
    private val relearningSteps: List<Duration> = DEFAULT_RELEARNING_STEPS,
    private val maximumInterval: Int = DEFAULT_MAXIMUM_INTERVAL,
    /** `null` disables fuzzing (deterministic, used in tests). */
    private val fuzz: Random? = Random.Default,
) {
    private val model = FsrsModel(parameters, desiredRetention, maximumInterval)

    fun review(card: SrsCard, rating: Rating, now: Instant): SrsCard {
        val elapsedDays = card.lastReview?.let { Duration.between(it, now).toDays() }
        val updated = updateMemory(card, rating, now, elapsedDays)
        val scheduled = when (card.state) {
            CardState.LEARNING -> scheduleStep(updated, rating, learningSteps)
            CardState.RELEARNING -> scheduleStep(updated, rating, relearningSteps)
            CardState.REVIEW -> scheduleReview(updated, rating)
        }
        val interval = if (fuzz != null && scheduled.card.state == CardState.REVIEW) {
            fuzzed(scheduled.interval, fuzz)
        } else {
            scheduled.interval
        }
        return scheduled.card.copy(due = now + interval, lastReview = now)
    }

    /** Probability of recall at [now]; 0 for cards never reviewed. */
    fun retrievability(card: SrsCard, now: Instant): Double {
        val stability = card.stability
        val lastReview = card.lastReview
        return if (stability == null || lastReview == null) {
            0.0
        } else {
            model.retrievability(stability, Duration.between(lastReview, now).toDays())
        }
    }

    private data class Scheduled(val card: SrsCard, val interval: Duration)

    /** New stability and difficulty; same rules for all states in py-fsrs. */
    private fun updateMemory(
        card: SrsCard,
        rating: Rating,
        now: Instant,
        elapsedDays: Long?,
    ): SrsCard {
        val stability = card.stability
        val difficulty = card.difficulty
        return when {
            stability == null || difficulty == null -> card.copy(
                stability = model.initialStability(rating),
                difficulty = model.initialDifficulty(rating),
            )

            elapsedDays != null && elapsedDays < 1 -> card.copy(
                stability = model.shortTermStability(stability, rating),
                difficulty = model.nextDifficulty(difficulty, rating),
            )

            else -> card.copy(
                stability = model.nextStability(
                    difficulty = difficulty,
                    stability = stability,
                    retrievability = retrievability(card, now),
                    rating = rating,
                ),
                difficulty = model.nextDifficulty(difficulty, rating),
            )
        }
    }

    /** Learning and relearning: walk through [steps], graduate to REVIEW at the end. */
    private fun scheduleStep(card: SrsCard, rating: Rating, steps: List<Duration>): Scheduled {
        val step = checkNotNull(card.step)
        val pastLastStep = step >= steps.size && rating != Rating.AGAIN
        return when {
            steps.isEmpty() || pastLastStep -> graduate(card)

            rating == Rating.AGAIN -> Scheduled(card.copy(step = 0), steps[0])

            rating == Rating.HARD -> Scheduled(card, hardStepInterval(step, steps))

            rating == Rating.GOOD && step + 1 < steps.size ->
                Scheduled(card.copy(step = step + 1), steps[step + 1])

            else -> graduate(card)
        }
    }

    private fun scheduleReview(card: SrsCard, rating: Rating): Scheduled =
        if (rating == Rating.AGAIN && relearningSteps.isNotEmpty()) {
            Scheduled(card.copy(state = CardState.RELEARNING, step = 0), relearningSteps[0])
        } else {
            Scheduled(card, Duration.ofDays(model.nextIntervalDays(checkNotNull(card.stability))))
        }

    private fun graduate(card: SrsCard): Scheduled {
        val days = model.nextIntervalDays(checkNotNull(card.stability))
        return Scheduled(card.copy(state = CardState.REVIEW, step = null), Duration.ofDays(days))
    }

    private fun hardStepInterval(step: Int, steps: List<Duration>): Duration = when {
        // 1.5 x the only step, or the mean of the first two steps
        step == 0 && steps.size == 1 -> steps[0].plus(steps[0].dividedBy(2))

        step == 0 -> steps[0].plus(steps[1]).dividedBy(2)

        else -> steps[step]
    }

    /** Random spread of review intervals (py-fsrs _get_fuzzed_interval). */
    private fun fuzzed(interval: Duration, random: Random): Duration {
        val days = interval.toDays()
        if (days < FUZZ_MIN_DAYS) return interval
        var delta = 1.0
        FUZZ_RANGES.forEach { range ->
            delta += range.factor * max(min(days.toDouble(), range.end) - range.start, 0.0)
        }
        val maxDays = min(round(days + delta).toLong(), maximumInterval.toLong())
        val minDays = min(max(FUZZ_MIN_RESULT_DAYS, round(days - delta).toLong()), maxDays)
        val fuzzedDays = round(random.nextDouble() * (maxDays - minDays + 1) + minDays).toLong()
        return Duration.ofDays(min(fuzzedDays, maximumInterval.toLong()))
    }

    companion object {
        const val DEFAULT_RETENTION = 0.9
        const val DEFAULT_MAXIMUM_INTERVAL = 36_500

        /** FSRS-6 default parameters (py-fsrs 6.3.2 DEFAULT_PARAMETERS). */
        @Suppress("MagicNumber")
        val DEFAULT_PARAMETERS = listOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666,
            0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658,
            0.1542,
        )
        val DEFAULT_LEARNING_STEPS: List<Duration> = listOf(
            Duration.ofMinutes(1),
            Duration.ofMinutes(10),
        )
        val DEFAULT_RELEARNING_STEPS: List<Duration> = listOf(Duration.ofMinutes(10))

        private const val FUZZ_MIN_DAYS = 2.5
        private const val FUZZ_MIN_RESULT_DAYS = 2L

        private class FuzzRange(val start: Double, val end: Double, val factor: Double)

        private val FUZZ_RANGES = listOf(
            FuzzRange(start = 2.5, end = 7.0, factor = 0.15),
            FuzzRange(start = 7.0, end = 20.0, factor = 0.1),
            FuzzRange(start = 20.0, end = Double.POSITIVE_INFINITY, factor = 0.05),
        )
    }
}
