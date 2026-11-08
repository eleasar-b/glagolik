package io.github.eleasarb.glagolik.domain.model

import io.github.eleasarb.glagolik.domain.srs.CardState
import io.github.eleasarb.glagolik.domain.srs.SrsCard

/**
 * How well a form is known, derived from its FSRS state. Ordered from weakest to strongest.
 */
enum class MasteryLevel {
    /** Never answered. */
    NEW,

    /** In the learning or relearning steps. */
    LEARNING,

    /** In review with a stability below [MATURE_STABILITY_DAYS]. */
    YOUNG,

    /** In review with a stability of at least [MATURE_STABILITY_DAYS]. */
    MATURE,
}

/** Same threshold as a "mature" card in Anki (interval of three weeks). */
const val MATURE_STABILITY_DAYS = 21.0

/** Mastery of a form; `null` means the form was never answered. */
fun SrsCard?.masteryLevel(): MasteryLevel = when {
    this == null -> MasteryLevel.NEW
    state != CardState.REVIEW -> MasteryLevel.LEARNING
    (stability ?: 0.0) >= MATURE_STABILITY_DAYS -> MasteryLevel.MATURE
    else -> MasteryLevel.YOUNG
}

/** Number of forms per [MasteryLevel]. */
data class MasteryCounts(
    val new: Int = 0,
    val learning: Int = 0,
    val young: Int = 0,
    val mature: Int = 0,
) {
    val total: Int get() = new + learning + young + mature

    /** Answered at least once (learning, young or mature). */
    val learned: Int get() = total - new

    /** At least one form was answered. */
    val isStarted: Boolean get() = learned > 0

    operator fun get(level: MasteryLevel): Int = when (level) {
        MasteryLevel.NEW -> new
        MasteryLevel.LEARNING -> learning
        MasteryLevel.YOUNG -> young
        MasteryLevel.MATURE -> mature
    }

    companion object {
        fun of(levels: Iterable<MasteryLevel>): MasteryCounts {
            val counts = levels.groupingBy { it }.eachCount()
            return MasteryCounts(
                new = counts[MasteryLevel.NEW] ?: 0,
                learning = counts[MasteryLevel.LEARNING] ?: 0,
                young = counts[MasteryLevel.YOUNG] ?: 0,
                mature = counts[MasteryLevel.MATURE] ?: 0,
            )
        }
    }
}

/** Mastery of all trainable cards of the core verbs. */
data class MasteryOverview(
    /** All trainable forms. */
    val forms: MasteryCounts,
    /** Forms per verb. */
    val byVerb: Map<Int, MasteryCounts>,
    /** Cards (forms and meaning) due at the time the overview was computed. */
    val dueNow: Int,
    /** Both meaning directions of every verb that takes part. */
    val meaning: MasteryCounts = MasteryCounts(),
    /** Verbs with at least one answered card (form or meaning). */
    val startedVerbs: Int = 0,
    /** Verbs with at least one trainable card. */
    val totalVerbs: Int = 0,
)
