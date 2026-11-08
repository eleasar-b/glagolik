package io.github.eleasarb.glagolik.domain.usecase

import io.github.eleasarb.glagolik.domain.model.ComparableMeanings
import io.github.eleasarb.glagolik.domain.model.CoreVerb
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import io.github.eleasarb.glagolik.domain.model.comparableMeanings
import kotlin.random.Random

/**
 * Meaning exercises of the core verbs: which verbs take part, distractors for the multiple
 * choice and other verbs that also fit a typed answer.
 *
 * A verb takes part if it has a German and an English translation. Two verbs "share a meaning"
 * if one of their meanings is the same in German or in English (see [ComparableMeanings]).
 */
class MeaningCatalog(verbs: List<CoreVerb>) {
    private val eligible: List<CoreVerb> = verbs.filter { verb ->
        verb.summary.translation.let { it.german.isNotBlank() && it.english.isNotBlank() }
    }
    private val byId: Map<Int, CoreVerb> = eligible.associateBy { it.summary.id }
    private val meanings: Map<Int, ComparableMeanings> =
        eligible.associate { it.summary.id to it.summary.translation.comparableMeanings() }

    /** Eligible verbs in the order given (frequency order). */
    val verbs: List<VerbSummary> get() = eligible.map { it.summary }

    fun contains(verbId: Int): Boolean = verbId in byId

    /** The verb if it takes part, `null` otherwise. */
    fun verb(verbId: Int): VerbSummary? = byId[verbId]?.summary

    /**
     * Multiple choice for [verbId]: the verb and [DISTRACTORS] other core verbs in random order.
     * A distractor is never the verb itself, an aspect partner (either direction) or a verb that
     * shares a meaning with it. `null` if the verb does not take part or there are too few
     * distractors.
     */
    fun choice(verbId: Int, random: Random): MeaningChoice? {
        val verb = byId[verbId]
        val candidates = verb?.let(::distractorsFor).orEmpty()
        if (verb == null || candidates.size < DISTRACTORS) return null
        val options = (candidates.shuffled(random).take(DISTRACTORS) + verb)
            .map { it.summary }
            .shuffled(random)
        return MeaningChoice(options = options, correctIndex = options.indexOf(verb.summary))
    }

    private fun distractorsFor(verb: CoreVerb): List<CoreVerb> {
        val verbId = verb.summary.id
        val own = meanings.getValue(verbId)
        return eligible.filter { other ->
            val id = other.summary.id
            id != verbId &&
                id !in verb.partnerIds &&
                verbId !in other.partnerIds &&
                !own.overlaps(meanings.getValue(id))
        }
    }

    /**
     * Other core verbs with the same aspect that share a meaning with [verbId]. Typing one of
     * them is not wrong, only not the verb asked for.
     */
    fun alsoFitting(verbId: Int): List<VerbSummary> {
        val verb = byId[verbId] ?: return emptyList()
        val own = meanings.getValue(verbId)
        return eligible
            .filter { other ->
                other.summary.id != verbId &&
                    other.summary.aspect == verb.summary.aspect &&
                    own.overlaps(meanings.getValue(other.summary.id))
            }
            .map { it.summary }
    }

    companion object {
        const val DISTRACTORS = 3
    }
}

/** Options of a multiple choice; exactly one is right. */
data class MeaningChoice(val options: List<VerbSummary>, val correctIndex: Int)
