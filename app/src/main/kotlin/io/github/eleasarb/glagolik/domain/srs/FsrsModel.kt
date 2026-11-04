// The FSRS formulas address the published parameters by index (w0..w20). Keeping the
// indices and constants literal keeps this file comparable 1:1 with py-fsrs.
@file:Suppress("MagicNumber")

package io.github.eleasarb.glagolik.domain.srs

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/**
 * FSRS-6 memory model: stability, difficulty, retrievability and intervals.
 * Port of the corresponding helpers of py-fsrs 6.3.2 Scheduler (MIT, see FsrsScheduler).
 */
internal class FsrsModel(
    private val w: List<Double>,
    private val desiredRetention: Double,
    private val maximumInterval: Int,
) {
    private val decay = -w[20]
    private val factor = 0.9.pow(1 / decay) - 1

    init {
        require(w.size == 21) { "expected 21 parameters, got ${w.size}" }
    }

    fun retrievability(stability: Double, elapsedDays: Long): Double =
        (1 + factor * max(0L, elapsedDays) / stability).pow(decay)

    fun initialStability(rating: Rating): Double = clampStability(w[rating.value - 1])

    fun initialDifficulty(rating: Rating): Double =
        clampDifficulty(unclampedInitialDifficulty(rating))

    fun nextIntervalDays(stability: Double): Long {
        val interval = stability / factor * (desiredRetention.pow(1 / decay) - 1)
        return round(interval).toLong().coerceIn(1L, maximumInterval.toLong())
    }

    fun shortTermStability(stability: Double, rating: Rating): Double {
        var increase = exp(w[17] * (rating.value - 3 + w[18])) * stability.pow(-w[19])
        if (rating != Rating.AGAIN) increase = max(increase, 1.0)
        return clampStability(stability * increase)
    }

    fun nextDifficulty(difficulty: Double, rating: Rating): Double {
        val delta = -(w[6] * (rating.value - 3))
        val damped = difficulty + (10.0 - difficulty) * delta / 9.0
        val reverted = w[7] * unclampedInitialDifficulty(Rating.EASY) + (1 - w[7]) * damped
        return clampDifficulty(reverted)
    }

    fun nextStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        rating: Rating,
    ): Double {
        val next = if (rating == Rating.AGAIN) {
            val longTerm = w[11] * difficulty.pow(-w[12]) * ((stability + 1).pow(w[13]) - 1) *
                exp((1 - retrievability) * w[14])
            val shortTerm = stability / exp(w[17] * w[18])
            min(longTerm, shortTerm)
        } else {
            val hardPenalty = if (rating == Rating.HARD) w[15] else 1.0
            val easyBonus = if (rating == Rating.EASY) w[16] else 1.0
            stability * (
                1 + exp(w[8]) * (11 - difficulty) * stability.pow(-w[9]) *
                    (exp((1 - retrievability) * w[10]) - 1) * hardPenalty * easyBonus
                )
        }
        return clampStability(next)
    }

    private fun unclampedInitialDifficulty(rating: Rating): Double =
        w[4] - exp(w[5] * (rating.value - 1)) + 1

    private fun clampStability(stability: Double): Double = max(stability, 0.001)

    private fun clampDifficulty(difficulty: Double): Double = difficulty.coerceIn(1.0, 10.0)
}
