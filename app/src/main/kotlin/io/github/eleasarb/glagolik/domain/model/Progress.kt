package io.github.eleasarb.glagolik.domain.model

import io.github.eleasarb.glagolik.domain.srs.SrsCard

/** Kind of exercise, used to group progress ("forms" and "meaning"). */
enum class ExerciseKind {
    FORM,
    MEANING,
}

/**
 * What is practised on a card of a verb. [id] is stored in progress.db (column `item`) and in
 * the export, so ids never change once released.
 */
sealed interface Exercise {
    val id: String
    val kind: ExerciseKind

    /** Type one conjugated form. The id is the FormKey name, as in progress.db version 1. */
    data class Form(val form: FormKey) : Exercise {
        override val id: String get() = form.name
        override val kind: ExerciseKind get() = ExerciseKind.FORM
    }

    /** Russian infinitive shown, pick the translation (multiple choice). */
    data object MeaningRuDe : Exercise {
        override val id: String = "MEANING_RU_DE"
        override val kind: ExerciseKind = ExerciseKind.MEANING
    }

    /** Translation and aspect shown, type the Russian infinitive. */
    data object MeaningDeRu : Exercise {
        override val id: String = "MEANING_DE_RU"
        override val kind: ExerciseKind = ExerciseKind.MEANING
    }

    companion object {
        /** Both meaning directions, in the order they are introduced. */
        val MEANINGS: List<Exercise> = listOf(MeaningRuDe, MeaningDeRu)

        /** `null` for an unknown id. */
        fun fromId(id: String): Exercise? =
            FormKey.entries.firstOrNull { it.name == id }?.let(::Form)
                ?: MEANINGS.firstOrNull { it.id == id }
    }
}

/** Identifies one card: stable verb id plus exercise. */
data class CardKey(val verbId: Int, val exercise: Exercise)

/** A card with its current scheduling state. */
data class ScheduledCard(val key: CardKey, val card: SrsCard)

/** A form that can be quizzed, with everything the prompt needs. */
data class TrainableForm(
    val verb: VerbSummary,
    val form: FormKey,
    /** All accepted variants, with stress marks. */
    val expected: List<String>,
) {
    val key: CardKey get() = CardKey(verb.id, Exercise.Form(form))
}

/** A core verb with the ids of its aspect partners (in both directions). */
data class CoreVerb(val summary: VerbSummary, val partnerIds: Set<Int>)
